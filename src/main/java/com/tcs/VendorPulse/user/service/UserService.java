package com.tcs.VendorPulse.user.service;

import com.tcs.VendorPulse.audit.service.AuditService;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.enums.AccountStatus;
import com.tcs.VendorPulse.common.enums.UserRole;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.user.domain.User;
import com.tcs.VendorPulse.user.dto.UserDtos.*;
import com.tcs.VendorPulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> searchUsers(UserRole role, AccountStatus status, String q, Pageable pageable) {
        Page<User> page = userRepository.searchUsers(role, status, q, pageable);
        return PagedResponse.from(page.map(this::toDto));
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_EXISTS);
        }

        if ((request.getRole() == UserRole.SUPPLIER_MANAGER || request.getRole() == UserRole.SUPPLIER_USER)
                && request.getSupplierId() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Supplier ID is required for supplier roles");
        }

        String tempPassword = "Temp@" + UUID.randomUUID().toString().substring(0, 8);

        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName(request.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(request.getRole())
                .status(request.getStatus() != null ? request.getStatus() : AccountStatus.ACTIVE)
                .supplierId(request.getSupplierId())
                .build();

        user = userRepository.save(user);

        CurrentUser current = CurrentUser.get();
        auditService.log(
                current != null ? current.getId() : null,
                current != null ? current.getFullName() : "System",
                "CREATE_USER",
                "USER",
                user.getId().toString(),
                user.getEmail()
        );

        return CreateUserResponse.builder()
                .user(toDto(user))
                .temporaryPassword(tempPassword)
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));
        return toDto(user);
    }

    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));

        if ((request.getRole() == UserRole.SUPPLIER_MANAGER || request.getRole() == UserRole.SUPPLIER_USER)
                && request.getSupplierId() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Supplier ID is required for supplier roles");
        }

        user.setFullName(request.getFullName().trim());
        user.setRole(request.getRole());
        user.setSupplierId(request.getSupplierId());
        user.setStatus(request.getStatus());

        user = userRepository.save(user);

        CurrentUser current = CurrentUser.get();
        auditService.log(
                current != null ? current.getId() : null,
                current != null ? current.getFullName() : "System",
                "UPDATE_USER",
                "USER",
                user.getId().toString(),
                user.getEmail()
        );

        return toDto(user);
    }

    @Transactional
    public UserDto updateUserStatus(UUID id, AccountStatus status) {
        CurrentUser current = CurrentUser.get();
        if (current != null && current.getId().equals(id) && status == AccountStatus.INACTIVE) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Cannot deactivate yourself");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));

        user.setStatus(status);
        user = userRepository.save(user);

        auditService.log(
                current != null ? current.getId() : null,
                current != null ? current.getFullName() : "System",
                status == AccountStatus.ACTIVE ? "ACTIVATE_USER" : "DEACTIVATE_USER",
                "USER",
                user.getId().toString(),
                user.getEmail()
        );

        return toDto(user);
    }

    public List<String> getRoles() {
        return Arrays.stream(UserRole.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    private UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .userCode(u.getUserCode())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .status(u.getStatus())
                .supplierId(u.getSupplierId())
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}

