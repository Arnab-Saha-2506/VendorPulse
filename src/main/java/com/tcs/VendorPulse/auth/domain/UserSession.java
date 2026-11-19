package com.tcs.VendorPulse.auth.domain;

import com.tcs.VendorPulse.common.enums.UserRole;
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
public class UserSession {
    private String sessionId;
    private UUID userId;
    private String userCode;
    private String email;
    private String fullName;
    private UserRole role;
    private UUID supplierId;
    private String device;
    private Instant createdAt;
    private Instant lastActivityAt;
    private Instant expiresAt;
    private String refreshTokenHash;
    private boolean revoked;
}

