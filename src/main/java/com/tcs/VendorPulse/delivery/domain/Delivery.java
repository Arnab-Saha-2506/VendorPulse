package com.tcs.VendorPulse.delivery.domain;

import com.tcs.VendorPulse.common.enums.DeliveryStatus;
import com.tcs.VendorPulse.common.enums.InspectionResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "po_id", nullable = false)
    private UUID poId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "delivered_qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal deliveredQty;

    @Column(name = "accepted_qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal acceptedQty;

    @Column(name = "rejected_qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal rejectedQty;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "completion", nullable = false)
    private DeliveryStatus completion;

    @Column(name = "quality_rating")
    private Short qualityRating;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "inspection")
    private InspectionResult inspection;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "recorded_by", nullable = false)
    private UUID recordedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}

