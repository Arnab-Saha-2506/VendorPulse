package com.tcs.VendorPulse.negotiation.api;

import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.negotiation.dto.NegotiationDtos.*;
import com.tcs.VendorPulse.negotiation.service.NegotiationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Negotiation", description = "Purchase order negotiation and proposal APIs")
public class NegotiationController {

    private final NegotiationService negotiationService;

    @GetMapping("/purchase-orders/{id}/negotiation")
    @Operation(summary = "Get negotiation rounds", description = "Get negotiation history and awaiting party turn")
    public ResponseEntity<NegotiationOverviewDto> getNegotiation(@PathVariable UUID id) {
        return ResponseEntity.ok(negotiationService.getNegotiationOverview(id));
    }

    @PostMapping("/purchase-orders/{id}/negotiation/respond")
    @Operation(summary = "Respond to negotiation round", description = "Accept, counter, or reject proposal (strictly enforced party turns)")
    public ResponseEntity<NegotiationOverviewDto> respondNegotiation(
            @PathVariable UUID id,
            @Valid @RequestBody NegotiationResponseRequest request) {
        return ResponseEntity.ok(negotiationService.respond(id, request));
    }

    @GetMapping("/proposals")
    @PreAuthorize("hasAnyRole('SUPPLIER_MANAGER', 'SUPPLIER_USER')")
    @Operation(summary = "List supplier pending proposals", description = "Proposals awaiting supplier action")
    public ResponseEntity<PagedResponse<ProposalSummaryDto>> getSupplierProposals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        CurrentUser current = CurrentUser.get();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(negotiationService.getSupplierProposals(current.getSupplierId(), pageable));
    }
}

