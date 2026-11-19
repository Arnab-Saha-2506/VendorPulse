package com.tcs.VendorPulse.message.dto;

import com.tcs.VendorPulse.common.enums.PartySide;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MessageDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoMessageDto {
        private UUID id;
        private UUID poId;
        private UUID senderId;
        private String senderName;
        private PartySide senderSide;
        private String body;
        private Instant readAt;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessagePollResponse {
        private List<PoMessageDto> messages;
        private long unreadCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        @NotBlank(message = "Message body cannot be empty")
        private String body;
    }
}

