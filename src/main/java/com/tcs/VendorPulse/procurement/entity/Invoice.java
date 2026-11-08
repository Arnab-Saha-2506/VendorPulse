package com.tcs.VendorPulse.procurement.entity;

import com.tcs.VendorPulse.common.enums.InvoiceStatus;
import com.tcs.VendorPulse.common.enums.InvoiceType;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "po_id")
    private PurchaseOrder purchaseOrder;
    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    @Column
    private String invoiceNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceType invoiceType = InvoiceType.PROFORMA;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime finalizedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder p) {
        this.purchaseOrder = p;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier s) {
        this.supplier = s;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String n) {
        this.invoiceNumber = n;
    }

    public InvoiceType getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(InvoiceType t) {
        this.invoiceType = t;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal s) {
        this.subtotal = s;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal t) {
        this.taxAmount = t;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal t) {
        this.totalAmount = t;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus s) {
        this.status = s;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime d) {
        this.confirmedAt = d;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime d) {
        this.finalizedAt = d;
    }
}