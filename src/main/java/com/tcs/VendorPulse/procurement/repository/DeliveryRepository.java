package com.tcs.VendorPulse.procurement.repository;

import com.tcs.VendorPulse.procurement.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByPurchaseOrderId(Long poId);
    @Query("SELECT COALESCE(SUM(d.deliveredQty),0) FROM Delivery d WHERE d.purchaseOrder.id=:poId")
    int totalDelivered(@Param("poId") Long poId);
    @Query("SELECT d FROM Delivery d JOIN d.purchaseOrder po JOIN po.agreement a WHERE a.supplier.id = :supplierId")
    List<Delivery> findBySupplierId(@Param("supplierId") Long supplierId);
}
