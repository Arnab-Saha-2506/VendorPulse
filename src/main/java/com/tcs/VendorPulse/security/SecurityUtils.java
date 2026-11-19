package com.tcs.VendorPulse.security;

import com.tcs.VendorPulse.common.enums.Role;
import com.tcs.VendorPulse.user.entity.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static User getCurrentUser(Authentication auth, UserRepository userRepository) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser();
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found in database: " + auth.getName()));
    }

    public static Long getCurrentUserId(Authentication auth, UserRepository userRepository) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getId();
        }
        return getCurrentUser(auth, userRepository).getId();
    }

    public static boolean hasRole(Authentication auth, Role role) {
        if (auth == null) return false;
        String expectedRole = "ROLE_" + role.name();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(expectedRole));
    }
}

