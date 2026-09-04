package com.example.inventory.dto;

import com.example.inventory.enums.TransactionType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InventoryTransactionDTO {
    private Long id;
    private Long inventoryId;
    private Integer quantityChange;
    private TransactionType transactionType;
    private String referenceType;
    private String referenceId;
    private String reason;
    private String createdBy;
    private LocalDateTime createdAt;
    private Integer resultingQuantity;
}
