package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.procurement.entity.*;
import com.tcs.VendorPulse.procurement.repository.*;
import com.tcs.VendorPulse.common.enums.InvoiceStatus;
import com.tcs.VendorPulse.common.enums.InvoiceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvoiceServiceImpl implements InvoiceService {
    private final InvoiceRepository invoices;
    private final InvoiceItemRepository items;
    private final PurchaseOrderRepository pos;
    private final DeliveryRepository deliveries;

    public InvoiceServiceImpl(InvoiceRepository i, InvoiceItemRepository it, PurchaseOrderRepository p,
            DeliveryRepository d) {
        invoices = i;
        items = it;
        pos = p;
        deliveries = d;
    }

    @Transactional
    public Invoice generate(Long poId, Long managerId) {
        PurchaseOrder po = pos.findById(poId).orElseThrow();
        List<Delivery> ds = deliveries.findByPurchaseOrderId(poId);
        int acceptedTotal = ds.stream().mapToInt(Delivery::getAcceptedQty).sum();
        int alreadyInvoiced = invoices.findByPurchaseOrderId(poId).stream()
                .flatMap(inv -> items.findByInvoiceId(inv.getId()).stream())
                .mapToInt(InvoiceItem::getQuantity).sum();
        int toInvoice = acceptedTotal - alreadyInvoiced;
        if (toInvoice <= 0)
            throw new IllegalStateException("No accepted quantity left to invoice");
        int poTotalQty = po.getItems().stream().mapToInt(PurchaseOrderItem::getQuantity).sum();
        Invoice inv = new Invoice();
        inv.setPurchaseOrder(po);
        inv.setSupplier(po.getAgreement().getSupplier());
        inv.setInvoiceType(InvoiceType.PROFORMA);
        inv.setStatus(InvoiceStatus.DRAFT);
        BigDecimal subtotal = BigDecimal.ZERO, taxTotal = BigDecimal.ZERO;
        for (PurchaseOrderItem poi : po.getItems()) {
            int itemQty = (int) Math.round((double) poi.getQuantity() / poTotalQty * toInvoice);
            if (itemQty <= 0) continue;
            BigDecimal line = poi.getUnitPrice().multiply(BigDecimal.valueOf(itemQty));
            BigDecimal rate = poi.getTaxRate() != null ? poi.getTaxRate() : BigDecimal.ZERO;
            BigDecimal tax = line.multiply(rate).divide(BigDecimal.valueOf(100));
            InvoiceItem ii = new InvoiceItem();
            ii.setInvoice(inv); ii.setItemName(poi.getItemName()); ii.setQuantity(itemQty);
            ii.setUnitPrice(poi.getUnitPrice()); ii.setLineTotal(line);
            ii.setTaxRate(rate); ii.setTaxAmount(tax);
            items.save(ii);
            subtotal = subtotal.add(line); taxTotal = taxTotal.add(tax);
        }
        inv.setSubtotal(subtotal); inv.setTaxAmount(taxTotal);
        inv.setTotalAmount(subtotal.add(taxTotal));
        return invoices.save(inv);
    }

    public Invoice get(Long id) {
        return invoices.findById(id).orElseThrow();
    }

    @Transactional
    public Invoice confirm(Long id, Long supplierUserId) {
        Invoice inv = get(id);
        if (inv.getStatus() != InvoiceStatus.DRAFT)
            throw new IllegalStateException("Not a draft");
        inv.setStatus(InvoiceStatus.CONFIRMED);
        inv.setConfirmedAt(LocalDateTime.now());
        return invoices.save(inv);
    }

    @Transactional
    public Invoice finalizeInvoice(Long id, String invoiceNumber, Long supplierUserId) {
        Invoice inv = get(id);
        inv.setInvoiceNumber(invoiceNumber);
        inv.setInvoiceType(InvoiceType.TAX_INVOICE);
        inv.setStatus(InvoiceStatus.FINALIZED);
        inv.setFinalizedAt(LocalDateTime.now());
        return invoices.save(inv);
    }

    @Transactional
    public Invoice approve(Long id, Long managerId) {
        Invoice inv = get(id);
        if (inv.getStatus() != InvoiceStatus.CONFIRMED)
            throw new IllegalStateException("Only confirmed invoices can be approved");
        inv.setStatus(InvoiceStatus.FINALIZED);
        inv.setFinalizedAt(LocalDateTime.now());
        return invoices.save(inv);
    }

    @Transactional
    public Invoice reject(Long id, Long managerId, String reason) {
        Invoice inv = get(id);
        inv.setStatus(InvoiceStatus.DRAFT);
        inv.setRejectionReason(reason);
        return invoices.save(inv);
    }
}