package com.example.inventory.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long id;
    private Long productVariantId;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private java.util.List<OrderItemAllocationDTO> allocations;
}
