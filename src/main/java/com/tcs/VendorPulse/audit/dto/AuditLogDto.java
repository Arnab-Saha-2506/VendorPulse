package com.tcs.VendorPulse.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private Instant occurredAt;
    private UUID userId;
    private String userName;
    private String action;
    private String recordType;
    private String recordId;
    private String recordLabel;
}

