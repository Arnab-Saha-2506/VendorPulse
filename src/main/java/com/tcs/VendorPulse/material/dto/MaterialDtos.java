package com.tcs.VendorPulse.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MaterialDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        private Short id;
        private String name;
        private long supplierCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryCreateRequest {
        @NotBlank(message = "Category name is required")
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySupplierMappingRequest {
        @NotNull(message = "Supplier IDs list is required")
        private List<UUID> supplierIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialDto {
        private UUID id;
        private String code;
        private String name;
        private Short categoryId;
        private String categoryName;
        private Boolean isCritical;
        private Boolean isActive;
        private BigDecimal stockQty;
        private BigDecimal dailyUsage;
        private Instant stockUpdatedAt;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateMaterialRequest {
        @NotBlank(message = "Material code is required")
        private String code;

        @NotBlank(message = "Material name is required")
        private String name;

        @NotNull(message = "Category ID is required")
        private Short categoryId;

        private Boolean isCritical;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateMaterialRequest {
        @NotBlank(message = "Material name is required")
        private String name;

        @NotNull(message = "Category ID is required")
        private Short categoryId;

        private Boolean isCritical;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialStatusRequest {
        @NotNull(message = "isActive is required")
        private Boolean isActive;
    }
}

