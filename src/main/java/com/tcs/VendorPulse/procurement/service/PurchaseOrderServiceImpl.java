package com.tcs.VendorPulse.procurement.service;
import com.tcs.VendorPulse.procurement.entity.*;
import com.tcs.VendorPulse.procurement.repository.*;
import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.enums.AgreementStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {
    private final PurchaseOrderRepository pos;
    private final AgreementRepository agreements;
    private final PurchaseOrderItemRepository items;
    public PurchaseOrderServiceImpl(PurchaseOrderRepository p, AgreementRepository a, PurchaseOrderItemRepository i){ pos=p; agreements=a; items=i; }
    @Transactional
    public PurchaseOrder generateFromAgreement(Long agreementId) {
        Agreement ag = agreements.findById(agreementId).orElseThrow();
        if (ag.getStatus() != AgreementStatus.ACTIVE) {
            throw new IllegalStateException("Only finalized agreements can generate Purchase Orders");
        }
        PurchaseOrder po = new PurchaseOrder();
        po.setAgreement(ag); po.setPoNumber("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        po.setIssueDate(LocalDate.now()); po.setTotalAmount(ag.getTotalAmount()); po.setStatus(PoStatus.CONFIRMED);
        PurchaseOrder saved = pos.save(po);
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(saved); item.setItemName(ag.getMaterial());
        item.setQuantity(ag.getQuantity()); item.setUnitPrice(ag.getUnitPrice());
        item.setTotalPrice(ag.getTotalAmount()); item.setTaxRate(BigDecimal.ZERO);
        items.save(item);
        return saved;
    }
    public PurchaseOrder get(Long id){ return pos.findById(id).orElseThrow(); }
    @Transactional
    public PurchaseOrder updateStatus(Long id, PoStatus newStatus) {
        PurchaseOrder po = get(id);
        PoStatus current = po.getStatus();
        if (current == PoStatus.CANCELLED || current == PoStatus.DELIVERED) {
            throw new IllegalStateException("Cannot change status of " + current);
        }
        if (newStatus == PoStatus.CANCELLED && (current == PoStatus.SHIPPED || current == PoStatus.DELIVERED)) {
            throw new IllegalStateException("Cancellation blocked after shipment");
        }
        boolean allowed = (current == PoStatus.CONFIRMED && (newStatus == PoStatus.PROCESSING || newStatus == PoStatus.CANCELLED))
            || (current == PoStatus.PROCESSING && (newStatus == PoStatus.SHIPPED || newStatus == PoStatus.CANCELLED))
            || (current == PoStatus.SHIPPED && newStatus == PoStatus.DELIVERED);
        if (!allowed) throw new IllegalStateException("Invalid transition " + current + " -> " + newStatus);
        po.setStatus(newStatus);
        return pos.save(po);
    }
}