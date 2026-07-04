package com.example.inventory.repository;

import com.example.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySkuAndDeletedFalse(String sku);
    List<Product> findByDeletedFalse();
    Boolean existsBySku(String sku);
}
