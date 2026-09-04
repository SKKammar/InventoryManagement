package com.example.inventory.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InventoryTransferDTO {
    private Long id;
    private Long productVariantId;
    private Long sourceWarehouseId;
    private Long destinationWarehouseId;
    private Integer quantity;
    private String reason;
    private String createdBy;
    private LocalDateTime createdAt;
}
