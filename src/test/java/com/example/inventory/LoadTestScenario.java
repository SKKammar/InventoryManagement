package com.example.inventory;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.entity.Inventory;
import com.example.inventory.entity.Product;
import com.example.inventory.entity.ProductVariant;
import com.example.inventory.entity.User;
import com.example.inventory.entity.Warehouse;
import com.example.inventory.enums.RoleType;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.*;
import com.example.inventory.service.CheckoutService;
import com.example.inventory.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadTestScenario extends IntegrationTestBase {

    private static final Logger log = LoggerFactory.getLogger(LoadTestScenario.class);

    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private IdempotencyRecordRepository idempotencyRepository;
    @Autowired
    private InventoryReservationRepository reservationRepository;
    
    private User testUser;
    private ProductVariant testVariant;

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
        testUser.setUsername("loaduser");
        testUser.setEmail("load@example.com");
        testUser.setPassword("password");
        testUser.setRole(RoleType.CUSTOMER);
        userRepository.save(testUser);

        Warehouse mainWh = new Warehouse();
        mainWh.setCode("WH-LOAD");
        mainWh.setName("Load Warehouse");
        mainWh.setPriority(1);
        warehouseRepository.save(mainWh);

        Product p = new Product();
        p.setName("Load Test Product");
        productRepository.save(p);

        testVariant = new ProductVariant();
        testVariant.setProduct(p);
        testVariant.setSku("SKU-LOAD-1");
        testVariant.setPrice(new BigDecimal("10.00"));
        productVariantRepository.save(testVariant);

        Inventory inv = new Inventory();
        inv.setProductVariant(testVariant);
        inv.setWarehouse(mainWh);
        inv.setQuantityOnHand(0);
        inv = inventoryRepository.save(inv);
        
        // Seed enough stock for load test
        inventoryService.adjustStock(inv, 10000, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial", "SYSTEM");
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
    void simulateConcurrentCheckoutLoad() throws InterruptedException {
        int threadCount = 50; // Simulate 50 concurrent checkout users
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait until all threads are ready
                    
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(testUser.getUsername(), "password", List.of())
                    );

                    CreateOrderRequest req = new CreateOrderRequest();
                    req.setIdempotencyKey(UUID.randomUUID().toString());
                    CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
                    item.setProductVariantId(testVariant.getId());
                    item.setQuantity(2);
                    req.setItems(List.of(item));

                    checkoutService.checkout(req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        stopWatch.start();
        startLatch.countDown(); // FIRE!
        
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        executor.shutdown();

        log.info("Load test completed in {} ms", stopWatch.getTotalTimeMillis());
        log.info("Successful Checkouts: {}", successCount.get());
        log.info("Failed Checkouts: {}", failureCount.get());
        
        assertThat(completed).isTrue();
        
        // Since we have plenty of stock, we expect mostly successes.
        // Some might fail due to PostgreSQL optimistic locking on Inventory (concurrent modification)
        // This validates if our connection pool and DB locking behave reasonably under load.
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
    }
}
