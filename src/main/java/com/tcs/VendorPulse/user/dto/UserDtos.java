package com.tcs.VendorPulse.user.dto;

import com.tcs.VendorPulse.common.enums.AccountStatus;
import com.tcs.VendorPulse.common.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class UserDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private UUID id;
        private String userCode;
        private String fullName;
        private String email;
        private UserRole role;
        private AccountStatus status;
        private UUID supplierId;
        private Instant lastLoginAt;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotNull(message = "Role is required")
        private UserRole role;

        private UUID supplierId;
        private AccountStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserResponse {
        private UserDto user;
        private String temporaryPassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotNull(message = "Role is required")
        private UserRole role;

        private UUID supplierId;

        @NotNull(message = "Status is required")
        private AccountStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatusRequest {
        @NotNull(message = "Status is required")
        private AccountStatus status;
    }
}

