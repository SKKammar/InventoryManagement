package com.example.inventory;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.dto.OrderDTO;
import com.example.inventory.entity.*;
import com.example.inventory.enums.OrderStatus;
import com.example.inventory.enums.RoleType;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.*;
import com.example.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class EndToEndCheckoutTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
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

    private User testUser;
    private ProductVariant testVariant;
    private Warehouse mainWh;

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("e2euser");
        testUser.setEmail("e2e@example.com");
        testUser.setPassword("password");
        testUser.setRole(RoleType.CUSTOMER);
        userRepository.save(testUser);

        mainWh = new Warehouse();
        mainWh.setCode("WH-E2E");
        mainWh.setName("E2E Warehouse");
        mainWh.setPriority(1);
        warehouseRepository.save(mainWh);

        Product p = new Product();
        p.setName("E2E Test Product");
        productRepository.save(p);

        testVariant = new ProductVariant();
        testVariant.setProduct(p);
        testVariant.setSku("SKU-E2E-1");
        testVariant.setPrice(new BigDecimal("15.50"));
        productVariantRepository.save(testVariant);

        Inventory inv = new Inventory();
        inv.setProductVariant(testVariant);
        inv.setWarehouse(mainWh);
        inv.setQuantityOnHand(0);
        inv = inventoryRepository.save(inv);
        inventoryService.adjustStock(inv, 50, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial Seed", "SYSTEM");
    }

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "e2euser", roles = {"CUSTOMER"})
    void fullCheckoutAndCompletionFlow() throws Exception {
        // 1. Create Checkout Request
        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(UUID.randomUUID().toString());
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(5);
        request.setItems(List.of(item));

        // 2. Perform Checkout via API
        String responseString = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();

        OrderDTO order = objectMapper.readValue(responseString, OrderDTO.class);

        // Verify Reservation
        Inventory inv = inventoryRepository.findByProductVariantAndWarehouse(testVariant, mainWh).orElseThrow();
        assertThat(inv.getQuantityReserved()).isEqualTo(5);
        assertThat(inv.getQuantityOnHand()).isEqualTo(50); // Not deducted yet

        // 3. Process Order (Requires ADMIN or WAREHOUSE_STAFF)
        mockMvc.perform(post("/api/orders/" + order.getId() + "/process"))
                .andExpect(status().isForbidden()); // Customer cannot process

        // We bypass controller for admin action to simulate backend fulfillment
        // or we mock as admin:
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void fullCheckoutAndCompletionFlowAsAdmin() throws Exception {
        User admin = new User();
        admin.setUsername("adminuser");
        admin.setEmail("admin@example.com");
        admin.setPassword("password");
        admin.setRole(RoleType.ADMIN);
        userRepository.save(admin);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(UUID.randomUUID().toString());
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(5);
        request.setItems(List.of(item));

        String responseString = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();

        OrderDTO order = objectMapper.readValue(responseString, OrderDTO.class);

        // Process
        mockMvc.perform(post("/api/orders/" + order.getId() + "/process"))
                .andExpect(status().isOk());

        Order processedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(processedOrder.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        // Complete
        mockMvc.perform(post("/api/orders/" + order.getId() + "/complete"))
                .andExpect(status().isOk());

        Order completedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // Verify final inventory state
        Inventory inv = inventoryRepository.findByProductVariantAndWarehouse(testVariant, mainWh).orElseThrow();
        assertThat(inv.getQuantityReserved()).isEqualTo(0);
        assertThat(inv.getQuantityOnHand()).isEqualTo(45);
    }
}
