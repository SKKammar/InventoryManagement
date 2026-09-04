package com.example.inventory.repository;

import com.example.inventory.entity.OrderItemAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemAllocationRepository extends JpaRepository<OrderItemAllocation, Long> {
    List<OrderItemAllocation> findByOrderItemId(Long orderItemId);
}
