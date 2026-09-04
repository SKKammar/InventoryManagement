package com.example.inventory.controller;

import com.example.inventory.dto.CreateInventoryTransferRequest;
import com.example.inventory.dto.InventoryTransferDTO;
import com.example.inventory.service.InventoryTransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/transfers")
public class InventoryTransferController {

    private final InventoryTransferService transferService;

    public InventoryTransferController(InventoryTransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryTransferDTO> transfer(@Valid @RequestBody CreateInventoryTransferRequest request) {
        return ResponseEntity.ok(transferService.transfer(request));
    }
}
