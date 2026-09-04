package com.example.inventory;

import com.example.inventory.entity.Inventory;
import com.example.inventory.entity.Product;
import com.example.inventory.entity.ProductVariant;
import com.example.inventory.entity.Warehouse;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ProductRepository;
import com.example.inventory.repository.ProductVariantRepository;
import com.example.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DatabaseConstraintTest extends IntegrationTestBase {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private InventoryRepository inventoryRepository;

    private Warehouse mainWh;
    private ProductVariant testVariant;

    @BeforeEach
    void setup() {
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();

        mainWh = new Warehouse();
        mainWh.setCode("WH-DB-1");
        mainWh.setName("DB Test Warehouse");
        mainWh.setPriority(1);
        warehouseRepository.save(mainWh);

        Product p = new Product();
        p.setName("DB Constraint Test");
        productRepository.save(p);

        testVariant = new ProductVariant();
        testVariant.setProduct(p);
        testVariant.setSku("SKU-DB-1");
        testVariant.setPrice(new BigDecimal("10.00"));
        productVariantRepository.save(testVariant);
    }

    @AfterEach
    void cleanup() {
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
    }

    @Test
    void shouldEnforceNegativeQuantityOnHandConstraint() {
        Inventory inv = new Inventory();
        inv.setProductVariant(testVariant);
        inv.setWarehouse(mainWh);
        inv.setQuantityOnHand(-5); // Negative quantity

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class, () -> {
            inventoryRepository.saveAndFlush(inv);
        });
        assertThat(ex.getMessage()).contains("quantity_on_hand");
    }

    @Test
    void shouldEnforceUniqueInventoryConstraint() {
        Inventory inv1 = new Inventory();
        inv1.setProductVariant(testVariant);
        inv1.setWarehouse(mainWh);
        inv1.setQuantityOnHand(10);
        inventoryRepository.saveAndFlush(inv1);

        Inventory inv2 = new Inventory();
        inv2.setProductVariant(testVariant);
        inv2.setWarehouse(mainWh);
        inv2.setQuantityOnHand(20);

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class, () -> {
            inventoryRepository.saveAndFlush(inv2);
        });
        // Postgres unique constraint violation
        assertThat(ex.getMessage()).contains("inventory_product_variant_id_warehouse_id_key");
    }

    @Test
    void shouldEnforceUniqueWarehouseCodeConstraint() {
        Warehouse wh2 = new Warehouse();
        wh2.setCode("WH-DB-1"); // Duplicate code
        wh2.setName("Duplicate WH");
        wh2.setPriority(2);

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class, () -> {
            warehouseRepository.saveAndFlush(wh2);
        });
        assertThat(ex.getMessage()).contains("warehouses_code_key");
    }
}
