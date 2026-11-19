package com.tcs.VendorPulse.supplier.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "supplier_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(SupplierCategory.SupplierCategoryId.class)
public class SupplierCategory {

    @Id
    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Id
    @Column(name = "category_id", nullable = false)
    private Short categoryId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierCategoryId implements Serializable {
        private UUID supplierId;
        private Short categoryId;
    }
}

