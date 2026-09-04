package com.example.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;
import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
    @NotBlank
    private String name;
    
    private String description;
    
    private String category;
}
