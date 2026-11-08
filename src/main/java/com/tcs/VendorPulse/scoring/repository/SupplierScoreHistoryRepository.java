package com.tcs.VendorPulse.scoring.repository;

import com.tcs.VendorPulse.scoring.entity.SupplierScoreHistory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierScoreHistoryRepository extends JpaRepository<SupplierScoreHistory, Long> {
    List<SupplierScoreHistory> findBySupplierIdOrderByCalculatedAtDesc(Long id);
}
