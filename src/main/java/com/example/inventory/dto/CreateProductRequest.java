package com.example.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;
import java.math.BigDecimal;

@Data
public class CreateProductRequest {
    @NotBlank
    private String name;
    
    private String description;
    
    private Set<CreateProductVariantRequest> variants;
    
    private String category;
    
    @Data
    public static class CreateProductVariantRequest {
        @NotBlank
        private String sku;
        private BigDecimal price;
        private String attributes;
    }
}
