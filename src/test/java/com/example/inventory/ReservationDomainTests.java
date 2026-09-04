package com.example.inventory;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.dto.OrderDTO;
import com.example.inventory.entity.*;
import com.example.inventory.enums.ReservationStatus;
import com.example.inventory.enums.RoleType;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.*;
import com.example.inventory.service.InventoryReservationService;
import com.example.inventory.service.InventoryService;
import com.example.inventory.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReservationDomainTests extends IntegrationTestBase {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private InventoryReservationRepository reservationRepository;
    @Autowired
    private InventoryTransactionRepository transactionRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private InventoryReservationService reservationService;
    @Autowired
    private OrderService orderService;

    private User testUser;
    private Warehouse mainWh;
    private ProductVariant testVariant;

    @BeforeEach
    void setup() {
        // Run cleanup in case of non-transactional test execution
        reservationRepository.deleteAll();
        transactionRepository.deleteAll();
        inventoryRepository.deleteAll();
        orderRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser_res");
        testUser.setEmail("test_res@example.com");
        testUser.setPassword("password");
        testUser.setRole(RoleType.CUSTOMER);
        userRepository.save(testUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getUsername(), "password", List.of())
        );

        mainWh = new Warehouse();
        mainWh.setCode("WH-MAIN");
        mainWh.setName("Main Warehouse");
        mainWh.setPriority(1);
        warehouseRepository.save(mainWh);

        Product p = new Product();
        p.setName("Reservation Test Product");
        productRepository.save(p);

        testVariant = new ProductVariant();
        testVariant.setProduct(p);
        testVariant.setSku("SKU-RES-1");
        testVariant.setPrice(new BigDecimal("10.00"));
        productVariantRepository.save(testVariant);
    }

    @AfterEach
    void cleanup() {
        reservationRepository.deleteAll();
        transactionRepository.deleteAll();
        inventoryRepository.deleteAll();
        orderRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReserveInventoryWithoutDeductingOnHand() {
        Inventory inv = createInventoryWithStock(100);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(UUID.randomUUID().toString());
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(20);
        request.setItems(List.of(item));

        OrderDTO order = orderService.createOrder(request);

        Inventory updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityOnHand()).isEqualTo(100); // Should not change
        assertThat(updatedInv.getQuantityReserved()).isEqualTo(20);
        assertThat(updatedInv.getAvailableQuantity()).isEqualTo(80);

        List<InventoryReservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void shouldConsumeReservationAndCreateLedgerEntry() {
        Inventory inv = createInventoryWithStock(100);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(UUID.randomUUID().toString());
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(30);
        request.setItems(List.of(item));

        OrderDTO order = orderService.createOrder(request);
        orderService.confirmOrder(order.getId());

        Inventory updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityOnHand()).isEqualTo(70);
        assertThat(updatedInv.getQuantityReserved()).isEqualTo(0);

        List<InventoryReservation> reservations = reservationRepository.findAll();
        assertThat(reservations.get(0).getStatus()).isEqualTo(ReservationStatus.CONSUMED);

        var transactions = transactionRepository.findByInventoryId(inv.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        InventoryTransaction deductTx = transactions.stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.ORDER_DEDUCTION)
                .findFirst().orElseThrow();
        assertThat(deductTx.getQuantityChange()).isEqualTo(-30);
    }

    @Test
    void shouldReleaseReservationWithoutLedgerEntry() {
        Inventory inv = createInventoryWithStock(100);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(UUID.randomUUID().toString());
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(30);
        request.setItems(List.of(item));

        OrderDTO order = orderService.createOrder(request);
        orderService.cancelOrder(order.getId());

        Inventory updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityOnHand()).isEqualTo(100);
        assertThat(updatedInv.getQuantityReserved()).isEqualTo(0);

        List<InventoryReservation> reservations = reservationRepository.findAll();
        assertThat(reservations.get(0).getStatus()).isEqualTo(ReservationStatus.RELEASED);

        var transactions = transactionRepository.findByInventoryId(inv.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        boolean hasDeduction = transactions.stream().anyMatch(tx -> tx.getTransactionType() == TransactionType.ORDER_DEDUCTION);
        assertThat(hasDeduction).isFalse();
    }

    @Test
    void shouldRejectDoubleConsume() {
        Inventory inv = createInventoryWithStock(100);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(UUID.randomUUID().toString());
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(30);
        request.setItems(List.of(item));

        OrderDTO order = orderService.createOrder(request);
        orderService.confirmOrder(order.getId());
        
        assertThrows(IllegalStateException.class, () -> orderService.confirmOrder(order.getId()));
    }

    @Test
    void shouldRejectConcurrentOverselling() throws InterruptedException {
        Inventory inv = createInventoryWithStock(10); // Only 10 available

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Runnable checkoutTask1 = () -> {
            try {
                latch.await();
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(testUser.getUsername(), "password", List.of()));
                CreateOrderRequest req = new CreateOrderRequest();
                req.setIdempotencyKey(UUID.randomUUID().toString());
                CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
                item.setProductVariantId(testVariant.getId());
                item.setQuantity(7);
                req.setItems(List.of(item));
                orderService.createOrder(req);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        };

        Runnable checkoutTask2 = () -> {
            try {
                latch.await();
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(testUser.getUsername(), "password", List.of()));
                CreateOrderRequest req = new CreateOrderRequest();
                req.setIdempotencyKey(UUID.randomUUID().toString());
                CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
                item.setProductVariantId(testVariant.getId());
                item.setQuantity(6);
                req.setItems(List.of(item));
                orderService.createOrder(req);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        };

        executor.submit(checkoutTask1);
        executor.submit(checkoutTask2);
        
        latch.countDown(); // Start both simultaneously
        
        Thread.sleep(2000); // Give enough time for both to finish

        Inventory updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        
        // One should succeed, one should fail (either due to insufficient available OR optimistic lock)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(updatedInv.getQuantityReserved()).isLessThanOrEqualTo(10);
        assertThat(updatedInv.getQuantityOnHand()).isEqualTo(10);
    }

    @Test
    void shouldHandleIdempotencyCorrectly() {
        Inventory inv = createInventoryWithStock(100);
        String idempotencyKey = UUID.randomUUID().toString();

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(idempotencyKey);
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(20);
        request.setItems(List.of(item));

        OrderDTO order1 = orderService.createOrder(request);
        OrderDTO order2 = orderService.createOrder(request); // Second identical call

        assertThat(order1.getId()).isEqualTo(order2.getId()); // Should return the same order
        
        Inventory updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityReserved()).isEqualTo(20); // Not 40

        // Submitting with different payload should fail
        request.getItems().get(0).setQuantity(30);
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
    }

    private Inventory createInventoryWithStock(int qty) {
        Inventory inv = new Inventory();
        inv.setProductVariant(testVariant);
        inv.setWarehouse(mainWh);
        inv.setQuantityOnHand(0);
        inv = inventoryRepository.save(inv);
        inventoryService.adjustStock(inv, qty, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial Seed", "SYSTEM");
        return inv;
    }
}
