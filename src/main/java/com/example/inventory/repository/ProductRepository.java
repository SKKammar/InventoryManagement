package com.example.inventory.repository;

import com.example.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import java.util.Set;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByDeletedFalse();
    
    @EntityGraph(attributePaths = {"variants", "variants.inventories"})
    @Query("SELECT p FROM Product p WHERE p.deleted = false")
    Set<Product> findAllWithVariantsAndInventory();
}
