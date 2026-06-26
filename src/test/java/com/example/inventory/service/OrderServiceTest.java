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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setSku("TEST-001");
        product.setPrice(BigDecimal.valueOf(19.99));
        product.setStockQuantity(100);
        product.setVersion(0L);

        order = new Order();
        order.setId(100L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(39.98));
    }

    @Test
    void createOrder_Success() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderDTO());

        OrderDTO result = orderService.createOrder(1L, List.of(itemRequest));

        assertNotNull(result);
        verify(inventoryService).deductStock(1L, 2, "ORDER_PLACED");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_ProductNotFound_ThrowsException() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(999L);
        itemRequest.setQuantity(1);

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.createOrder(1L, List.of(itemRequest))
        );
        verify(inventoryService, never()).deductStock(any(), any(), any());
    }

    @Test
    void updateOrderStatus_ValidTransition_Success() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderDTO());

        OrderDTO result = orderService.updateOrderStatus(100L, OrderStatus.CONFIRMED);

        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        order.setStatus(OrderStatus.DELIVERED); // Terminal state
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () ->
                orderService.updateOrderStatus(100L, OrderStatus.CONFIRMED)
        );
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_OrderNotFound_ThrowsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.updateOrderStatus(999L, OrderStatus.CONFIRMED)
        );
    }

    @Test
    void getOrdersByUser_AsCustomer_ReturnsOwnOrders() {
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderDTO());

        List<OrderDTO> result = orderService.getOrdersByUser(1L, 1L, false);

        assertEquals(1, result.size());
        verify(orderRepository).findByUserId(1L);
    }

    @Test
    void getOrdersByUser_AsAdmin_ReturnsAnyUserOrders() {
        when(orderRepository.findByUserId(2L)).thenReturn(List.of(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderDTO());

        List<OrderDTO> result = orderService.getOrdersByUser(2L, 1L, true);

        assertEquals(1, result.size());
        verify(orderRepository).findByUserId(2L);
    }
}