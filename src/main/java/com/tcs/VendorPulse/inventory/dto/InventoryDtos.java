package com.tcs.VendorPulse.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class InventoryDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventorySnapshotItem {
        @NotBlank(message = "Material code is required")
        private String materialCode;

        @NotNull(message = "Stock quantity is required")
        @DecimalMin(value = "0.0", message = "Stock quantity cannot be negative")
        private BigDecimal stockQty;

        @NotNull(message = "Daily usage is required")
        @DecimalMin(value = "0.0", message = "Daily usage cannot be negative")
        private BigDecimal dailyUsage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventorySnapshotRequest {
        @NotEmpty(message = "Snapshot items cannot be empty")
        private List<InventorySnapshotItem> items;
        private Instant asOf;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventorySnapshotResponse {
        private int acceptedCount;
        private Instant asOf;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryStatusDto {
        private Instant asOf;
        private String lastResult; // OK or FAILED
        private String message;
    }
}

