package com.example.inventory.dto;

import lombok.Data;

@Data
public class InventoryDTO {
    private Long id;
    private WarehouseDTO warehouse;
    private Integer quantityOnHand;
}
