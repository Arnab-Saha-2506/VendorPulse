package com.tcs.VendorPulse.negotiation.dto;

import com.tcs.VendorPulse.common.enums.NegotiationAction;
import com.tcs.VendorPulse.common.enums.PartySide;
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

public class NegotiationDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NegotiationRoundDto {
        private UUID id;
        private Short roundNo;
        private PartySide fromParty;
        private NegotiationAction action;
        private LocalDate proposedDate;
        private BigDecimal proposedPrice;
        private String reason;
        private UUID madeBy;
        private String madeByName;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NegotiationOverviewDto {
        private UUID poId;
        private String poNumber;
        private Short currentRound;
        private Short maxRounds;
        private PartySide awaitingParty;
        private List<NegotiationRoundDto> rounds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NegotiationResponseRequest {
        @NotNull(message = "Action is required (ACCEPTED, COUNTER, REJECTED)")
        private NegotiationAction action;

        private LocalDate proposedDate;
        private BigDecimal proposedPrice;

        @NotBlank(message = "Reason is required")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProposalSummaryDto {
        private UUID poId;
        private String poNumber;
        private Short round;
        private LocalDate dueDate;
        private BigDecimal proposedPrice;
        private LocalDate proposedDate;
        private String reason;
    }
}

