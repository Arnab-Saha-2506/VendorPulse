package com.tcs.VendorPulse.agreement.repository;

import com.tcs.VendorPulse.agreement.domain.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, UUID> {
    Optional<Agreement> findByPoId(UUID poId);
    boolean existsByPoId(UUID poId);
}

