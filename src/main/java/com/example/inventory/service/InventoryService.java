package com.example.inventory.service;

import com.example.inventory.entity.Inventory;
import com.example.inventory.entity.InventoryTransaction;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.InventoryTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final AuditService auditService;

    public InventoryService(InventoryRepository inventoryRepository, 
                            InventoryTransactionRepository inventoryTransactionRepository,
                            AuditService auditService) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public InventoryTransaction adjustStock(Inventory inventory, int quantityChange, 
                                            TransactionType type, String refType, 
                                            String refId, String reason, String createdBy) {
        if (quantityChange == 0) {
            throw new IllegalArgumentException("Quantity change cannot be zero");
        }

        // Update balance
        int newBalance = inventory.getQuantityOnHand() + quantityChange;
        if (newBalance < inventory.getQuantityReserved()) {
            throw new IllegalArgumentException("Quantity change would reduce on-hand below reserved quantity for warehouse: " + inventory.getWarehouse().getCode());
        }
        
        inventory.setQuantityOnHand(newBalance);
        inventoryRepository.save(inventory); // Triggers optimistic locking

        // Append to ledger
        InventoryTransaction tx = new InventoryTransaction();
        tx.setInventory(inventory);
        tx.setQuantityChange(quantityChange);
        tx.setTransactionType(type);
        tx.setReferenceType(refType);
        tx.setReferenceId(refId);
        tx.setReason(reason);
        tx.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");
        tx.setResultingQuantity(newBalance);

        return inventoryTransactionRepository.save(tx);
    }

    @Transactional
    public InventoryTransaction manualAdjustStock(Long inventoryId, int quantityChange, String reason, String username) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Inventory not found"));
        
        int oldQuantity = inventory.getQuantityOnHand();
        
        InventoryTransaction tx = adjustStock(inventory, quantityChange, TransactionType.MANUAL_ADJUSTMENT, "MANUAL", null, reason, username);
        
        auditService.logAction(
                com.example.inventory.enums.AuditAction.MANUAL_INVENTORY_ADJUSTMENT,
                "INVENTORY",
                inventory.getId().toString(),
                reason,
                java.util.Map.of(
                        "quantityBefore", oldQuantity,
                        "quantityChange", quantityChange,
                        "quantityAfter", oldQuantity + quantityChange
                )
        );
        
        return tx;
    }
}
