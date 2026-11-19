package com.tcs.VendorPulse.supplier.api;

import com.tcs.VendorPulse.common.enums.ApprovalStatus;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.dto.SupplierDtos.*;
import com.tcs.VendorPulse.supplier.service.SupplierApprovalService;
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
@Tag(name = "Supplier Approvals", description = "Procurement Manager registration approval review APIs")
public class SupplierApprovalController {

    private final SupplierApprovalService approvalService;

    @GetMapping("/supplier-registrations")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "List pending supplier registrations")
    public ResponseEntity<PagedResponse<SupplierSummaryDto>> getRegistrations(
            @RequestParam(required = false, defaultValue = "PENDING") ApprovalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(approvalService.getRegistrations(status, pageable));
    }

    @GetMapping("/supplier-registrations/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get registration review detail")
    public ResponseEntity<SupplierDetailDto> getRegistrationDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(approvalService.getRegistrationDetail(id));
    }

    @PostMapping("/supplier-registrations/{id}/approve")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Approve supplier registration")
    public ResponseEntity<Supplier> approveRegistration(@PathVariable UUID id) {
        return ResponseEntity.ok(approvalService.approveRegistration(id));
    }

    @PostMapping("/supplier-registrations/{id}/reject")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Reject supplier registration with reason")
    public ResponseEntity<Supplier> rejectRegistration(
            @PathVariable UUID id,
            @RequestBody(required = false) SupplierApprovalRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(approvalService.rejectRegistration(id, reason));
    }

    @PatchMapping("/supplier-documents/{docId}/review")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Review one document (approve/reject)")
    public ResponseEntity<SupplierDocumentDto> reviewDocument(
            @PathVariable UUID docId,
            @Valid @RequestBody DocumentReviewRequest request) {
        return ResponseEntity.ok(approvalService.reviewDocument(docId, request));
    }
}

