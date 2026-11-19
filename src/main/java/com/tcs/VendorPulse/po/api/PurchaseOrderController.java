package com.tcs.VendorPulse.po.api;

import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.dto.PoDtos.*;
import com.tcs.VendorPulse.po.service.PoCancelService;
import com.tcs.VendorPulse.po.service.PoCoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders", description = "Purchase order core and lifecycle APIs")
public class PurchaseOrderController {

    private final PoCoreService poCoreService;
    private final PoCancelService poCancelService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Create PO draft")
    public ResponseEntity<PurchaseOrder> createDraft(@Valid @RequestBody CreatePoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(poCoreService.createDraft(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Edit PO draft")
    public ResponseEntity<PurchaseOrder> editDraft(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePoRequest request) {
        return ResponseEntity.ok(poCoreService.editDraft(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Delete PO draft")
    public ResponseEntity<Void> deleteDraft(@PathVariable UUID id) {
        poCoreService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Submit PO as proposal", description = "Submits draft PO, creates round 1 proposal, notifies supplier")
    public ResponseEntity<PurchaseOrder> submitPo(@PathVariable UUID id) {
        return ResponseEntity.ok(poCoreService.submitPo(id));
    }

    @GetMapping
    @Operation(summary = "List purchase orders", description = "List POs with filters. Scoped to supplier company for suppliers.")
    public ResponseEntity<PagedResponse<PoSummaryDto>> searchPos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) PoStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(poCoreService.searchPos(q, supplierId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PO detail", description = "PO overview with lines, dates, and allowed action flags")
    public ResponseEntity<PoDetailDto> getPoDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(poCoreService.getPoDetail(id));
    }

    @GetMapping("/eligible-suppliers")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get eligible suppliers for PO creation")
    public ResponseEntity<List<EligibleSupplierDto>> getEligibleSuppliers(
            @RequestParam(required = false) List<UUID> materialIds,
            @RequestParam(required = false) Short categoryId) {
        return ResponseEntity.ok(poCoreService.getEligibleSuppliers(materialIds, categoryId));
    }

    @GetMapping("/{id}/cancellation-window")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Check cancellation window")
    public ResponseEntity<CancellationWindowDto> getCancellationWindow(@PathVariable UUID id) {
        return ResponseEntity.ok(poCancelService.getCancellationWindow(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Cancel purchase order")
    public ResponseEntity<PurchaseOrder> cancelPo(
            @PathVariable UUID id,
            @Valid @RequestBody CancelPoRequest request) {
        return ResponseEntity.ok(poCancelService.cancelPo(id, request.getReason()));
    }
}

