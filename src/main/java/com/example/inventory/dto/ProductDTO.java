package com.example.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDTO {
    private Long id;
    
    @NotBlank
    private String sku;
    
    @NotBlank
    private String name;
    
    private String description;
    
    @NotNull
    private BigDecimal price;
    
    @NotNull
    private Integer stockQuantity;
    
    private String category;
}
