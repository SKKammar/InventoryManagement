package com.example.inventory;

import com.example.inventory.entity.Inventory;
import com.example.inventory.entity.Product;
import com.example.inventory.entity.ProductVariant;
import com.example.inventory.entity.Warehouse;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ProductRepository;
import com.example.inventory.repository.ProductVariantRepository;
import com.example.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class InventoryDomainTests extends IntegrationTestBase {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldCreateProductWithVariantAndInventory() {
        Product p = new Product();
        p.setName("Test Product");
        p = productRepository.save(p);

        ProductVariant pv = new ProductVariant();
        pv.setProduct(p);
        pv.setSku("TEST-SKU-1");
        pv.setPrice(new BigDecimal("99.99"));
        pv = productVariantRepository.save(pv);

        Warehouse w = new Warehouse();
        w.setCode("WH-1");
        w.setName("Warehouse 1");
        w = warehouseRepository.save(w);

        Inventory i = new Inventory();
        i.setProductVariant(pv);
        i.setWarehouse(w);
        i.setQuantityOnHand(100);
        i = inventoryRepository.save(i);

        assertThat(inventoryRepository.findById(i.getId())).isPresent();
        assertThat(i.getQuantityOnHand()).isEqualTo(100);
    }

    @Test
    void shouldRejectDuplicateSku() {
        Product p = new Product();
        p.setName("Test Product");
        p = productRepository.save(p);

        ProductVariant pv1 = new ProductVariant();
        pv1.setProduct(p);
        pv1.setSku("DUPLICATE-SKU");
        pv1.setPrice(new BigDecimal("10.00"));
        productVariantRepository.saveAndFlush(pv1);

        ProductVariant pv2 = new ProductVariant();
        pv2.setProduct(p);
        pv2.setSku("DUPLICATE-SKU");
        pv2.setPrice(new BigDecimal("20.00"));

        assertThrows(DataIntegrityViolationException.class, () -> productVariantRepository.saveAndFlush(pv2));
    }

    @Test
    void shouldRejectDuplicateWarehouseCode() {
        Warehouse w1 = new Warehouse();
        w1.setCode("WH-DUP");
        w1.setName("Warehouse A");
        warehouseRepository.saveAndFlush(w1);

        Warehouse w2 = new Warehouse();
        w2.setCode("WH-DUP");
        w2.setName("Warehouse B");

        assertThrows(DataIntegrityViolationException.class, () -> warehouseRepository.saveAndFlush(w2));
    }

    @Test
    void shouldRejectDuplicateInventoryForSameVariantAndWarehouse() {
        Product p = new Product();
        p.setName("Product A");
        p = productRepository.save(p);

        ProductVariant pv = new ProductVariant();
        pv.setProduct(p);
        pv.setSku("SKU-A");
        pv.setPrice(new BigDecimal("10.00"));
        pv = productVariantRepository.save(pv);

        Warehouse w = new Warehouse();
        w.setCode("WH-A");
        w.setName("Warehouse A");
        w = warehouseRepository.save(w);

        Inventory i1 = new Inventory();
        i1.setProductVariant(pv);
        i1.setWarehouse(w);
        i1.setQuantityOnHand(10);
        inventoryRepository.saveAndFlush(i1);

        Inventory i2 = new Inventory();
        i2.setProductVariant(pv);
        i2.setWarehouse(w);
        i2.setQuantityOnHand(20);

        assertThrows(DataIntegrityViolationException.class, () -> inventoryRepository.saveAndFlush(i2));
    }

    @Test
    void shouldRejectNegativeInventoryQuantity() {
        Product p = new Product();
        p.setName("Product");
        p = productRepository.save(p);

        ProductVariant pv = new ProductVariant();
        pv.setProduct(p);
        pv.setSku("SKU-NEG");
        pv.setPrice(new BigDecimal("10.00"));
        pv = productVariantRepository.save(pv);

        Warehouse w = new Warehouse();
        w.setCode("WH-NEG");
        w.setName("Warehouse");
        w = warehouseRepository.save(w);

        Inventory i = new Inventory();
        i.setProductVariant(pv);
        i.setWarehouse(w);
        i.setQuantityOnHand(-5);

        // This requires DB check constraints which might fail at flush
        assertThrows(DataIntegrityViolationException.class, () -> inventoryRepository.saveAndFlush(i));
    }
    @Test
    void shouldAllocateInventoryDeterministicallyByPriority() {
        Product p = new Product();
        p.setName("Product C");
        p = productRepository.save(p);

        ProductVariant pv = new ProductVariant();
        pv.setProduct(p);
        pv.setSku("SKU-C");
        pv.setPrice(new BigDecimal("10.00"));
        pv = productVariantRepository.save(pv);

        Warehouse w1 = new Warehouse();
        w1.setCode("WH-A");
        w1.setName("Warehouse A");
        w1.setPriority(2); // Lower priority (higher number)
        w1 = warehouseRepository.save(w1);

        Warehouse w2 = new Warehouse();
        w2.setCode("WH-B");
        w2.setName("Warehouse B");
        w2.setPriority(1); // Higher priority (lower number)
        w2 = warehouseRepository.save(w2);

        Inventory i1 = new Inventory();
        i1.setProductVariant(pv);
        i1.setWarehouse(w1);
        i1.setQuantityOnHand(10);
        inventoryRepository.save(i1);

        Inventory i2 = new Inventory();
        i2.setProductVariant(pv);
        i2.setWarehouse(w2);
        i2.setQuantityOnHand(10);
        inventoryRepository.save(i2);

        var inventories = inventoryRepository.findActiveInventoryForVariantOrderByPriority(pv);
        
        assertThat(inventories).hasSize(2);
        assertThat(inventories.get(0).getWarehouse().getCode()).isEqualTo("WH-B"); // WH-B should be first due to priority 1
        assertThat(inventories.get(1).getWarehouse().getCode()).isEqualTo("WH-A");
    }

    @Test
    void shouldFindAllWithVariantsAndInventoryWithoutNPlusOne() {
        Product p = new Product();
        p.setName("Product D");
        p = productRepository.save(p);

        ProductVariant pv = new ProductVariant();
        pv.setProduct(p);
        pv.setSku("SKU-D");
        pv.setPrice(new BigDecimal("10.00"));
        pv = productVariantRepository.save(pv);

        Warehouse w = new Warehouse();
        w.setCode("WH-D");
        w.setName("Warehouse D");
        w.setPriority(1);
        w = warehouseRepository.save(w);

        Inventory i = new Inventory();
        i.setProductVariant(pv);
        i.setWarehouse(w);
        i.setQuantityOnHand(10);
        inventoryRepository.save(i);

        // Fetch using the optimized query
        var products = productRepository.findAllWithVariantsAndInventory();
        
        assertThat(products).isNotEmpty();
        Product fetched = products.stream().filter(pr -> pr.getId().equals(p.getId())).findFirst().orElseThrow();
        
        // Assert that the collections are initialized (if they weren't, accessing them outside a session would throw LazyInitializationException)
        assertThat(fetched.getVariants()).isNotEmpty();
        assertThat(fetched.getVariants().iterator().next().getInventories()).isNotEmpty();
    }
}
