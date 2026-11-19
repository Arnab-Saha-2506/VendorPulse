package com.tcs.VendorPulse.message.api;

import com.tcs.VendorPulse.message.dto.MessageDtos.*;
import com.tcs.VendorPulse.message.service.PoMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders/{id}/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Real-time purchase order thread messaging and polling")
public class PoMessageController {

    private final PoMessageService messageService;

    @GetMapping
    @Operation(summary = "Get order messages", description = "Incremental message polling with cursor and unread count")
    public ResponseEntity<MessagePollResponse> getMessages(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID after,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(messageService.getMessages(id, after, Math.min(limit, 100)));
    }

    @PostMapping
    @Operation(summary = "Send message in PO thread")
    public ResponseEntity<PoMessageDto> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.sendMessage(id, request));
    }

    @PostMapping("/read")
    @Operation(summary = "Mark message thread as read")
    public ResponseEntity<Void> markThreadRead(@PathVariable UUID id) {
        messageService.markThreadRead(id);
        return ResponseEntity.noContent().build();
    }
}

