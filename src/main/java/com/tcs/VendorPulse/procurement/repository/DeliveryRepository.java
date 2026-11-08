package com.tcs.VendorPulse.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tcs.VendorPulse.procurement.entity.Delivery;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByPurchaseOrderId(Long poId);
    @Query("SELECT COALESCE(SUM(d.deliveredQty),0) FROM Delivery d WHERE d.purchaseOrder.id=:poId")
    int totalDelivered(@Param("poId") Long poId);
}