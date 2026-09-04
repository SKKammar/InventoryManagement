package com.example.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Set;

@Data
public class ProductVariantDTO {
    private Long id;
    
    @NotBlank
    private String sku;
    
    @NotNull
    private BigDecimal price;
    
    private Boolean active;
    
    private Set<InventoryDTO> inventories;
}
