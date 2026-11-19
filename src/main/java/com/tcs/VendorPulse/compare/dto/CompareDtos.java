package com.tcs.VendorPulse.compare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CompareDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierRankItem {
        private int rank;
        private UUID supplierId;
        private String supplierName;
        private BigDecimal overallScore;
        private BigDecimal deliveryReliability;
        private BigDecimal commitmentStability;
        private BigDecimal deliveryConsistency;
        private BigDecimal qualityPerformance;
        private BigDecimal cdi;
        private boolean isRated;
        private String ratingLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedSupplierDto {
        private UUID supplierId;
        private String supplierName;
        private BigDecimal overallScore;
        private BigDecimal cdi;
        private List<String> reasons;
        private boolean insufficientHistory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompareResponse {
        private Short categoryId;
        private String categoryName;
        private List<SupplierRankItem> ranking;
        private RecommendedSupplierDto recommended;
    }
}

