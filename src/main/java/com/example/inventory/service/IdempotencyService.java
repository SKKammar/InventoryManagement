package com.example.inventory.service;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.entity.IdempotencyRecord;
import com.example.inventory.entity.User;
import com.example.inventory.enums.IdempotencyStatus;
import com.example.inventory.repository.IdempotencyRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    public String generateHash(CreateOrderRequest request) {
        // Canonical string: "varId:qty,varId:qty..." sorted by varId
        String canonicalPayload = request.getItems().stream()
                .sorted(Comparator.comparing(CreateOrderRequest.CreateOrderItemRequest::getProductVariantId))
                .map(item -> item.getProductVariantId() + ":" + item.getQuantity())
                .collect(Collectors.joining(","));
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecord claim(User user, String operationType, String key, String requestHash) {
        try {
            IdempotencyRecord newRecord = new IdempotencyRecord();
            newRecord.setUser(user);
            newRecord.setOperationType(operationType);
            newRecord.setIdempotencyKey(key);
            newRecord.setRequestHash(requestHash);
            newRecord.setStatus(IdempotencyStatus.PROCESSING);
            
            return repository.saveAndFlush(newRecord);
        } catch (DataIntegrityViolationException e) {
            // A record already exists, fetch it
            IdempotencyRecord existing = repository.findByUserAndOperationTypeAndIdempotencyKey(user, operationType, key)
                    .orElseThrow(() -> new IllegalStateException("Idempotency conflict but record not found"));
            
            if (existing.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new IllegalStateException("409 CONFLICT: A request with this idempotency key is already being processed.");
            }
            
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IllegalStateException("409 CONFLICT: The idempotency key has already been used with a different request.");
            }
            
            return existing;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryClaim(Long recordId) {
        IdempotencyRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found"));
        if (record.getStatus() != IdempotencyStatus.FAILED) {
            throw new IllegalStateException("Can only retry failed idempotency records");
        }
        record.setStatus(IdempotencyStatus.PROCESSING);
        repository.saveAndFlush(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long recordId, Long resourceId) {
        IdempotencyRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found"));
        record.setStatus(IdempotencyStatus.SUCCEEDED);
        record.setResourceId(resourceId);
        record.setCompletedAt(LocalDateTime.now());
        repository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long recordId) {
        IdempotencyRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found"));
        record.setStatus(IdempotencyStatus.FAILED);
        record.setCompletedAt(LocalDateTime.now());
        repository.save(record);
    }
}
