package com.tcs.VendorPulse.material.api;

import com.tcs.VendorPulse.material.dto.MaterialDtos.*;
import com.tcs.VendorPulse.material.service.CategoryService;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Material Categories", description = "Material category management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "List categories", description = "Get all categories with active mapped supplier count")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Create category")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Rename category")
    public ResponseEntity<CategoryDto> renameCategory(
            @PathVariable Short id,
            @Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.ok(categoryService.renameCategory(id, request));
    }

    @GetMapping("/{id}/suppliers")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get mapped suppliers for category")
    public ResponseEntity<List<Supplier>> getMappedSuppliers(@PathVariable Short id) {
        return ResponseEntity.ok(categoryService.getMappedSuppliers(id));
    }

    @PutMapping("/{id}/suppliers")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Map suppliers to category", description = "Map approved active suppliers to category")
    public ResponseEntity<Void> mapSuppliers(
            @PathVariable Short id,
            @Valid @RequestBody CategorySupplierMappingRequest request) {
        categoryService.mapSuppliers(id, request.getSupplierIds());
        return ResponseEntity.noContent().build();
    }
}

