package com.tcs.VendorPulse.commitment.dto;

import com.tcs.VendorPulse.common.enums.CommitmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CommitmentDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitmentRevisionDto {
        private Short seq;
        private CommitmentType type;
        private LocalDate date;
        private String reason;
        private UUID user;
        private String userName;
        private Instant time;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitmentTimelineDto {
        private UUID poId;
        private String poNumber;
        private LocalDate originalDate;
        private LocalDate latestCommittedDate;
        private LocalDate actualDeliveryDate;
        private List<CommitmentRevisionDto> revisions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviseCommitmentRequest {
        @NotNull(message = "New delivery date is required")
        private LocalDate newDate;

        @NotBlank(message = "Reason is required")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {
        @NotBlank(message = "Status is required (DISPATCHED or DELIVERED)")
        private String status;

        @NotNull(message = "Date is required")
        private LocalDate date;
    }
}

