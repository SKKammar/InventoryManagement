package com.example.inventory.repository;

import com.example.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    boolean existsByCode(String code);
    Optional<Warehouse> findByCode(String code);
}
