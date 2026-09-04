package com.example.inventory.dto;

import com.example.inventory.enums.AuditAction;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AuditLogDTO {
    private Long id;
    private String actor;
    private AuditAction action;
    private String entityType;
    private String entityId;
    private String reason;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
