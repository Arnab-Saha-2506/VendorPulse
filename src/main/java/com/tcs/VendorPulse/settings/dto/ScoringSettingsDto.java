package com.tcs.VendorPulse.settings.dto;

import jakarta.validation.constraints.NotNull;
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
public class ScoringSettingsDto {
    private Short id;

    @NotNull(message = "Delivery reliability weight is required")
    private Short wDeliveryReliability;

    @NotNull(message = "Commitment stability weight is required")
    private Short wCommitmentStability;

    @NotNull(message = "Delivery consistency weight is required")
    private Short wDeliveryConsistency;

    @NotNull(message = "Quality performance weight is required")
    private Short wQualityPerformance;

    @NotNull(message = "High CDI threshold is required")
    private Short highCdiThreshold;

    @NotNull(message = "Stock alert horizon days is required")
    private Short stockAlertHorizonDays;

    @NotNull(message = "Max negotiation rounds is required")
    private Short maxNegotiationRounds;

    @NotNull(message = "Default cutoff days is required")
    private Short defaultCutoffDays;

    private UUID updatedBy;
    private Instant updatedAt;
}

