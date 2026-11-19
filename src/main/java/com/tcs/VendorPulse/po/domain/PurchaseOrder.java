package com.tcs.VendorPulse.po.domain;

import com.tcs.VendorPulse.common.enums.PartySide;
import com.tcs.VendorPulse.common.enums.PoStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "po_number", length = 20, nullable = false, unique = true)
    private String poNumber;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private PoStatus status;

    @Column(name = "required_date", nullable = false)
    private LocalDate requiredDate;

    @Column(name = "agreed_price_total", precision = 14, scale = 2)
    private BigDecimal agreedPriceTotal;

    @Column(name = "agreed_delivery_date")
    private LocalDate agreedDeliveryDate;

    @Column(name = "cancellation_cutoff_days", nullable = false)
    private Short cancellationCutoffDays;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "awaiting_party")
    private PartySide awaitingParty;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "dispatched_on")
    private LocalDate dispatchedOn;

    @Column(name = "supplier_marked_delivered_on")
    private LocalDate supplierMarkedDeliveredOn;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = PoStatus.DRAFT;
        }
        if (this.cancellationCutoffDays == null) {
            this.cancellationCutoffDays = 7;
        }
    }
}

