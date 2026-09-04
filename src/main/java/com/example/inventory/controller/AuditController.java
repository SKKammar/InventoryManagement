package com.example.inventory.controller;

import com.example.inventory.dto.AuditLogDTO;
import com.example.inventory.entity.AuditLog;
import com.example.inventory.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> auditLogs = auditLogRepository.findAll(pageable);

        Page<AuditLogDTO> dtoPage = auditLogs.map(log -> {
            AuditLogDTO dto = new AuditLogDTO();
            dto.setId(log.getId());
            dto.setActor(log.getActor());
            dto.setAction(log.getAction());
            dto.setEntityType(log.getEntityType());
            dto.setEntityId(log.getEntityId());
            dto.setReason(log.getReason());
            dto.setMetadata(log.getMetadata());
            dto.setCreatedAt(log.getCreatedAt());
            return dto;
        });

        return ResponseEntity.ok(dtoPage);
    }
}
