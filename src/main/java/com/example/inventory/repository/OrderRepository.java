package com.example.inventory.repository;

import com.example.inventory.entity.Order;
import com.example.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}
