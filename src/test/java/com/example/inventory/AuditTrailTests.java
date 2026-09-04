package com.example.inventory;

import com.example.inventory.entity.*;
import com.example.inventory.enums.AuditAction;
import com.example.inventory.enums.RoleType;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuditTrailTests extends IntegrationTestBase {

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InventoryTransactionRepository transactionRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    private User testAdmin;
    private Inventory testInventory;

    @BeforeEach
    void setup() {
        auditLogRepository.deleteAll();
        transactionRepository.deleteAll();
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();

        testAdmin = new User();
        testAdmin.setUsername("admin_audit");
        testAdmin.setEmail("admin_audit@example.com");
        testAdmin.setPassword("password");
        testAdmin.setRole(RoleType.ADMIN);
        userRepository.save(testAdmin);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testAdmin.getUsername(), "password", List.of())
        );

        Warehouse wh = new Warehouse();
        wh.setCode("WH-AUDIT");
        wh.setName("Audit WH");
        wh.setPriority(1);
        warehouseRepository.save(wh);

        Product p = new Product();
        p.setName("Audit Product");
        productRepository.save(p);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(p);
        variant.setSku("SKU-AUDIT-1");
        variant.setPrice(new BigDecimal("10.00"));
        productVariantRepository.save(variant);

        testInventory = new Inventory();
        testInventory.setProductVariant(variant);
        testInventory.setWarehouse(wh);
        testInventory.setQuantityOnHand(0);
        testInventory = inventoryRepository.save(testInventory);

        inventoryService.adjustStock(testInventory, 100, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial", "SYSTEM");
    }

    @AfterEach
    void cleanup() {
        auditLogRepository.deleteAll();
        transactionRepository.deleteAll();
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void manualAdjustmentShouldCreateAuditLog() {
        // Act
        inventoryService.manualAdjustStock(testInventory.getId(), -5, "Damaged stock", testAdmin.getUsername());

        // Assert
        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        
        AuditLog log = logs.get(0);
        assertThat(log.getActor()).isEqualTo("admin_audit");
        assertThat(log.getAction()).isEqualTo(AuditAction.MANUAL_INVENTORY_ADJUSTMENT);
        assertThat(log.getEntityType()).isEqualTo("INVENTORY");
        assertThat(log.getEntityId()).isEqualTo(testInventory.getId().toString());
        assertThat(log.getReason()).isEqualTo("Damaged stock");
        
        // Assert Metadata is correctly serialized/deserialized
        assertThat(log.getMetadata()).isNotNull();
        assertThat(log.getMetadata().get("quantityChange")).isEqualTo(-5);
        
        List<InventoryTransaction> txs = transactionRepository.findAll();
        boolean hasManualTx = txs.stream().anyMatch(t -> t.getTransactionType() == TransactionType.MANUAL_ADJUSTMENT);
        assertThat(hasManualTx).isTrue();
    }

    @Test
    void failedAdjustmentShouldNotCreateAuditLog() {
        // Act
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.manualAdjustStock(testInventory.getId(), -500, "Too much damage", testAdmin.getUsername());
        });

        // Assert
        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).isEmpty(); // The transaction rolled back, so no log should be created
    }
}
