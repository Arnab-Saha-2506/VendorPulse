package com.tcs.VendorPulse.po.dto;

import com.tcs.VendorPulse.common.enums.PartySide;
import com.tcs.VendorPulse.common.enums.PoStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
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

public class PoDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoLineRequest {
        @NotNull(message = "Material ID is required")
        private UUID materialId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
        private BigDecimal quantity;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", message = "Unit price must be zero or positive")
        private BigDecimal unitPrice;

        @NotNull(message = "Required date is required")
        private LocalDate requiredDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePoRequest {
        @NotNull(message = "Supplier ID is required")
        private UUID supplierId;

        @NotNull(message = "Required date is required")
        private LocalDate requiredDate;

        private Short cutoffDays;
        private String notes;

        @NotEmpty(message = "Lines cannot be empty")
        @Valid
        private List<PoLineRequest> lines;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoLineDto {
        private UUID id;
        private Short lineNo;
        private UUID materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private LocalDate requiredDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoSummaryDto {
        private UUID id;
        private String poNumber;
        private UUID supplierId;
        private String supplierName;
        private PoStatus status;
        private LocalDate requiredDate;
        private LocalDate agreedDeliveryDate;
        private LocalDate latestCommittedDate;
        private BigDecimal agreedPriceTotal;
        private PartySide awaitingParty;
        private long revisions;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoDetailDto {
        private UUID id;
        private String poNumber;
        private UUID supplierId;
        private String supplierName;
        private String supplierContact;
        private String supplierEmail;
        private String supplierPhone;
        private PoStatus status;
        private LocalDate requiredDate;
        private BigDecimal agreedPriceTotal;
        private LocalDate agreedDeliveryDate;
        private Short cancellationCutoffDays;
        private PartySide awaitingParty;
        private String notes;
        private LocalDate dispatchedOn;
        private LocalDate supplierMarkedDeliveredOn;
        private String cancelReason;
        private UUID cancelledBy;
        private Instant cancelledAt;
        private UUID createdBy;
        private Instant createdAt;
        private Instant updatedAt;

        private List<PoLineDto> lines;

        // Action flags
        private boolean canCancel;
        private boolean canRevise;
        private boolean canRecordDelivery;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancellationWindowDto {
        private LocalDate cutoffDate;
        private boolean allowed;
        private String blockedReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelPoRequest {
        @NotNull(message = "Reason is required")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EligibleSupplierDto {
        private UUID id;
        private String companyName;
        private String contactPerson;
        private String email;
        private BigDecimal overallScore;
        private BigDecimal cdi;
        private boolean isRecommended;
    }
}

