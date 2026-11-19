package com.tcs.VendorPulse.supplier.repository;

import com.tcs.VendorPulse.common.enums.ApprovalStatus;
import com.tcs.VendorPulse.supplier.domain.SupplierDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierDocumentRepository extends JpaRepository<SupplierDocument, UUID> {
    List<SupplierDocument> findBySupplierId(UUID supplierId);
    List<SupplierDocument> findBySupplierIdAndReviewStatus(UUID supplierId, ApprovalStatus reviewStatus);
}

