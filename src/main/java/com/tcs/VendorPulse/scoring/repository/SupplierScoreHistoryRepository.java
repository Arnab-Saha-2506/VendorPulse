package com.tcs.VendorPulse.scoring.repository;

import com.tcs.VendorPulse.scoring.domain.SupplierScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierScoreHistoryRepository extends JpaRepository<SupplierScoreHistory, UUID> {
    Optional<SupplierScoreHistory> findBySupplierIdAndPeriod(UUID supplierId, LocalDate period);
    Optional<SupplierScoreHistory> findFirstBySupplierIdOrderByPeriodDesc(UUID supplierId);
    List<SupplierScoreHistory> findBySupplierIdOrderByPeriodAsc(UUID supplierId);
    List<SupplierScoreHistory> findByPeriod(LocalDate period);
}

