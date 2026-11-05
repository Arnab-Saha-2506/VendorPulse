package com.tcs.VendorPulse.procurement.entity;

import com.tcs.VendorPulse.common.enums.CommitmentStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "commitments")
public class Commitment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "po_id")
    private PurchaseOrder purchaseOrder;
    @Column(nullable = false)
    private LocalDate committedDeliveryDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommitmentStatus status = CommitmentStatus.ON_TRACK;
    @Column(nullable = false)
    private LocalDateTime createdAt;

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

    public LocalDate getCommittedDeliveryDate() {
        return committedDeliveryDate;
    }

    public void setCommittedDeliveryDate(LocalDate d) {
        this.committedDeliveryDate = d;
    }

    public CommitmentStatus getStatus() {
        return status;
    }

    public void setStatus(CommitmentStatus s) {
        this.status = s;
    }
}