package com.example.inventory.controller;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.dto.OrderDTO;
import com.example.inventory.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }
}
