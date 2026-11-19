package com.tcs.VendorPulse.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scoring_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringSettings {

    @Id
    @Column(name = "id", nullable = false)
    private Short id;

    @Column(name = "w_delivery_reliability", nullable = false)
    private Short wDeliveryReliability;

    @Column(name = "w_commitment_stability", nullable = false)
    private Short wCommitmentStability;

    @Column(name = "w_delivery_consistency", nullable = false)
    private Short wDeliveryConsistency;

    @Column(name = "w_quality_performance", nullable = false)
    private Short wQualityPerformance;

    @Column(name = "high_cdi_threshold", nullable = false)
    private Short highCdiThreshold;

    @Column(name = "stock_alert_horizon_days", nullable = false)
    private Short stockAlertHorizonDays;

    @Column(name = "max_negotiation_rounds", nullable = false)
    private Short maxNegotiationRounds;

    @Column(name = "default_cutoff_days", nullable = false)
    private Short defaultCutoffDays;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

