package com.example.inventory.repository;

import com.example.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.inventory.id = :inventoryId")
    Page<InventoryTransaction> findByInventoryId(@Param("inventoryId") Long inventoryId, Pageable pageable);
}
