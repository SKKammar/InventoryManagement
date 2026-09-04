package com.example.inventory.repository;

import com.example.inventory.entity.IdempotencyRecord;
import com.example.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByUserAndOperationTypeAndIdempotencyKey(User user, String operationType, String idempotencyKey);
}
