package com.tcs.VendorPulse.supplier.repository;

import com.tcs.VendorPulse.supplier.entity.SupplierDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SupplierDocumentRepository extends JpaRepository<SupplierDocument, Long> {
    Optional<SupplierDocument> findTopBySupplierIdAndDocTypeOrderByUploadedAtDesc(Long supplierId, String docType);

    List<SupplierDocument> findBySupplierId(Long supplierId);
}
