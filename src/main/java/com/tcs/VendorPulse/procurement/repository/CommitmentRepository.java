package com.tcs.VendorPulse.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcs.VendorPulse.procurement.entity.Commitment;

public interface CommitmentRepository extends JpaRepository<Commitment, Long> {
    List<Commitment> findByPurchaseOrderId(Long poId);
}
