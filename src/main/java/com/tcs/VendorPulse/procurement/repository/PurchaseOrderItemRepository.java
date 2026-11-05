package com.tcs.VendorPulse.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcs.VendorPulse.procurement.entity.PurchaseOrderItem;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    List<PurchaseOrderItem> findByPurchaseOrderId(Long poId);
}
