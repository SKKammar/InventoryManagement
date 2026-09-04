package com.example.inventory.service;

import com.example.inventory.entity.AuditLog;
import com.example.inventory.enums.AuditAction;
import com.example.inventory.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void logAction(AuditAction action, String entityType, String entityId, String reason, Map<String, Object> metadata) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth != null && auth.getName() != null && !auth.getName().isEmpty()) ? auth.getName() : "SYSTEM";

        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setReason(reason);
        log.setMetadata(metadata);

        auditLogRepository.save(log);
    }
}
