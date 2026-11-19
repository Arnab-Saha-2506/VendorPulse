package com.tcs.VendorPulse.delivery.repository;

import com.tcs.VendorPulse.delivery.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    List<Delivery> findByPoIdOrderByDeliveryDateAscCreatedAtAsc(UUID poId);

    @Query("SELECT COALESCE(SUM(d.deliveredQty), 0) FROM Delivery d WHERE d.poId = :poId")
    BigDecimal sumDeliveredQtyByPoId(@Param("poId") UUID poId);

    @Query("SELECT d FROM Delivery d JOIN PurchaseOrder po ON d.poId = po.id WHERE po.supplierId = :supplierId")
    List<Delivery> findAllBySupplierId(@Param("supplierId") UUID supplierId);
}

