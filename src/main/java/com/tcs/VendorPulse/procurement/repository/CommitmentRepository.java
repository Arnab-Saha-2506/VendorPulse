package com.tcs.VendorPulse.procurement.repository;

import com.tcs.VendorPulse.procurement.entity.Commitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommitmentRepository extends JpaRepository<Commitment, Long> {
    List<Commitment> findByPurchaseOrderId(Long poId);
    @Query("SELECT c FROM Commitment c JOIN c.purchaseOrder po JOIN po.agreement a WHERE a.supplier.id = :supplierId")
    List<Commitment> findBySupplierId(@Param("supplierId") Long supplierId);
}
