package com.example.inventory.dto;

import com.example.inventory.enums.ReservationStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InventoryReservationDTO {
    private Long id;
    private Long inventoryId;
    private Long orderId;
    private Integer quantity;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime releasedAt;
}
