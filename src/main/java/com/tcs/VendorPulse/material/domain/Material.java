package com.tcs.VendorPulse.material.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "materials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", length = 30, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 160, nullable = false)
    private String name;

    @Column(name = "category_id", nullable = false)
    private Short categoryId;

    @Column(name = "is_critical", nullable = false)
    private Boolean isCritical;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "stock_qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal stockQty;

    @Column(name = "daily_usage", nullable = false, precision = 14, scale = 2)
    private BigDecimal dailyUsage;

    @Column(name = "stock_updated_at")
    private Instant stockUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.isCritical == null) {
            this.isCritical = false;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.stockQty == null) {
            this.stockQty = BigDecimal.ZERO;
        }
        if (this.dailyUsage == null) {
            this.dailyUsage = BigDecimal.ZERO;
        }
    }
}

