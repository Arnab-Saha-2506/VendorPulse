package com.tcs.VendorPulse.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcs.VendorPulse.procurement.entity.Agreement;

public interface AgreementRepository extends JpaRepository<Agreement, Long> {
    List<Agreement> findBySupplierId(Long supplierId);
}
