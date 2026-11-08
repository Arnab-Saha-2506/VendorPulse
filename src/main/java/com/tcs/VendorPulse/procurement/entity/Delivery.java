package com.tcs.VendorPulse.procurement.entity;

import com.tcs.VendorPulse.common.enums.DeliveryStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "po_id")
    private PurchaseOrder purchaseOrder;
    @Column(nullable = false)
    private LocalDate actualDeliveryDate;
    @Column(nullable = false)
    private int deliveredQty;
    @Column(nullable = false)
    private int acceptedQty;
    @Column(nullable = false)
    private int rejectedQty;
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.PARTIALLY_DELIVERED;
    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    void onCreate() {
        recordedAt = LocalDateTime.now();
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

    public LocalDate getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(LocalDate d) {
        this.actualDeliveryDate = d;
    }

    public int getDeliveredQty() {
        return deliveredQty;
    }

    public void setDeliveredQty(int q) {
        this.deliveredQty = q;
    }

    public int getAcceptedQty() {
        return acceptedQty;
    }

    public void setAcceptedQty(int q) {
        this.acceptedQty = q;
    }

    public int getRejectedQty() {
        return rejectedQty;
    }

    public void setRejectedQty(int q) {
        this.rejectedQty = q;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String r) {
        this.remarks = r;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus s) {
        this.status = s;
    }
}