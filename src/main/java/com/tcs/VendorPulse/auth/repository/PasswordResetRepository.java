package com.tcs.VendorPulse.auth.repository;

import com.tcs.VendorPulse.auth.domain.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, UUID> {
    Optional<PasswordReset> findByTokenHash(String tokenHash);
    Optional<PasswordReset> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}

