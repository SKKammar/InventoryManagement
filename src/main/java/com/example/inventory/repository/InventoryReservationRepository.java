package com.example.inventory.repository;

import com.example.inventory.entity.InventoryReservation;
import com.example.inventory.entity.Order;
import com.example.inventory.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    
    List<InventoryReservation> findByOrder(Order order);
    
    List<InventoryReservation> findByOrderAndStatus(Order order, ReservationStatus status);

    @Query("SELECT r FROM InventoryReservation r WHERE r.status = 'ACTIVE' AND r.expiresAt < :now")
    List<InventoryReservation> findExpiredReservations(LocalDateTime now);
}
