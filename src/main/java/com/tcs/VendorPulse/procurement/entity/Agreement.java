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
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private String currency = "INR";
    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status = AgreementStatus.DRAFT;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime acceptedBySupplierAt;
    private LocalDateTime acceptedByAdminAt;

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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal p) {
        this.price = p;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String c) {
        this.currency = c;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate d) {
        this.startDate = d;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate d) {
        this.endDate = d;
    }

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
}