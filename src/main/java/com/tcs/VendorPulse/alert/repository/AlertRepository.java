package com.tcs.VendorPulse.alert.repository;

import com.tcs.VendorPulse.alert.domain.Alert;
import com.tcs.VendorPulse.common.enums.AlertStatus;
import com.tcs.VendorPulse.common.enums.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    long countByStatus(AlertStatus status);

    @Query("SELECT a FROM Alert a WHERE " +
           "(:type IS NULL OR a.type = :type) AND " +
           "(:status IS NULL OR a.status = :status) " +
           "ORDER BY a.raisedAt DESC")
    Page<Alert> searchAlerts(@Param("type") AlertType type, @Param("status") AlertStatus status, Pageable pageable);

    List<Alert> findTop5ByStatusOrderByRaisedAtDesc(AlertStatus status);

    Optional<Alert> findFirstByTypeAndPoIdAndStatus(AlertType type, UUID poId, AlertStatus status);
    Optional<Alert> findFirstByTypeAndMaterialIdAndStatus(AlertType type, UUID materialId, AlertStatus status);
    Optional<Alert> findFirstByTypeAndSupplierIdAndStatus(AlertType type, UUID supplierId, AlertStatus status);

    List<Alert> findByPoIdAndStatus(UUID poId, AlertStatus status);
}

