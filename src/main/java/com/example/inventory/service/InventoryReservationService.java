package com.example.inventory.service;

import com.example.inventory.entity.Inventory;
import com.example.inventory.entity.InventoryReservation;
import com.example.inventory.entity.Order;
import com.example.inventory.entity.ProductVariant;
import com.example.inventory.enums.ReservationStatus;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.InventoryReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryReservationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryService inventoryService;

    public InventoryReservationService(InventoryRepository inventoryRepository,
                                       InventoryReservationRepository reservationRepository,
                                       InventoryService inventoryService) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public void reserve(com.example.inventory.entity.OrderItem orderItem) {
        ProductVariant variant = orderItem.getProductVariant();
        int quantity = orderItem.getQuantity();
        Order order = orderItem.getOrder();
        
        List<Inventory> inventories = inventoryRepository.findActiveInventoryForVariantOrderByPriority(variant);
        
        int totalAvailable = inventories.stream()
                .mapToInt(Inventory::getAvailableQuantity)
                .sum();

        if (totalAvailable < quantity) {
            throw new IllegalArgumentException("Insufficient available stock for SKU: " + variant.getSku());
        }

        int remainingToFulfill = quantity;
        for (Inventory inventory : inventories) {
            if (remainingToFulfill <= 0) break;
            
            int availableInWh = inventory.getAvailableQuantity();
            if (availableInWh > 0) {
                int toReserve = Math.min(availableInWh, remainingToFulfill);
                
                // Update reserved quantity (optimistic locking applies here upon save)
                inventory.setQuantityReserved(inventory.getQuantityReserved() + toReserve);
                inventoryRepository.save(inventory);

                InventoryReservation reservation = new InventoryReservation();
                reservation.setInventory(inventory);
                reservation.setOrder(order);
                reservation.setQuantity(toReserve);
                reservation.setStatus(ReservationStatus.ACTIVE);
                reservation.setExpiresAt(LocalDateTime.now().plusMinutes(15)); // 15 min expiry
                reservationRepository.save(reservation);

                com.example.inventory.entity.OrderItemAllocation allocation = new com.example.inventory.entity.OrderItemAllocation();
                allocation.setWarehouse(inventory.getWarehouse());
                allocation.setQuantity(toReserve);
                orderItem.addAllocation(allocation);

                remainingToFulfill -= toReserve;
            }
        }
    }

    @Transactional
    public void consume(Order order) {
        List<InventoryReservation> activeReservations = reservationRepository.findByOrderAndStatus(order, ReservationStatus.ACTIVE);
        
        if (activeReservations.isEmpty()) {
            throw new IllegalStateException("No active reservations found for order: " + order.getId());
        }

        for (InventoryReservation reservation : activeReservations) {
            reservation.setStatus(ReservationStatus.CONSUMED);
            reservationRepository.save(reservation);
            
            Inventory inventory = reservation.getInventory();
            // Decrease reserved quantity
            inventory.setQuantityReserved(inventory.getQuantityReserved() - reservation.getQuantity());
            
            // Deduct actual stock (which will also append to the ledger inside adjustStock)
            inventoryService.adjustStock(inventory, -reservation.getQuantity(), 
                TransactionType.ORDER_DEDUCTION, "ORDER", order.getId().toString(), 
                "Order Consumed", order.getUser().getUsername());
        }
    }

    @Transactional
    public void release(Order order) {
        List<InventoryReservation> activeReservations = reservationRepository.findByOrderAndStatus(order, ReservationStatus.ACTIVE);
        
        for (InventoryReservation reservation : activeReservations) {
            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setReleasedAt(LocalDateTime.now());
            reservationRepository.save(reservation);
            
            Inventory inventory = reservation.getInventory();
            inventory.setQuantityReserved(inventory.getQuantityReserved() - reservation.getQuantity());
            inventoryRepository.save(inventory);
        }
    }

    @Transactional
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    public void expireReservations() {
        List<InventoryReservation> expired = reservationRepository.findExpiredReservations(LocalDateTime.now());
        
        for (InventoryReservation reservation : expired) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setReleasedAt(LocalDateTime.now());
            reservationRepository.save(reservation);
            
            Inventory inventory = reservation.getInventory();
            inventory.setQuantityReserved(inventory.getQuantityReserved() - reservation.getQuantity());
            inventoryRepository.save(inventory);
        }
    }
}
