package com.tcs.VendorPulse.alert.dto;

import com.tcs.VendorPulse.common.enums.AlertStatus;
import com.tcs.VendorPulse.common.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
    private UUID id;
    private AlertType type;
    private String message;
    private AlertStatus status;
    private UUID poId;
    private String poNumber;
    private UUID materialId;
    private String materialName;
    private UUID supplierId;
    private String supplierName;
    private Instant raisedAt;
    private Instant resolvedAt;
}

