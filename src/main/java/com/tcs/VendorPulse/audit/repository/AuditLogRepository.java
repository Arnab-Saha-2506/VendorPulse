package com.tcs.VendorPulse.audit.repository;

import com.tcs.VendorPulse.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends Repository<AuditLog, Long> {
    AuditLog save(AuditLog auditLog);
    Optional<AuditLog> findById(Long id);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:from IS NULL OR a.occurredAt >= :from) AND " +
           "(:to IS NULL OR a.occurredAt <= :to) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:action IS NULL OR LOWER(a.action) = LOWER(:action)) " +
           "ORDER BY a.occurredAt DESC")
    Page<AuditLog> searchAuditLogs(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("userId") UUID userId,
            @Param("action") String action,
            Pageable pageable);
}

