package com.tcs.VendorPulse.material.api;

import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.material.dto.MaterialDtos.*;
import com.tcs.VendorPulse.material.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
@Tag(name = "Materials", description = "Raw material master data APIs")
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Search materials")
    public ResponseEntity<PagedResponse<MaterialDto>> searchMaterials(
            @RequestParam(required = false) Short categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(materialService.searchMaterials(categoryId, q, active, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Add material")
    public ResponseEntity<MaterialDto> createMaterial(@Valid @RequestBody CreateMaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.createMaterial(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get material by ID")
    public ResponseEntity<MaterialDto> getMaterialById(@PathVariable UUID id) {
        return ResponseEntity.ok(materialService.getMaterialById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Edit material")
    public ResponseEntity<MaterialDto> updateMaterial(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMaterialRequest request) {
        return ResponseEntity.ok(materialService.updateMaterial(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Activate or deactivate material")
    public ResponseEntity<MaterialDto> updateMaterialStatus(
            @PathVariable UUID id,
            @Valid @RequestBody MaterialStatusRequest request) {
        return ResponseEntity.ok(materialService.updateMaterialStatus(id, request.getIsActive()));
    }
}

