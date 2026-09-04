package com.example.inventory.repository;

import com.example.inventory.entity.Inventory;
import com.example.inventory.entity.ProductVariant;
import com.example.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductVariantAndWarehouse(ProductVariant variant, Warehouse warehouse);
    List<Inventory> findByProductVariant(ProductVariant variant);
    
    @Query("SELECT i FROM Inventory i JOIN FETCH i.warehouse w WHERE i.productVariant = :variant AND w.active = true ORDER BY w.priority ASC")
    List<Inventory> findActiveInventoryForVariantOrderByPriority(@Param("variant") ProductVariant variant);
}
