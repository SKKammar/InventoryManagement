package com.example.inventory.service;

import com.example.inventory.dto.request.OrderItemRequest;
import com.example.inventory.dto.response.OrderDTO;
import com.example.inventory.entity.Order;
import com.example.inventory.entity.OrderItem;
import com.example.inventory.entity.Product;
import com.example.inventory.enums.OrderStatus;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.mapper.OrderMapper;
import com.example.inventory.repository.OrderRepository;
import com.example.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderDTO createOrder(Long userId, List<OrderItemRequest> itemRequests) {
        Order order = new Order();
        order.setUserId(userId);

        for (OrderItemRequest req : itemRequests) {
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + req.getProductId()));

            // Deduct inventory using the safe service (handles @Version)
            inventoryService.deductStock(req.getProductId(), req.getQuantity(), "ORDER_PLACED");

            OrderItem item = new OrderItem();
            item.setProductId(req.getProductId());
            item.setQuantity(req.getQuantity());
            item.setPrice(product.getPrice());
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {} for User: {}", savedOrder.getId(), userId);
        return orderMapper.toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUser(Long userId, Long requestingUserId, boolean isAdmin) {
        if (!isAdmin && !userId.equals(requestingUserId)) {
            throw new AccessDeniedException("You can only view your own orders.");
        }
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        order.setStatusSafely(newStatus); // Uses the state machine
        Order updated = orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, newStatus);
        return orderMapper.toDto(updated);
    }
}