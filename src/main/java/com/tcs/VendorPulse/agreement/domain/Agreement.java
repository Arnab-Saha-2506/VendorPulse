package com.tcs.VendorPulse.agreement.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "agreements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agreement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "po_id", nullable = false, unique = true)
    private UUID poId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "price_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal priceTotal;

    @Column(name = "cutoff_days", nullable = false)
    private Short cutoffDays;

    @Column(name = "accepted_by_manufacturer", nullable = false)
    private UUID acceptedByManufacturer;

    @Column(name = "accepted_by_manufacturer_at", nullable = false)
    private Instant acceptedByManufacturerAt;

    @Column(name = "accepted_by_supplier", nullable = false)
    private UUID acceptedBySupplier;

    @Column(name = "accepted_by_supplier_at", nullable = false)
    private Instant acceptedBySupplierAt;

    @Column(name = "pdf_storage_key", columnDefinition = "TEXT")
    private String pdfStorageKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}

