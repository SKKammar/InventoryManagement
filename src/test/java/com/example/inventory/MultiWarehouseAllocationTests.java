package com.example.inventory;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.dto.OrderDTO;
import com.example.inventory.dto.OrderItemAllocationDTO;
import com.example.inventory.entity.*;
import com.example.inventory.enums.RoleType;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.*;
import com.example.inventory.service.CheckoutService;
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

import static org.assertj.core.api.Assertions.assertThat;

public class MultiWarehouseAllocationTests extends IntegrationTestBase {

    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private OrderItemAllocationRepository allocationRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;
    @Autowired
    private InventoryReservationRepository reservationRepository;

    private User testUser;
    private ProductVariant testVariant;
    private Warehouse whA;
    private Warehouse whB;

    @BeforeEach
    void setup() {
        reservationRepository.deleteAll();
        allocationRepository.deleteAll();
        orderRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("user_alloc");
        testUser.setEmail("user_alloc@example.com");
        testUser.setPassword("password");
        testUser.setRole(RoleType.CUSTOMER);
        userRepository.save(testUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getUsername(), "password", List.of())
        );

        whA = new Warehouse();
        whA.setCode("WH-A");
        whA.setName("Warehouse A");
        whA.setPriority(1); // Higher priority
        warehouseRepository.save(whA);

        whB = new Warehouse();
        whB.setCode("WH-B");
        whB.setName("Warehouse B");
        whB.setPriority(2); // Lower priority
        warehouseRepository.save(whB);

        Product p = new Product();
        p.setName("Alloc Product");
        productRepository.save(p);

        testVariant = new ProductVariant();
        testVariant.setProduct(p);
        testVariant.setSku("SKU-ALLOC-1");
        testVariant.setPrice(new BigDecimal("10.00"));
        productVariantRepository.save(testVariant);

        // WH A gets 5 units
        Inventory invA = new Inventory();
        invA.setProductVariant(testVariant);
        invA.setWarehouse(whA);
        invA.setQuantityOnHand(0);
        invA = inventoryRepository.save(invA);
        inventoryService.adjustStock(invA, 5, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial A", "SYSTEM");

        // WH B gets 10 units
        Inventory invB = new Inventory();
        invB.setProductVariant(testVariant);
        invB.setWarehouse(whB);
        invB.setQuantityOnHand(0);
        invB = inventoryRepository.save(invB);
        inventoryService.adjustStock(invB, 10, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial B", "SYSTEM");
    }

    @AfterEach
    void cleanup() {
        reservationRepository.deleteAll();
        allocationRepository.deleteAll();
        orderRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldSplitOrderAcrossWarehouses() {
        // Order 12 units. A has 5, B has 10.
        // Expected: A -> 5, B -> 7
        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(UUID.randomUUID().toString());
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(12);
        request.setItems(List.of(item));

        OrderDTO order = checkoutService.checkout(request);
        assertThat(order).isNotNull();
        assertThat(order.getItems()).hasSize(1);
        
        List<OrderItemAllocationDTO> allocations = order.getItems().get(0).getAllocations();
        assertThat(allocations).isNotNull();
        assertThat(allocations).hasSize(2);

        boolean hasWhA = allocations.stream().anyMatch(a -> a.getWarehouseId().equals(whA.getId()) && a.getQuantity() == 5);
        boolean hasWhB = allocations.stream().anyMatch(a -> a.getWarehouseId().equals(whB.getId()) && a.getQuantity() == 7);

        assertThat(hasWhA).isTrue();
        assertThat(hasWhB).isTrue();
    }
}
