package com.tcs.VendorPulse.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ErrorResponse errorResponse = ErrorResponse.builder()
                                    .timestamp(Instant.now())
                                    .status(HttpStatus.UNAUTHORIZED.value())
                                    .code(ErrorCode.UNAUTHORIZED.name())
                                    .message(ErrorCode.UNAUTHORIZED.getDefaultMessage())
                                    .path(request.getRequestURI())
                                    .build();
                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ErrorResponse errorResponse = ErrorResponse.builder()
                                    .timestamp(Instant.now())
                                    .status(HttpStatus.FORBIDDEN.value())
                                    .code(ErrorCode.FORBIDDEN.name())
                                    .message(ErrorCode.FORBIDDEN.getDefaultMessage())
                                    .path(request.getRequestURI())
                                    .build();
                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/verify-reset-otp",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/refresh",
                                "/api/v1/public/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/error"
                        ).permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/v1/users/**", "/api/v1/roles").hasRole("SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/audit/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/admin/jobs/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/settings/scoring").hasRole("SYSTEM_ADMIN")

                        // Procurement Manager endpoints
                        .requestMatchers("/api/v1/supplier-registrations/**").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/categories/**").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/materials/**").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/materials/**").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/materials/**").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/purchase-orders").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/purchase-orders/**").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/purchase-orders/**").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/purchase-orders/*/submit").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/purchase-orders/*/cancel").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/dashboard/procurement-manager").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/reports/**").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/suppliers/compare").hasAnyRole("PROCUREMENT_MANAGER", "PROCUREMENT_EXECUTIVE", "SYSTEM_ADMIN")

                        // Supplier Manager endpoints
                        .requestMatchers("/api/v1/proposals/**").hasAnyRole("SUPPLIER_MANAGER", "SUPPLIER_USER")
                        .requestMatchers("/api/v1/suppliers/me/**").hasAnyRole("SUPPLIER_MANAGER", "SUPPLIER_USER")
                        .requestMatchers("/api/v1/dashboard/supplier").hasAnyRole("SUPPLIER_MANAGER", "SUPPLIER_USER")
                        .requestMatchers("/api/v1/purchase-orders/*/commitments/revisions").hasAnyRole("SUPPLIER_MANAGER", "SUPPLIER_USER")
                        .requestMatchers("/api/v1/purchase-orders/*/status-updates").hasAnyRole("SUPPLIER_MANAGER", "SUPPLIER_USER")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "device"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

