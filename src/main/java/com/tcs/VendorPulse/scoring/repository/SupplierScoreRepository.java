package com.tcs.VendorPulse.scoring.repository;

import com.tcs.VendorPulse.scoring.entity.SupplierScore;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


public interface SupplierScoreRepository extends JpaRepository<SupplierScore, Long> {
    Optional<SupplierScore> findBySupplierId(Long id);
}
