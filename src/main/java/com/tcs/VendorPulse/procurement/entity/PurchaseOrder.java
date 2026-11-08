package com.tcs.VendorPulse.procurement.entity;

import com.tcs.VendorPulse.common.enums.PoStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "agreement_id", unique = true)
    private Agreement agreement;
    @Column(unique = true, nullable = false)
    private String poNumber;
    @Column(nullable = false)
    private LocalDate issueDate;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PoStatus status = PoStatus.CONFIRMED;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();
    public List<PurchaseOrderItem> getItems() { return items; }
    public void setItems(List<PurchaseOrderItem> items) { this.items = items; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Agreement getAgreement() {
        return agreement;
    }

    public void setAgreement(Agreement a) {
        this.agreement = a;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String n) {
        this.poNumber = n;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate d) {
        this.issueDate = d;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal t) {
        this.totalAmount = t;
    }

    public PoStatus getStatus() {
        return status;
    }

    public void setStatus(PoStatus s) {
        this.status = s;
    }
}