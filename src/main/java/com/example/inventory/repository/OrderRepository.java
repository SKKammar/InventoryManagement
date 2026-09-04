package com.example.inventory.repository;

import com.example.inventory.entity.Order;
import com.example.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"user", "items"})
    Page<Order> findByUser(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "items"})
    Page<Order> findAll(Pageable pageable);
}
