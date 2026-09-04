package com.example.inventory.service;

import com.example.inventory.dto.CreateInventoryTransferRequest;
import com.example.inventory.dto.InventoryTransferDTO;
import com.example.inventory.entity.Inventory;
import com.example.inventory.entity.InventoryTransfer;
import com.example.inventory.entity.ProductVariant;
import com.example.inventory.entity.Warehouse;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.InventoryTransferRepository;
import com.example.inventory.repository.ProductVariantRepository;
import com.example.inventory.repository.WarehouseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class InventoryTransferService {

    private final InventoryTransferRepository transferRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;
    private final MetricsService metricsService;

    public InventoryTransferService(InventoryTransferRepository transferRepository, 
                                    InventoryRepository inventoryRepository, 
                                    ProductVariantRepository productVariantRepository, 
                                    WarehouseRepository warehouseRepository, 
                                    InventoryService inventoryService,
                                    AuditService auditService,
                                    MetricsService metricsService) {
        this.transferRepository = transferRepository;
        this.inventoryRepository = inventoryRepository;
        this.productVariantRepository = productVariantRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.metricsService = metricsService;
    }

    @Transactional
    public InventoryTransferDTO transfer(CreateInventoryTransferRequest request) {
        if (request.getSourceWarehouseId().equals(request.getDestinationWarehouseId())) {
            throw new IllegalArgumentException("Source and destination warehouse must be different");
        }
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be positive");
        }

        ProductVariant variant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new NoSuchElementException("ProductVariant not found"));
        
        Warehouse sourceWh = warehouseRepository.findById(request.getSourceWarehouseId())
                .orElseThrow(() -> new NoSuchElementException("Source Warehouse not found"));
                
        Warehouse destWh = warehouseRepository.findById(request.getDestinationWarehouseId())
                .orElseThrow(() -> new NoSuchElementException("Destination Warehouse not found"));

        Inventory sourceInv = inventoryRepository.findByProductVariantAndWarehouse(variant, sourceWh)
                .orElseThrow(() -> new IllegalArgumentException("Source inventory does not exist"));

        if (sourceInv.getAvailableQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient available stock in source warehouse");
        }

        Inventory destInv = inventoryRepository.findByProductVariantAndWarehouse(variant, destWh)
                .orElseGet(() -> {
                    Inventory newInv = new Inventory();
                    newInv.setProductVariant(variant);
                    newInv.setWarehouse(destWh);
                    newInv.setQuantityOnHand(0);
                    newInv.setQuantityReserved(0);
                    try {
                        return inventoryRepository.saveAndFlush(newInv);
                    } catch (DataIntegrityViolationException e) {
                        return inventoryRepository.findByProductVariantAndWarehouse(variant, destWh)
                                .orElseThrow(() -> new IllegalStateException("Failed to fetch or create destination inventory"));
                    }
                });

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Create transfer record to get ID
        InventoryTransfer transfer = new InventoryTransfer();
        transfer.setProductVariant(variant);
        transfer.setSourceWarehouse(sourceWh);
        transfer.setDestinationWarehouse(destWh);
        transfer.setQuantity(request.getQuantity());
        transfer.setReason(request.getReason());
        transfer.setCreatedBy(username);
        transfer = transferRepository.save(transfer);

        try {
            // 2. Adjust stock (creates ledger entries safely)
            String refId = transfer.getId().toString();
            
            inventoryService.adjustStock(sourceInv, -request.getQuantity(), 
                    TransactionType.TRANSFER_OUT, "INVENTORY_TRANSFER", refId, 
                    request.getReason(), username);

            inventoryService.adjustStock(destInv, request.getQuantity(), 
                    TransactionType.TRANSFER_IN, "INVENTORY_TRANSFER", refId, 
                    request.getReason(), username);
                    
            auditService.logAction(
                    com.example.inventory.enums.AuditAction.INVENTORY_TRANSFER_CREATED, 
                    "INVENTORY_TRANSFER", 
                    refId, 
                    request.getReason(), 
                    java.util.Map.of("sourceWarehouseId", sourceWh.getId(), "destinationWarehouseId", destWh.getId(), "quantity", request.getQuantity())
            );

            metricsService.incrementInventoryTransfer("success");
            return toDto(transfer);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            metricsService.incrementConcurrencyConflict();
            metricsService.incrementInventoryTransfer("failure");
            throw e;
        } catch (Exception e) {
            metricsService.incrementInventoryTransfer("failure");
            throw e;
        }
    }

    private InventoryTransferDTO toDto(InventoryTransfer transfer) {
        InventoryTransferDTO dto = new InventoryTransferDTO();
        dto.setId(transfer.getId());
        dto.setProductVariantId(transfer.getProductVariant().getId());
        dto.setSourceWarehouseId(transfer.getSourceWarehouse().getId());
        dto.setDestinationWarehouseId(transfer.getDestinationWarehouse().getId());
        dto.setQuantity(transfer.getQuantity());
        dto.setReason(transfer.getReason());
        dto.setCreatedBy(transfer.getCreatedBy());
        dto.setCreatedAt(transfer.getCreatedAt());
        return dto;
    }
}
