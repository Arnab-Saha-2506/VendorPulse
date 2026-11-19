package com.tcs.VendorPulse.auth.security;

import com.tcs.VendorPulse.auth.domain.UserSession;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SessionRegistryService {

    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final long refreshTokenExpirationMs;

    public SessionRegistryService(
            @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public UserSession createSession(User user, String device, String refreshTokenHash) {
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshTokenExpirationMs);

        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userId(user.getId())
                .userCode(user.getUserCode())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .supplierId(user.getSupplierId())
                .device(device != null ? device : "Unknown Device")
                .createdAt(now)
                .lastActivityAt(now)
                .expiresAt(expiresAt)
                .refreshTokenHash(refreshTokenHash)
                .revoked(false)
                .build();

        sessions.put(sessionId, session);
        return session;
    }

    public Optional<UserSession> getSession(String sessionId) {
        if (sessionId == null) return Optional.empty();
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public boolean validateSession(String sessionId) {
        if (sessionId == null) return false;
        UserSession session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }
        if (session.isRevoked() || session.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }
        session.setLastActivityAt(Instant.now());
        return true;
    }

    public void updateRefreshToken(String sessionId, String newRefreshTokenHash) {
        UserSession session = sessions.get(sessionId);
        if (session == null || session.isRevoked()) {
            throw new ApiException(ErrorCode.SESSION_REVOKED);
        }
        session.setRefreshTokenHash(newRefreshTokenHash);
        session.setLastActivityAt(Instant.now());
    }

    public void revokeSession(String sessionId) {
        UserSession session = sessions.get(sessionId);
        if (session != null) {
            session.setRevoked(true);
        }
    }

    public void revokeAllUserSessions(UUID userId) {
        sessions.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .forEach(s -> s.setRevoked(true));
    }

    public List<UserSession> getUserSessions(UUID userId) {
        return sessions.values().stream()
                .filter(s -> s.getUserId().equals(userId) && !s.isRevoked() && s.getExpiresAt().isAfter(Instant.now()))
                .sorted(Comparator.comparing(UserSession::getLastActivityAt).reversed())
                .collect(Collectors.toList());
    }
}

