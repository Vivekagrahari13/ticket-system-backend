package com.ticket.system.ticket.repository;

import com.ticket.system.ticket.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord,Long> {
    Optional<IdempotencyRecord> findByUserIdAndIdemKey(Long userId, String idemKey);
}
