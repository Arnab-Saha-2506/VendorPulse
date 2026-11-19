package com.tcs.VendorPulse.procurement.entity;

import com.tcs.VendorPulse.common.enums.AgreementStatus;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agreements")
public class Agreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String terms;
    @Column(nullable = false)
    private String material;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    @Column(nullable = false)
    private java.time.LocalDate deliveryDate;
    @Column(columnDefinition = "TEXT")
    private String paymentTerms;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status = AgreementStatus.DRAFT;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime acceptedBySupplierAt;
    private LocalDateTime acceptedByAdminAt;
    private Long createdByManagerId;
    private Long sentByManagerId;
    private Long finalizedByAdminId;

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

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier s) {
        this.supplier = s;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String t) {
        this.title = t;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String t) {
        this.terms = t;
    }

    public String getMaterial() { return material; }
    public void setMaterial(String m) { this.material = m; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal u) { this.unitPrice = u; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal t) { this.totalAmount = t; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate d) { this.deliveryDate = d; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String p) { this.paymentTerms = p; }

    public AgreementStatus getStatus() {
        return status;
    }

    public void setStatus(AgreementStatus s) {
        this.status = s;
    }

    public LocalDateTime getAcceptedBySupplierAt() {
        return acceptedBySupplierAt;
    }

    public void setAcceptedBySupplierAt(LocalDateTime d) {
        this.acceptedBySupplierAt = d;
    }

    public LocalDateTime getAcceptedByAdminAt() {
        return acceptedByAdminAt;
    }

    public void setAcceptedByAdminAt(LocalDateTime d) {
        this.acceptedByAdminAt = d;
    }

    public Long getCreatedByManagerId() {
        return createdByManagerId;
    }

    public void setCreatedByManagerId(Long createdByManagerId) {
        this.createdByManagerId = createdByManagerId;
    }

    public Long getSentByManagerId() {
        return sentByManagerId;
    }

    public void setSentByManagerId(Long sentByManagerId) {
        this.sentByManagerId = sentByManagerId;
    }

    public Long getFinalizedByAdminId() {
        return finalizedByAdminId;
    }

    public void setFinalizedByAdminId(Long finalizedByAdminId) {
        this.finalizedByAdminId = finalizedByAdminId;
    }
}