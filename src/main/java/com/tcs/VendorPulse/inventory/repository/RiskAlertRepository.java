package com.tcs.VendorPulse.inventory.repository;

import com.tcs.VendorPulse.inventory.entity.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
    Optional<RiskAlert> findByMaterialCodeAndActiveTrue(String code);
}
