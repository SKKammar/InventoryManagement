package com.example.inventory.dto;

import com.example.inventory.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private String username;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItemDTO> items;
}
