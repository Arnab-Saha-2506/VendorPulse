package com.tcs.VendorPulse.auth.security;

import com.tcs.VendorPulse.auth.jwt.JwtService;
import com.tcs.VendorPulse.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SessionRegistryService sessionRegistryService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        Claims claims = jwtService.extractClaims(jwt);

        if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String sid = claims.get("sid", String.class);

            // Validate that the in-memory session is active and not revoked
            if (sid != null && sessionRegistryService.validateSession(sid)) {
                UUID userId = UUID.fromString(claims.getSubject());
                String roleStr = claims.get("role", String.class);
                UserRole role = UserRole.valueOf(roleStr);
                String email = claims.get("email", String.class);
                String fullName = claims.get("fullName", String.class);
                String supplierIdStr = claims.get("supplierId", String.class);
                UUID supplierId = supplierIdStr != null ? UUID.fromString(supplierIdStr) : null;

                CurrentUser currentUser = CurrentUser.builder()
                        .id(userId)
                        .email(email)
                        .fullName(fullName)
                        .role(role)
                        .supplierId(supplierId)
                        .sessionId(sid)
                        .build();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        currentUser,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}

