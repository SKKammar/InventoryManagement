package com.example.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateOrderRequest {
    
    @NotEmpty
    private List<CreateOrderItemRequest> items;

    @Data
    public static class CreateOrderItemRequest {
        @NotNull
        private Long productId;

        @NotNull
        @Min(1)
        private Integer quantity;
    }
}
