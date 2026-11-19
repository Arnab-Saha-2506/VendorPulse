package com.tcs.VendorPulse.supplier.dto;

import com.tcs.VendorPulse.common.enums.ApprovalStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

public class SupplierDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierRegistrationRequest {
        @NotBlank(message = "Company name is required")
        private String companyName;

        @NotBlank(message = "Registration number is required")
        private String registrationNo;

        @NotBlank(message = "Contact person is required")
        private String contactPerson;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Phone is required")
        private String phone;

        @NotBlank(message = "Address is required")
        private String address;

        private String materialType;
        private String businessUnit;
        private List<Short> categoryIds;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierRegistrationResponse {
        private UUID supplierId;
        private ApprovalStatus status;
        private Instant submittedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrationStatusDto {
        private ApprovalStatus status;
        private Instant submittedAt;
        private Instant decidedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierApprovalRequest {
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentReviewRequest {
        @NotNull(message = "Review status is required")
        private ApprovalStatus reviewStatus;
        private String rejectionReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierDocumentDto {
        private UUID id;
        private UUID supplierId;
        private String fileName;
        private String mimeType;
        private Long sizeBytes;
        private String storageKey;
        private ApprovalStatus reviewStatus;
        private String rejectionReason;
        private UUID reviewedBy;
        private Instant reviewedAt;
        private UUID replacesDocumentId;
        private Instant uploadedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierSummaryDto {
        private UUID id;
        private String companyName;
        private String registrationNo;
        private String contactPerson;
        private String email;
        private String phone;
        private List<String> categories;
        private ApprovalStatus approvalStatus;
        private Boolean isActive;
        private BigDecimal overallScore;
        private BigDecimal cdi;
        private String ratingLabel;
        private Instant submittedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierDetailDto {
        private UUID id;
        private String companyName;
        private String registrationNo;
        private String contactPerson;
        private String email;
        private String phone;
        private String address;
        private String materialType;
        private String businessUnit;
        private ApprovalStatus approvalStatus;
        private Boolean isActive;
        private String rejectionReason;
        private List<Short> categoryIds;
        private List<String> categoryNames;
        private List<SupplierDocumentDto> documents;
        private BigDecimal overallScore;
        private BigDecimal cdi;
        private Instant submittedAt;
        private Instant decidedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSupplierRequest {
        @NotBlank(message = "Contact person is required")
        private String contactPerson;

        @NotBlank(message = "Phone is required")
        private String phone;

        @NotBlank(message = "Address is required")
        private String address;

        private String materialType;
        private String businessUnit;
        private List<Short> categoryIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateMyProfileRequest {
        @NotBlank(message = "Contact person is required")
        private String contactPerson;

        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "Phone is required")
        private String phone;

        private String address;
        private List<Short> categoryIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierStatusRequest {
        @NotNull(message = "isActive is required")
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierOrderHistoryDto {
        private UUID id;
        private String poNumber;
        private String status;
        private LocalDate originalDate;
        private LocalDate latestDate;
        private long revisions;
    }
}

