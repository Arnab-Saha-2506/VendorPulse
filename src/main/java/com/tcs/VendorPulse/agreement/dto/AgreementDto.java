package com.tcs.VendorPulse.agreement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementDto {
    private UUID id;
    private UUID poId;
    private String poNumber;
    private LocalDate deliveryDate;
    private BigDecimal priceTotal;
    private Short cutoffDays;
    private UUID acceptedByManufacturer;
    private String manufacturerName;
    private Instant acceptedByManufacturerAt;
    private UUID acceptedBySupplier;
    private String supplierName;
    private Instant acceptedBySupplierAt;
    private String pdfStorageKey;
    private Instant createdAt;
}

