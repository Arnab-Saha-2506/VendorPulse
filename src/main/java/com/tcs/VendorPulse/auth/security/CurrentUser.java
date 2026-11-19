package com.tcs.VendorPulse.auth.security;

import com.tcs.VendorPulse.common.enums.PartySide;
import com.tcs.VendorPulse.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser {
    private UUID id;
    private String email;
    private String fullName;
    private UserRole role;
    private UUID supplierId;
    private String sessionId;

    public PartySide getPartySide() {
        if (role == UserRole.SUPPLIER_MANAGER || role == UserRole.SUPPLIER_USER) {
            return PartySide.SUPPLIER;
        }
        return PartySide.MANUFACTURER;
    }

    public boolean isProcurement() {
        return role == UserRole.PROCUREMENT_MANAGER || role == UserRole.PROCUREMENT_EXECUTIVE || role == UserRole.SYSTEM_ADMIN;
    }

    public boolean isSupplier() {
        return role == UserRole.SUPPLIER_MANAGER || role == UserRole.SUPPLIER_USER;
    }

    public static CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser currentUser) {
            return currentUser;
        }
        return null;
    }
}

