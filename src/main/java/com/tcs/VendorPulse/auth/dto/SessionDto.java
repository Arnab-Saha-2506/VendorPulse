package com.tcs.VendorPulse.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    private String sessionId;
    private String device;
    private Instant createdAt;
    private Instant lastActivityAt;
    private Instant expiresAt;
    private boolean isCurrent;
}

