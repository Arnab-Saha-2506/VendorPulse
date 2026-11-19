package com.tcs.VendorPulse.supplier.api;

import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.dto.SupplierDtos.*;
import com.tcs.VendorPulse.supplier.service.SupplierProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Supplier profile and document management APIs")
public class SupplierController {

    private final SupplierProfileService profileService;

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "List approved suppliers with scores")
    public ResponseEntity<PagedResponse<SupplierSummaryDto>> searchSuppliers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Short categoryId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(profileService.searchSuppliers(q, categoryId, status, pageable));
    }

    @GetMapping("/suppliers/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Supplier detail with order and score overview")
    public ResponseEntity<SupplierDetailDto> getSupplierDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(profileService.getSupplierDetail(id));
    }

    @GetMapping("/suppliers/{id}/orders")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Supplier order history")
    public ResponseEntity<PagedResponse<SupplierOrderHistoryDto>> getSupplierOrders(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(profileService.getSupplierOrders(id, pageable));
    }

    @PutMapping("/suppliers/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Edit supplier profile")
    public ResponseEntity<Supplier> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierRequest request) {
        return ResponseEntity.ok(profileService.updateSupplier(id, request));
    }

    @PatchMapping("/suppliers/{id}/status")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Activate or deactivate supplier")
    public ResponseEntity<Supplier> updateSupplierStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierStatusRequest request) {
        return ResponseEntity.ok(profileService.updateSupplierStatus(id, request.getIsActive()));
    }

    @GetMapping("/suppliers/me")
    @PreAuthorize("hasAnyRole('SUPPLIER_MANAGER', 'SUPPLIER_USER')")
    @Operation(summary = "Get own company profile (for suppliers)")
    public ResponseEntity<SupplierDetailDto> getMyProfile() {
        return ResponseEntity.ok(profileService.getMyProfile());
    }

    @PutMapping("/suppliers/me")
    @PreAuthorize("hasAnyRole('SUPPLIER_MANAGER', 'SUPPLIER_USER')")
    @Operation(summary = "Save own company profile")
    public ResponseEntity<SupplierDetailDto> updateMyProfile(@Valid @RequestBody UpdateMyProfileRequest request) {
        return ResponseEntity.ok(profileService.updateMyProfile(request));
    }

    @PostMapping(value = "/suppliers/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPPLIER_MANAGER', 'SUPPLIER_USER')")
    @Operation(summary = "Upload or replace document (own company)")
    public ResponseEntity<SupplierDocumentDto> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "replacesDocumentId", required = false) UUID replacesDocumentId) {
        CurrentUser current = CurrentUser.get();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.addOrReplaceDocument(current.getSupplierId(), file, replacesDocumentId));
    }

    @GetMapping("/suppliers/{id}/documents")
    @Operation(summary = "Get supplier documents")
    public ResponseEntity<List<SupplierDocumentDto>> getSupplierDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(profileService.getSupplierDocuments(id));
    }

    @GetMapping("/supplier-documents/{docId}/download")
    @Operation(summary = "Download a supplier document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID docId) {
        Resource resource = profileService.downloadDocument(docId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}

