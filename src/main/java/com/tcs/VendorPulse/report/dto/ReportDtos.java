package com.tcs.VendorPulse.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ReportDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierPerformanceReportRow {
        private UUID supplierId;
        private String supplierName;
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
    public static class CommitmentHistoryReportRow {
        private String poNumber;
        private String supplierName;
        private LocalDate originalDate;
        private LocalDate revisedDate;
        private String reason;
        private String revisedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierComparisonReportRow {
        private int rank;
        private String supplierName;
        private BigDecimal overallScore;
        private BigDecimal cdi;
        private BigDecimal deliveryConsistency;
        private BigDecimal qualityPerformance;
    }
}

