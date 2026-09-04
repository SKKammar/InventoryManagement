package com.example.inventory.config;

import com.example.inventory.entity.Inventory;
import com.example.inventory.entity.Product;
import com.example.inventory.entity.ProductVariant;
import com.example.inventory.entity.User;
import com.example.inventory.entity.Warehouse;
import com.example.inventory.enums.RoleType;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ProductRepository;
import com.example.inventory.repository.ProductVariantRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.repository.WarehouseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final com.example.inventory.service.InventoryService inventoryService;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, 
                      ProductRepository productRepository, 
                      ProductVariantRepository productVariantRepository,
                      WarehouseRepository warehouseRepository,
                      InventoryRepository inventoryRepository,
                      com.example.inventory.service.InventoryService inventoryService,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(RoleType.ADMIN);
            userRepository.save(admin);
            
            User user = new User();
            user.setUsername("user");
            user.setEmail("user@example.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRole(RoleType.CUSTOMER);
            userRepository.save(user);
        }

        if (warehouseRepository.count() == 0) {
            Warehouse mainWarehouse = new Warehouse();
            mainWarehouse.setCode("DEFAULT-WH");
            mainWarehouse.setName("Main Warehouse");
            mainWarehouse.setLocation("Bangalore");
            mainWarehouse.setPriority(1);
            warehouseRepository.save(mainWarehouse);
        }

        if (productRepository.count() == 0) {
            Warehouse defaultWh = warehouseRepository.findByCode("DEFAULT-WH").orElseThrow();

            createProductWithInventory("Premium Wireless Mouse", "Electronics", "PROD-001", new BigDecimal("49.99"), 150, defaultWh);
            createProductWithInventory("Mechanical Keyboard", "Electronics", "PROD-002", new BigDecimal("129.99"), 85, defaultWh);
            createProductWithInventory("Ergonomic Office Chair", "Furniture", "PROD-003", new BigDecimal("299.99"), 5, defaultWh);
        }
    }

    private void createProductWithInventory(String name, String category, String sku, BigDecimal price, int qty, Warehouse wh) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p = productRepository.save(p);

        ProductVariant pv = new ProductVariant();
        pv.setProduct(p);
        pv.setSku(sku);
        pv.setPrice(price);
        pv = productVariantRepository.save(pv);

        Inventory inv = new Inventory();
        inv.setProductVariant(pv);
        inv.setWarehouse(wh);
        inv.setQuantityOnHand(0); // Set to 0 initially
        inv = inventoryRepository.save(inv);

        inventoryService.adjustStock(inv, qty, com.example.inventory.enums.TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial Seed", "SYSTEM");
    }
}
