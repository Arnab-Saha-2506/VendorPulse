package com.tcs.VendorPulse.po.repository;

import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID>, JpaSpecificationExecutor<PurchaseOrder> {
    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    List<PurchaseOrder> findBySupplierId(UUID supplierId);

    long countByStatus(PoStatus status);

    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.status IN ('SUBMITTED', 'CONFIRMED', 'IN_PROGRESS', 'PARTIALLY_DELIVERED')")
    long countOpenPos();

    @Query("SELECT po FROM PurchaseOrder po WHERE " +
           "(:supplierId IS NULL OR po.supplierId = :supplierId) AND " +
           "(:status IS NULL OR po.status = :status) AND " +
           "(:from IS NULL OR po.requiredDate >= :from) AND " +
           "(:to IS NULL OR po.requiredDate <= :to) AND " +
           "(:q IS NULL OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<PurchaseOrder> searchPos(
            @Param("supplierId") UUID supplierId,
            @Param("status") PoStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("q") String q,
            Pageable pageable);

    @Query(value = "SELECT nextval('po_number_seq')", nativeQuery = true)
    Long getNextPoSequence();
}

