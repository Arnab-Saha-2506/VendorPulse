package com.tcs.VendorPulse.supplier.repository;

import com.tcs.VendorPulse.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcs.VendorPulse.common.enums.SupplierStatus;
import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    boolean existsByGstin(String gstin);

    boolean existsByEmail(String email);

    List<Supplier> findByStatus(SupplierStatus status);
}