package com.example.inventory.dto;

import lombok.Data;

@Data
public class OrderItemAllocationDTO {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Integer quantity;
}
