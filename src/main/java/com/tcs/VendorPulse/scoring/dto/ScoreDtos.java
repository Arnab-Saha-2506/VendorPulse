package com.tcs.VendorPulse.scoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ScoreDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyScoreDto {
        private LocalDate period;
        private BigDecimal overallScore;
        private BigDecimal deliveryReliability;
        private BigDecimal commitmentStability;
        private BigDecimal deliveryConsistency;
        private BigDecimal qualityPerformance;
        private BigDecimal cdi;
        private int ordersCounted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScorecardDto {
        private UUID supplierId;
        private String supplierName;
        private BigDecimal overallScore;
        private BigDecimal deliveryReliability;
        private BigDecimal commitmentStability;
        private BigDecimal deliveryConsistency;
        private BigDecimal qualityPerformance;
        private BigDecimal cdi;
        private int ordersCounted;
        private boolean isRated;
        private String ratingLabel;
        private List<MonthlyScoreDto> history;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryStatDto {
        private int totalDeliveries;
        private BigDecimal totalDeliveredQty;
        private BigDecimal totalAcceptedQty;
        private BigDecimal totalRejectedQty;
        private double averageRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierPerformanceHistoryDto {
        private UUID supplierId;
        private DeliveryStatDto deliveryStats;
        private List<MonthlyScoreDto> monthlyTrend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecalculateResponse {
        private int count;
        private LocalDate snapshotPeriod;
    }
}

