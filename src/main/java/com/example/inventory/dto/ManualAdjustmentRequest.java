package com.example.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManualAdjustmentRequest {
    @NotNull
    private Integer quantityChange;
    
    @NotBlank
    private String reason;
}
