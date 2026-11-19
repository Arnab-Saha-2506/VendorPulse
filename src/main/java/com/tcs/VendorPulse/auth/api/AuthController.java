package com.tcs.VendorPulse.auth.api;

import com.tcs.VendorPulse.auth.dto.*;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and session management APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Login for PROCUREMENT_MANAGER, SUPPLIER_MANAGER and SYSTEM_ADMIN")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String device = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(request, device));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user profile", description = "Get profile of the currently authenticated user")
    public ResponseEntity<UserSummaryDto> getMe() {
        return ResponseEntity.ok(authService.getMe());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password request", description = "Request an OTP verification code sent to user email")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/verify-reset-otp")
    @Operation(summary = "Verify reset OTP", description = "Verify email OTP code and receive a short-lived reset authorization token")
    public ResponseEntity<VerifyOtpResponse> verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyResetOtp(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using reset authorization token")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Rotate refresh token and issue new access token")
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String device = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.refreshToken(request, device));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout session", description = "Logout current application session")
    public ResponseEntity<Void> logout() {
        CurrentUser current = CurrentUser.get();
        if (current != null) {
            authService.logout(current.getSessionId());
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all sessions", description = "Logout all active sessions for current user")
    public ResponseEntity<Void> logoutAll() {
        CurrentUser current = CurrentUser.get();
        if (current != null) {
            authService.logoutAll(current.getId());
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    @Operation(summary = "List active sessions", description = "List all active application sessions for current user")
    public ResponseEntity<List<SessionDto>> getSessions() {
        CurrentUser current = CurrentUser.get();
        return ResponseEntity.ok(authService.getUserSessions(current.getId(), current.getSessionId()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke session", description = "Revoke a specific session owned by current user")
    public ResponseEntity<Void> revokeSession(@PathVariable String sessionId) {
        CurrentUser current = CurrentUser.get();
        authService.revokeUserSession(current.getId(), sessionId);
        return ResponseEntity.noContent().build();
    }
}

