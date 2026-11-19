package com.tcs.VendorPulse.delivery.dto;

import com.tcs.VendorPulse.common.enums.DeliveryStatus;
import com.tcs.VendorPulse.common.enums.InspectionResult;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class DeliveryDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryDto {
        private UUID id;
        private UUID poId;
        private LocalDate deliveryDate;
        private BigDecimal deliveredQty;
        private BigDecimal acceptedQty;
        private BigDecimal rejectedQty;
        private DeliveryStatus completion;
        private Short qualityRating;
        private InspectionResult inspection;
        private String remarks;
        private UUID recordedBy;
        private String recordedByName;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryOverviewDto {
        private UUID poId;
        private String poNumber;
        private BigDecimal orderedQty;
        private BigDecimal deliveredQty;
        private BigDecimal remainingQty;
        private List<DeliveryDto> deliveries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordDeliveryRequest {
        @NotNull(message = "Delivery date is required")
        private LocalDate deliveryDate;

        @NotNull(message = "Delivered quantity is required")
        @DecimalMin(value = "0.01", message = "Delivered quantity must be positive")
        private BigDecimal deliveredQty;

        @NotNull(message = "Completion status is required (PARTIAL or COMPLETE)")
        private DeliveryStatus completion;

        @NotNull(message = "Accepted quantity is required")
        @DecimalMin(value = "0.0", message = "Accepted quantity cannot be negative")
        private BigDecimal acceptedQty;

        @NotNull(message = "Rejected quantity is required")
        @DecimalMin(value = "0.0", message = "Rejected quantity cannot be negative")
        private BigDecimal rejectedQty;

        @Min(value = 1, message = "Quality rating must be between 1 and 5")
        @Max(value = 5, message = "Quality rating must be between 1 and 5")
        private Short qualityRating;

        private InspectionResult inspection;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordDeliveryResponse {
        private DeliveryDto delivery;
        private BigDecimal defectPercent;
    }
}

