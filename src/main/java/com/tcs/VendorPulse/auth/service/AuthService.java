package com.tcs.VendorPulse.auth.service;

import com.tcs.VendorPulse.auth.domain.PasswordReset;
import com.tcs.VendorPulse.auth.domain.UserSession;
import com.tcs.VendorPulse.auth.dto.*;
import com.tcs.VendorPulse.auth.jwt.JwtService;
import com.tcs.VendorPulse.auth.repository.PasswordResetRepository;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.auth.security.RateLimitingService;
import com.tcs.VendorPulse.auth.security.SessionRegistryService;
import com.tcs.VendorPulse.common.enums.AccountStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.user.domain.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionRegistryService sessionRegistryService;
    private final RateLimitingService rateLimitingService;
    private final Optional<JavaMailSender> mailSender;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public LoginResponse login(LoginRequest request, String device) {
        String identifier = request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().trim().toLowerCase()
                : (request.getUserCode() != null ? request.getUserCode().trim() : "");

        rateLimitingService.checkLoginRateLimit(identifier);

        Optional<User> userOpt = request.getEmail() != null && !request.getEmail().isBlank()
                ? userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                : userRepository.findByUserCode(request.getUserCode().trim());

        if (userOpt.isEmpty()) {
            rateLimitingService.recordFailedLogin(identifier);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            rateLimitingService.recordFailedLogin(identifier);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getRole() != request.getRole()) {
            rateLimitingService.recordFailedLogin(identifier);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }

        rateLimitingService.resetLoginAttempts(identifier);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate preliminary session ID for token creation
        String tempSessionId = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user.getId(), tempSessionId);
        String refreshTokenHash = jwtService.hashToken(refreshToken);

        UserSession session = sessionRegistryService.createSession(user, device, refreshTokenHash);
        // Regenerate tokens with finalized sessionId
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getSupplierId(), session.getSessionId());
        String finalRefreshToken = jwtService.generateRefreshToken(user.getId(), session.getSessionId());
        sessionRegistryService.updateRefreshToken(session.getSessionId(), jwtService.hashToken(finalRefreshToken));

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .supplierId(user.getSupplierId())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(finalRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .user(userSummary)
                .sessionId(session.getSessionId())
                .build();
    }

    public UserSummaryDto getMe() {
        CurrentUser currentUser = CurrentUser.get();
        if (currentUser == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return UserSummaryDto.builder()
                .id(currentUser.getId())
                .fullName(currentUser.getFullName())
                .email(currentUser.getEmail())
                .role(currentUser.getRole())
                .supplierId(currentUser.getSupplierId())
                .build();
    }

    public LoginResponse refreshToken(RefreshTokenRequest request, String device) {
        String token = request.getRefreshToken();
        if (!jwtService.isTokenValid(token)) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Claims claims = jwtService.extractClaims(token);
        if (claims == null || !"REFRESH".equals(claims.get("type", String.class))) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String sessionId = claims.get("sid", String.class);
        Optional<UserSession> sessionOpt = sessionRegistryService.getSession(sessionId);
        if (sessionOpt.isEmpty() || sessionOpt.get().isRevoked()) {
            throw new ApiException(ErrorCode.SESSION_REVOKED);
        }

        UserSession session = sessionOpt.get();
        String tokenHash = jwtService.hashToken(token);

        if (!tokenHash.equals(session.getRefreshTokenHash())) {
            // Refresh token reuse detected! Revoke session
            sessionRegistryService.revokeSession(sessionId);
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "Token reuse detected. Session revoked.");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            sessionRegistryService.revokeSession(sessionId);
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }

        // Rotate refresh token
        String newRefreshToken = jwtService.generateRefreshToken(userId, sessionId);
        String newRefreshTokenHash = jwtService.hashToken(newRefreshToken);
        sessionRegistryService.updateRefreshToken(sessionId, newRefreshTokenHash);

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getSupplierId(), sessionId);

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .supplierId(user.getSupplierId())
                .build();

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .user(userSummary)
                .sessionId(sessionId)
                .build();
    }

    public void logout(String sessionId) {
        if (sessionId != null) {
            sessionRegistryService.revokeSession(sessionId);
        }
    }

    public void logoutAll(UUID userId) {
        sessionRegistryService.revokeAllUserSessions(userId);
    }

    public List<SessionDto> getUserSessions(UUID userId, String currentSessionId) {
        return sessionRegistryService.getUserSessions(userId).stream()
                .map(s -> SessionDto.builder()
                        .sessionId(s.getSessionId())
                        .device(s.getDevice())
                        .createdAt(s.getCreatedAt())
                        .lastActivityAt(s.getLastActivityAt())
                        .expiresAt(s.getExpiresAt())
                        .isCurrent(s.getSessionId().equals(currentSessionId))
                        .build())
                .collect(Collectors.toList());
    }

    public void revokeUserSession(UUID userId, String sessionId) {
        Optional<UserSession> sessionOpt = sessionRegistryService.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
        }
        UserSession session = sessionOpt.get();
        if (!session.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        sessionRegistryService.revokeSession(sessionId);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            // Generic 202 response to prevent user enumeration
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();
        int otp = 100000 + secureRandom.nextInt(900000);
        String otpStr = String.valueOf(otp);
        String otpHash = jwtService.hashToken(otpStr);

        Instant expiresAt = Instant.now().plusSeconds(300); // 5 minutes

        PasswordReset passwordReset = PasswordReset.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(otpHash)
                .expiresAt(expiresAt)
                .build();
        passwordResetRepository.save(passwordReset);

        // Send OTP email
        try {
            if (mailSender.isPresent()) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(user.getEmail());
                message.setSubject("VendorPulse Password Reset Code");
                message.setText("Your password reset code is: " + otpStr + ". This code expires in 5 minutes.");
                mailSender.get().send(message);
            } else {
                log.info("MAIL SENDER NOT CONFIGURED. DEMO OTP CODE FOR {}: {}", email, otpStr);
            }
        } catch (Exception e) {
            log.warn("Failed to send OTP email to {}: {}. Demo OTP: {}", email, e.getMessage(), otpStr);
        }
    }

    @Transactional
    public VerifyOtpResponse verifyResetOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        rateLimitingService.checkOtpRateLimit(email);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(ErrorCode.OTP_INVALID));

        PasswordReset resetRecord = passwordResetRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.OTP_INVALID));

        if (resetRecord.getUsedAt() != null) {
            throw new ApiException(ErrorCode.OTP_ALREADY_USED);
        }

        if (resetRecord.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.OTP_EXPIRED);
        }

        String inputHash = jwtService.hashToken(request.getOtp().trim());
        if (!inputHash.equals(resetRecord.getTokenHash())) {
            rateLimitingService.recordFailedOtp(email);
            throw new ApiException(ErrorCode.OTP_INVALID);
        }

        resetRecord.setUsedAt(Instant.now());
        passwordResetRepository.save(resetRecord);

        String resetToken = jwtService.generateResetAuthorizationToken(user.getId(), user.getEmail());
        return VerifyOtpResponse.builder().resetToken(resetToken).build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Claims claims = jwtService.extractClaims(request.getResetToken());
        if (claims == null || !"RESET_AUTH".equals(claims.get("type", String.class))) {
            throw new ApiException(ErrorCode.TOKEN_INVALID_OR_EXPIRED);
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Password must be at least 6 characters");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing sessions for this user
        sessionRegistryService.revokeAllUserSessions(user.getId());
    }
}

