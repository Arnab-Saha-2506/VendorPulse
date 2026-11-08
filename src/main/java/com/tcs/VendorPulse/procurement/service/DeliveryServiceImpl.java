package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.procurement.entity.*;
import com.tcs.VendorPulse.procurement.repository.*;
import com.tcs.VendorPulse.common.enums.DeliveryStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class DeliveryServiceImpl implements DeliveryService {
    private final DeliveryRepository deliveries;
    private final PurchaseOrderRepository pos;

    public DeliveryServiceImpl(DeliveryRepository d, PurchaseOrderRepository p) {
        deliveries = d;
        pos = p;
    }

    @Transactional
    public Delivery record(Long poId, LocalDate actualDate, int delivered, int accepted, int rejected, String remarks,
            Long managerId) {
        if (accepted + rejected > delivered)
            throw new IllegalArgumentException("accepted+rejected > delivered");
        PurchaseOrder po = pos.findById(poId).orElseThrow();
        int totalDelivered = deliveries.totalDelivered(poId);
        if (totalDelivered + delivered > po.getTotalAmount().intValue()) // or PO quantity if items
            throw new IllegalArgumentException("Total delivered exceeds PO quantity");
        Delivery d = new Delivery();
        d.setPurchaseOrder(po);
        d.setActualDeliveryDate(actualDate);
        d.setDeliveredQty(delivered);
        d.setAcceptedQty(accepted);
        d.setRejectedQty(rejected);
        d.setRemarks(remarks);
        d.setStatus(totalDelivered + delivered >= /* po qty */ delivered ? DeliveryStatus.DELIVERED
                : DeliveryStatus.PARTIALLY_DELIVERED);
        return deliveries.save(d);
    }
}