package com.tcs.VendorPulse.scoring.entity;

import com.tcs.VendorPulse.common.enums.RatingStatus;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_score_history")
public class SupplierScoreHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    private Double deliveryReliability, commitmentStability, deliveryConsistency, qualityPerformance, overallScore, cdi;
    @Enumerated(EnumType.STRING)
    private RatingStatus ratingStatus;
    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    void onCreate() {
        calculatedAt = LocalDateTime.now();
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

    public Double getDeliveryReliability() {
        return deliveryReliability;
    }

    public void setDeliveryReliability(Double v) {
        this.deliveryReliability = v;
    }

    public Double getCommitmentStability() {
        return commitmentStability;
    }

    public void setCommitmentStability(Double v) {
        this.commitmentStability = v;
    }

    public Double getDeliveryConsistency() {
        return deliveryConsistency;
    }

    public void setDeliveryConsistency(Double v) {
        this.deliveryConsistency = v;
    }

    public Double getQualityPerformance() {
        return qualityPerformance;
    }

    public void setQualityPerformance(Double v) {
        this.qualityPerformance = v;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double v) {
        this.overallScore = v;
    }

    public Double getCdi() {
        return cdi;
    }

    public void setCdi(Double v) {
        this.cdi = v;
    }

    public RatingStatus getRatingStatus() {
        return ratingStatus;
    }

    public void setRatingStatus(RatingStatus r) {
        this.ratingStatus = r;
    }
}