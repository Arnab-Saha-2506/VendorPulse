package com.tcs.VendorPulse.audit.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_name", length = 120, nullable = false)
    private String userName;

    @Column(name = "action", length = 80, nullable = false)
    private String action;

    @Column(name = "record_type", length = 40, nullable = false)
    private String recordType;

    @Column(name = "record_id", length = 60)
    private String recordId;

    @Column(name = "record_label", length = 160)
    private String recordLabel;
}

