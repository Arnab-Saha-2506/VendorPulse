package com.tcs.VendorPulse.procurement.service;
import com.tcs.VendorPulse.procurement.entity.*;
import com.tcs.VendorPulse.procurement.repository.*;
import com.tcs.VendorPulse.common.enums.PoStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.UUID;
@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {
    private final PurchaseOrderRepository pos;
    private final AgreementRepository agreements;
    public PurchaseOrderServiceImpl(PurchaseOrderRepository p, AgreementRepository a){ pos=p; agreements=a; }
    @Transactional
    public PurchaseOrder generateFromAgreement(Long agreementId) {
        Agreement ag = agreements.findById(agreementId).orElseThrow();
        if (ag.getStatus().name().equals("ACTIVE")) throw new IllegalStateException("Agreement must be ACTIVE");
        PurchaseOrder po = new PurchaseOrder();
        po.setAgreement(ag); po.setPoNumber("PO-"+UUID.randomUUID().toString().substring(0,8).toUpperCase());
        po.setIssueDate(LocalDate.now()); po.setTotalAmount(ag.getPrice()); po.setStatus(PoStatus.CONFIRMED);
        return pos.save(po);
    }
    public PurchaseOrder get(Long id){ return pos.findById(id).orElseThrow(); }
    @Transactional
    public PurchaseOrder updateStatus(Long id, PoStatus newStatus) {
        PurchaseOrder po = get(id);
        if (po.getStatus() == PoStatus.SHIPPED || po.getStatus() == PoStatus.DELIVERED) {
            if (newStatus == PoStatus.CANCELLED) throw new IllegalStateException("Cancellation blocked after shipment");
        }
        // enforce forward-only progression
        po.setStatus(newStatus);
        return pos.save(po);
    }
}