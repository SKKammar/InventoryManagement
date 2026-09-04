package com.example.inventory;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.dto.OrderDTO;
import com.example.inventory.entity.*;
import com.example.inventory.enums.OrderStatus;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

public class OrderLifecycleTests extends IntegrationTestBase {

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
        reservationRepository.deleteAll();
        transactionRepository.deleteAll();
        inventoryRepository.deleteAll();
        orderRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser_life");
        testUser.setEmail("test_life@example.com");
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
        p.setName("Lifecycle Test Product");
        productRepository.save(p);

        testVariant = new ProductVariant();
        testVariant.setProduct(p);
        testVariant.setSku("SKU-LIFE-1");
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
    void shouldSuccessfullyCompleteValidLifecycle() {
        Inventory inv = createInventoryWithStock(100);

        OrderDTO order = createTestOrder(20);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        orderService.startProcessing(order.getId());
        Order processingOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(processingOrder.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        orderService.completeOrder(order.getId());
        Order completedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // Verify inventory deduction and ledger
        Inventory updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityOnHand()).isEqualTo(80);
        assertThat(updatedInv.getQuantityReserved()).isEqualTo(0);
        
        var transactions = transactionRepository.findByInventoryId(inv.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        boolean hasDeduction = transactions.stream().anyMatch(tx -> tx.getTransactionType() == TransactionType.ORDER_DEDUCTION);
        assertThat(hasDeduction).isTrue();
    }

    @Test
    void shouldRejectInvalidTransitions() {
        Inventory inv = createInventoryWithStock(100);
        OrderDTO order = createTestOrder(20);

        // CONFIRMED -> COMPLETED is invalid, must be PROCESSING
        assertThrows(IllegalStateException.class, () -> orderService.completeOrder(order.getId()));

        orderService.startProcessing(order.getId()); // -> PROCESSING

        // PROCESSING -> CANCELLED is invalid in our state machine
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(order.getId()));

        orderService.completeOrder(order.getId()); // -> COMPLETED

        // COMPLETED -> CANCELLED is invalid
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(order.getId()));
    }

    @Test
    void shouldHandleDoubleCancellationGracefully() {
        Inventory inv = createInventoryWithStock(100);
        OrderDTO order = createTestOrder(20);

        orderService.cancelOrder(order.getId());
        
        Inventory updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityReserved()).isEqualTo(0);

        // Second cancellation should be idempotent and not fail or over-release
        orderService.cancelOrder(order.getId());
        
        updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityReserved()).isEqualTo(0); // Still 0
    }

    @Test
    void shouldRejectCompletionIfReservationExpired() {
        Inventory inv = createInventoryWithStock(100);
        OrderDTO order = createTestOrder(20);

        orderService.startProcessing(order.getId());

        // Simulate expiration
        InventoryReservation reservation = reservationRepository.findByOrder(orderRepository.findById(order.getId()).get()).get(0);
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        reservationRepository.save(reservation);
        reservationService.expireReservations(); // This transitions reservation to EXPIRED

        // Attempting to complete should fail because no active reservations exist
        assertThrows(IllegalStateException.class, () -> orderService.completeOrder(order.getId()));

        // Ensure order is not COMPLETED
        Order finalOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void shouldRejectConcurrentStateTransitions() throws InterruptedException {
        Inventory inv = createInventoryWithStock(100);
        OrderDTO orderDto = createTestOrder(20);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Runnable cancelTask = () -> {
            try {
                latch.await();
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(testUser.getUsername(), "password", List.of()));
                orderService.cancelOrder(orderDto.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        };

        Runnable startProcessingTask = () -> {
            try {
                latch.await();
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(testUser.getUsername(), "password", List.of()));
                orderService.startProcessing(orderDto.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        };

        executor.submit(cancelTask);
        executor.submit(startProcessingTask);
        
        latch.countDown();
        Thread.sleep(2000);

        // Only one should succeed because they modify the same Order and increment its @Version
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        Order finalOrder = orderRepository.findById(orderDto.getId()).orElseThrow();
        // It must be either CANCELLED or PROCESSING, but not both/neither
        assertThat(finalOrder.getStatus()).isIn(OrderStatus.CANCELLED, OrderStatus.PROCESSING);
    }

    private OrderDTO createTestOrder(int qty) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(UUID.randomUUID().toString());
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(qty);
        request.setItems(List.of(item));

        return orderService.createOrder(request);
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
