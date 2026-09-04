package com.example.inventory.service;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.inventory.entity.Inventory;
import com.example.inventory.entity.Order;
import com.example.inventory.entity.OrderItem;
import com.example.inventory.entity.ProductVariant;
import com.example.inventory.entity.User;
import com.example.inventory.mapper.OrderMapper;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.OrderRepository;
import com.example.inventory.repository.ProductVariantRepository;
import com.example.inventory.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final InventoryReservationService reservationService;
    private final AuditService auditService;

    public OrderService(OrderRepository orderRepository,
                        ProductVariantRepository productVariantRepository,
                        InventoryRepository inventoryRepository,
                        InventoryService inventoryService,
                        InventoryReservationService reservationService,
                        UserRepository userRepository,
                        OrderMapper orderMapper,
                        AuditService auditService) {
        this.orderRepository = orderRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.reservationService = reservationService;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
        this.auditService = auditService;
    }

    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toDto);
    }

    public Page<OrderDTO> getMyOrders(Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return orderRepository.findByUser(user, pageable).map(orderMapper::toDto);
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order = orderRepository.save(order); // Save to generate ID for reservations

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.CreateOrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemReq.getProductVariantId())
                    .filter(v -> v.getActive() && !v.getProduct().getDeleted())
                    .orElseThrow(() -> new NoSuchElementException("Product Variant not found: " + itemReq.getProductVariantId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(variant.getPrice());
            
            order.addItem(orderItem);

            // Reserve stock (throws exception and rolls back if unavailable)
            reservationService.reserve(orderItem);
            
            totalAmount = totalAmount.add(variant.getPrice().multiply(new BigDecimal(itemReq.getQuantity())));
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(totalAmount);
        orderRepository.save(order);
        
        auditService.logAction(com.example.inventory.enums.AuditAction.ORDER_CONFIRMED, "ORDER", order.getId().toString(), "Order verified and confirmed", null);
        
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDTO processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        if (order.getStatus() == OrderStatus.PROCESSING) {
            return orderMapper.toDto(order); // Idempotent return
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order must be CONFIRMED to start processing");
        }

        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
        
        auditService.logAction(com.example.inventory.enums.AuditAction.ORDER_PROCESSING_STARTED, "ORDER", order.getId().toString(), null, null);

        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDTO completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            return orderMapper.toDto(order); // Idempotent return
        }

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Order must be PROCESSING to be completed");
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        reservationService.consume(order);
        
        auditService.logAction(com.example.inventory.enums.AuditAction.ORDER_COMPLETED, "ORDER", order.getId().toString(), null, null);

        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                
        if (!isAdmin && !order.getUser().getUsername().equals(currentUsername)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to cancel this order.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return orderMapper.toDto(order); // Idempotent return
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel an order that is already COMPLETED");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        reservationService.release(order);
        
        auditService.logAction(com.example.inventory.enums.AuditAction.ORDER_CANCELLED, "ORDER", order.getId().toString(), null, null);

        return orderMapper.toDto(order);
    }
}
