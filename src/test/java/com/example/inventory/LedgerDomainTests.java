package com.example.inventory;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.dto.OrderDTO;
import com.example.inventory.entity.*;
import com.example.inventory.enums.RoleType;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.*;
import com.example.inventory.service.InventoryService;
import com.example.inventory.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
public class LedgerDomainTests extends IntegrationTestBase {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private InventoryTransactionRepository transactionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private OrderService orderService;

    private User testUser;
    private Warehouse mainWh;
    private ProductVariant testVariant;

    @BeforeEach
    void setup() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
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
        p.setName("Ledger Test Product");
        productRepository.save(p);

        testVariant = new ProductVariant();
        testVariant.setProduct(p);
        testVariant.setSku("SKU-LEDGER-1");
        testVariant.setPrice(new BigDecimal("10.00"));
        productVariantRepository.save(testVariant);
    }

    @Test
    void shouldCreateInitialStockLedger() {
        Inventory inv = new Inventory();
        inv.setProductVariant(testVariant);
        inv.setWarehouse(mainWh);
        inv.setQuantityOnHand(0);
        inv = inventoryRepository.save(inv);

        inventoryService.adjustStock(inv, 100, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial Seed", "SYSTEM");

        var transactions = transactionRepository.findByInventoryId(inv.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        assertThat(transactions).hasSize(1);
        InventoryTransaction tx = transactions.get(0);
        assertThat(tx.getQuantityChange()).isEqualTo(100);
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.INITIAL_STOCK);
        assertThat(tx.getResultingQuantity()).isEqualTo(100);
        assertThat(inv.getQuantityOnHand()).isEqualTo(100);
    }

    @Test
    void shouldCreateLedgerEntryOnOrderDeduction() {
        Inventory inv = new Inventory();
        inv.setProductVariant(testVariant);
        inv.setWarehouse(mainWh);
        inv.setQuantityOnHand(0);
        inv = inventoryRepository.save(inv);
        inventoryService.adjustStock(inv, 100, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial Seed", "SYSTEM");

        CreateOrderRequest request = new CreateOrderRequest();
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(20);
        request.setItems(List.of(item));

        OrderDTO order = orderService.createOrder(request);

        var transactions = transactionRepository.findByInventoryId(inv.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        assertThat(transactions).hasSize(2);
        
        InventoryTransaction tx = transactions.stream().filter(t -> t.getTransactionType() == TransactionType.ORDER_DEDUCTION).findFirst().orElseThrow();
        assertThat(tx.getQuantityChange()).isEqualTo(-20);
        assertThat(tx.getReferenceType()).isEqualTo("ORDER");
        assertThat(tx.getReferenceId()).isEqualTo(order.getId().toString());
        assertThat(tx.getResultingQuantity()).isEqualTo(80);
        
        Inventory updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityOnHand()).isEqualTo(80);
    }

    @Test
    void shouldMaintainReconciliationInvariant() {
        Inventory inv = new Inventory();
        inv.setProductVariant(testVariant);
        inv.setWarehouse(mainWh);
        inv.setQuantityOnHand(0);
        inv = inventoryRepository.save(inv);

        inventoryService.adjustStock(inv, 100, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial Seed", "SYSTEM");
        inventoryService.adjustStock(inv, -20, TransactionType.ORDER_DEDUCTION, "ORDER", "1", "Order 1", "SYSTEM");
        inventoryService.adjustStock(inv, -10, TransactionType.ORDER_DEDUCTION, "ORDER", "2", "Order 2", "SYSTEM");
        inventoryService.adjustStock(inv, 5, TransactionType.SYSTEM_MIGRATION, "SYSTEM", null, "Correction", "SYSTEM");

        var transactions = transactionRepository.findByInventoryId(inv.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        int sum = transactions.stream().mapToInt(InventoryTransaction::getQuantityChange).sum();
        
        Inventory updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(sum).isEqualTo(updatedInv.getQuantityOnHand());
        assertThat(updatedInv.getQuantityOnHand()).isEqualTo(75);
    }

    @Test
    void shouldRollbackInventoryIfOrderFails() {
        Inventory inv = new Inventory();
        inv.setProductVariant(testVariant);
        inv.setWarehouse(mainWh);
        inv.setQuantityOnHand(0);
        inv = inventoryRepository.save(inv);
        inventoryService.adjustStock(inv, 10, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial Seed", "SYSTEM");

        CreateOrderRequest request = new CreateOrderRequest();
        CreateOrderRequest.CreateOrderItemRequest item = new CreateOrderRequest.CreateOrderItemRequest();
        item.setProductVariantId(testVariant.getId());
        item.setQuantity(20); // Try to order more than available
        request.setItems(List.of(item));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));

        // Ledger should only have the initial stock, no deductions
        var transactions = transactionRepository.findByInventoryId(inv.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        assertThat(transactions).hasSize(1);
        
        Inventory updatedInv = inventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityOnHand()).isEqualTo(10);
    }
}
