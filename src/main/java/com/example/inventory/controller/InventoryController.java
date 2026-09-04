package com.example.inventory.controller;

import com.example.inventory.dto.InventoryTransactionDTO;
import com.example.inventory.entity.InventoryTransaction;
import com.example.inventory.repository.InventoryTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryTransactionRepository transactionRepository;
    private final com.example.inventory.service.InventoryService inventoryService;

    public InventoryController(InventoryTransactionRepository transactionRepository, com.example.inventory.service.InventoryService inventoryService) {
        this.transactionRepository = transactionRepository;
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{inventoryId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryTransactionDTO> adjustInventory(
            @PathVariable Long inventoryId,
            @jakarta.validation.Valid @RequestBody com.example.inventory.dto.ManualAdjustmentRequest request,
            java.security.Principal principal) {
        
        InventoryTransaction tx = inventoryService.manualAdjustStock(
                inventoryId, request.getQuantityChange(), request.getReason(), principal.getName());
                
        InventoryTransactionDTO dto = new InventoryTransactionDTO();
        dto.setId(tx.getId());
        dto.setInventoryId(tx.getInventory().getId());
        dto.setQuantityChange(tx.getQuantityChange());
        dto.setTransactionType(tx.getTransactionType());
        dto.setReferenceType(tx.getReferenceType());
        dto.setReferenceId(tx.getReferenceId());
        dto.setReason(tx.getReason());
        dto.setCreatedBy(tx.getCreatedBy());
        dto.setCreatedAt(tx.getCreatedAt());
        dto.setResultingQuantity(tx.getResultingQuantity());
        
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{inventoryId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<Page<InventoryTransactionDTO>> getInventoryTransactions(
            @PathVariable Long inventoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InventoryTransaction> transactions = transactionRepository.findByInventoryId(inventoryId, pageable);
        
        Page<InventoryTransactionDTO> dtoPage = transactions.map(tx -> {
            InventoryTransactionDTO dto = new InventoryTransactionDTO();
            dto.setId(tx.getId());
            dto.setInventoryId(tx.getInventory().getId());
            dto.setQuantityChange(tx.getQuantityChange());
            dto.setTransactionType(tx.getTransactionType());
            dto.setReferenceType(tx.getReferenceType());
            dto.setReferenceId(tx.getReferenceId());
            dto.setReason(tx.getReason());
            dto.setCreatedBy(tx.getCreatedBy());
            dto.setCreatedAt(tx.getCreatedAt());
            dto.setResultingQuantity(tx.getResultingQuantity());
            return dto;
        });

        return ResponseEntity.ok(dtoPage);
    }
}
