package com.tcs.VendorPulse.scoring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "supplier_score_history", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"supplier_id", "period"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierScoreHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "period", nullable = false)
    private LocalDate period;

    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "delivery_reliability", nullable = false, precision = 5, scale = 2)
    private BigDecimal deliveryReliability;

    @Column(name = "commitment_stability", nullable = false, precision = 5, scale = 2)
    private BigDecimal commitmentStability;

    @Column(name = "delivery_consistency", nullable = false, precision = 5, scale = 2)
    private BigDecimal deliveryConsistency;

    @Column(name = "quality_performance", nullable = false, precision = 5, scale = 2)
    private BigDecimal qualityPerformance;

    @Column(name = "cdi", nullable = false, precision = 5, scale = 2)
    private BigDecimal cdi;

    @Column(name = "orders_counted", nullable = false)
    private Integer ordersCounted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.ordersCounted == null) {
            this.ordersCounted = 0;
        }
    }
}

