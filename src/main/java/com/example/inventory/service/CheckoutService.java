package com.example.inventory.service;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.dto.OrderDTO;
import com.example.inventory.entity.IdempotencyRecord;
import com.example.inventory.entity.Order;
import com.example.inventory.entity.User;
import com.example.inventory.enums.IdempotencyStatus;
import com.example.inventory.mapper.OrderMapper;
import com.example.inventory.repository.OrderRepository;
import com.example.inventory.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class CheckoutService {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final InventoryReservationService reservationService;
    private final MetricsService metricsService;

    public CheckoutService(OrderService orderService, IdempotencyService idempotencyService, UserRepository userRepository, OrderRepository orderRepository, OrderMapper orderMapper, InventoryReservationService reservationService, MetricsService metricsService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.reservationService = reservationService;
        this.metricsService = metricsService;
    }

    public OrderDTO checkout(CreateOrderRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String requestHash = idempotencyService.generateHash(request);
        long startTime = System.currentTimeMillis();
        
        IdempotencyRecord record = idempotencyService.claim(user, "CHECKOUT", request.getIdempotencyKey(), requestHash);

        if (record.getStatus() == IdempotencyStatus.SUCCEEDED) {
            metricsService.incrementCheckout("idempotent_replay");
            Order existingOrder = orderRepository.findById(record.getResourceId())
                    .orElseThrow(() -> new IllegalStateException("Idempotent order not found: " + record.getResourceId()));
            return orderMapper.toDto(existingOrder);
        } else if (record.getStatus() == IdempotencyStatus.FAILED) {
            idempotencyService.retryClaim(record.getId());
        }

        try {
            OrderDTO orderDTO = orderService.createOrder(request);
            idempotencyService.markSuccess(record.getId(), orderDTO.getId());
            metricsService.incrementCheckout("success");
            metricsService.recordCheckoutDuration(System.currentTimeMillis() - startTime);
            return orderDTO;
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            idempotencyService.markFailed(record.getId());
            metricsService.incrementConcurrencyConflict();
            metricsService.incrementCheckout("failure");
            throw e;
        } catch (Exception e) {
            idempotencyService.markFailed(record.getId());
            metricsService.incrementCheckout("failure");
            throw e;
        }
    }
}
