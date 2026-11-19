package com.tcs.VendorPulse.commitment.api;

import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.commitment.dto.CommitmentDtos.*;
import com.tcs.VendorPulse.commitment.service.CommitmentService;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders/{id}")
@RequiredArgsConstructor
@Tag(name = "Commitments & Status", description = "Commitment revision timeline and supplier status updates")
public class CommitmentController {

    private final CommitmentService commitmentService;

    @GetMapping("/commitments")
    @Operation(summary = "Get commitments timeline", description = "Original promise and append-only revisions history")
    public ResponseEntity<CommitmentTimelineDto> getCommitments(@PathVariable UUID id) {
        return ResponseEntity.ok(commitmentService.getCommitmentTimeline(id));
    }

    @PostMapping("/commitments/revisions")
    @PreAuthorize("hasAnyRole('SUPPLIER_MANAGER', 'SUPPLIER_USER')")
    @Operation(summary = "Revise committed delivery date", description = "Supplier requests revised delivery date with mandatory reason")
    public ResponseEntity<PoCommitment> reviseCommitment(
            @PathVariable UUID id,
            @Valid @RequestBody ReviseCommitmentRequest request) {
        return ResponseEntity.ok(commitmentService.reviseDeliveryDate(id, request));
    }

    @PostMapping("/status-updates")
    @PreAuthorize("hasAnyRole('SUPPLIER_MANAGER', 'SUPPLIER_USER')")
    @Operation(summary = "Supplier updates order status", description = "Mark as DISPATCHED (sets IN_PROGRESS) or DELIVERED")
    public ResponseEntity<PurchaseOrder> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(commitmentService.updateStatus(id, request));
    }
}

