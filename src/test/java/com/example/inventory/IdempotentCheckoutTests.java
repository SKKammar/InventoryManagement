package com.example.inventory;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.dto.OrderDTO;
import com.example.inventory.entity.*;
import com.example.inventory.enums.IdempotencyStatus;
import com.example.inventory.enums.RoleType;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.*;
import com.example.inventory.service.CheckoutService;
import com.example.inventory.service.IdempotencyService;
import com.example.inventory.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdempotentCheckoutTests extends IntegrationTestBase {

    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private IdempotencyRecordRepository idempotencyRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private InventoryReservationRepository reservationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private IdempotencyService idempotencyService;

    private User testUser;
    private ProductVariant testVariant;
    private Warehouse mainWh;

    @BeforeEach
    void setup() {
        idempotencyRepository.deleteAll();
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();
        orderRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser_idem");
        testUser.setEmail("test_idem@example.com");
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
        p.setName("Idem Test Product");
        productRepository.save(p);

        testVariant = new ProductVariant();
        testVariant.setProduct(p);
        testVariant.setSku("SKU-IDEM-1");
        testVariant.setPrice(new BigDecimal("10.00"));
        productVariantRepository.save(testVariant);

        createInventoryWithStock(100);
    }

    @AfterEach
    void cleanup() {
        idempotencyRepository.deleteAll();
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();
        orderRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldProcessIdempotentCheckoutTwiceSafely() {
        String key = UUID.randomUUID().toString();
        CreateOrderRequest request = createRequest(key, 5);

        // First attempt
        OrderDTO firstResponse = checkoutService.checkout(request);
        assertThat(firstResponse).isNotNull();

        // Second attempt
        OrderDTO secondResponse = checkoutService.checkout(request);
        
        // Assertions
        assertThat(firstResponse.getId()).isEqualTo(secondResponse.getId());
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(idempotencyRepository.count()).isEqualTo(1);
        
        IdempotencyRecord record = idempotencyRepository.findAll().get(0);
        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.SUCCEEDED);
        assertThat(record.getResourceId()).isEqualTo(firstResponse.getId());
    }

    @Test
    void shouldRejectPayloadMismatch() {
        String key = UUID.randomUUID().toString();
        CreateOrderRequest request1 = createRequest(key, 5);
        checkoutService.checkout(request1);

        // Mismatched payload (quantity = 10)
        CreateOrderRequest request2 = createRequest(key, 10);
        
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> checkoutService.checkout(request2));
        assertThat(ex.getMessage()).contains("409 CONFLICT");
    }

    @Test
    void shouldRetryFailedCheckout() {
        String key = UUID.randomUUID().toString();
        CreateOrderRequest request = createRequest(key, 500); // 500 quantity, but we only have 100 stock

        // First attempt (fails due to insufficient stock)
        assertThrows(IllegalStateException.class, () -> checkoutService.checkout(request));

        IdempotencyRecord record = idempotencyRepository.findByUserAndOperationTypeAndIdempotencyKey(testUser, "CHECKOUT", key).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.FAILED);

        // Add stock
        Inventory inv = inventoryRepository.findAll().get(0);
        inventoryService.adjustStock(inv, 400, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Add", "SYSTEM");

        // Second attempt (now succeeds)
        OrderDTO response = checkoutService.checkout(request);
        assertThat(response).isNotNull();

        record = idempotencyRepository.findById(record.getId()).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.SUCCEEDED);
    }

    @Test
    void shouldSafelyHandleConcurrentCheckoutWithSameKey() throws InterruptedException {
        String key = UUID.randomUUID().toString();
        CreateOrderRequest request = createRequest(key, 10);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(1);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicReference<Long> orderId1 = new AtomicReference<>();
        AtomicReference<Long> orderId2 = new AtomicReference<>();

        Runnable checkoutTask1 = () -> {
            try {
                latch.await();
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(testUser.getUsername(), "password", List.of()));
                OrderDTO order = checkoutService.checkout(request);
                orderId1.set(order.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                // If it fails with 409 because it's processing, that's expected
            }
        };

        Runnable checkoutTask2 = () -> {
            try {
                latch.await();
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(testUser.getUsername(), "password", List.of()));
                OrderDTO order = checkoutService.checkout(request);
                orderId2.set(order.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                // If it fails with 409 because it's processing, that's expected
            }
        };

        executor.submit(checkoutTask1);
        executor.submit(checkoutTask2);
        
        latch.countDown(); // fire simultaneously
        Thread.sleep(3000);

        // At most ONE order should exist.
        // It's possible one thread got a 409 Processing error, which is perfectly safe.
        // It's also possible one thread blocked on constraint, then read the successful record and returned the same Order ID.
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(reservationRepository.count()).isEqualTo(1);
        
        if (orderId1.get() != null && orderId2.get() != null) {
            assertThat(orderId1.get()).isEqualTo(orderId2.get());
        }
    }

    private CreateOrderRequest createRequest(String key, int qty) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(key);
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(qty);
        request.setItems(List.of(item));
        return request;
    }

    private Inventory createInventoryWithStock(int qty) {
        Inventory inv = new Inventory();
        inv.setProductVariant(testVariant);
        inv.setWarehouse(mainWh);
        inv.setQuantityOnHand(0);
        inv = inventoryRepository.save(inv);
        inventoryService.adjustStock(inv, qty, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial", "SYSTEM");
        return inv;
    }
}
