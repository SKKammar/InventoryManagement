package com.example.inventory.config;

import com.example.inventory.entity.Product;
import com.example.inventory.entity.User;
import com.example.inventory.enums.RoleType;
import com.example.inventory.repository.ProductRepository;
import com.example.inventory.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, ProductRepository productRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
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

        if (productRepository.count() == 0) {
            Product p1 = new Product();
            p1.setSku("PROD-001");
            p1.setName("Premium Wireless Mouse");
            p1.setPrice(new BigDecimal("49.99"));
            p1.setStockQuantity(150);
            p1.setCategory("Electronics");
            productRepository.save(p1);

            Product p2 = new Product();
            p2.setSku("PROD-002");
            p2.setName("Mechanical Keyboard");
            p2.setPrice(new BigDecimal("129.99"));
            p2.setStockQuantity(85);
            p2.setCategory("Electronics");
            productRepository.save(p2);

            Product p3 = new Product();
            p3.setSku("PROD-003");
            p3.setName("Ergonomic Office Chair");
            p3.setPrice(new BigDecimal("299.99"));
            p3.setStockQuantity(5); // Low stock
            p3.setCategory("Furniture");
            productRepository.save(p3);
        }
    }
}
