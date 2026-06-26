package com.example.inventory.repository;

import com.example.inventory.entity.InventoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
    List<InventoryHistory> findByProductIdOrderByCreatedAtDesc(Long productId);
}