package com.tcs.VendorPulse.compare.api;

import com.tcs.VendorPulse.compare.dto.CompareDtos.*;
import com.tcs.VendorPulse.compare.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Compare & Recommendation", description = "Supplier ranking and AI/rule-based recommendation engine")
public class CompareController {

    private final RankingService rankingService;

    @GetMapping("/suppliers/compare")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Compare suppliers by category", description = "Rank category suppliers and receive top recommendation with justifications")
    public ResponseEntity<CompareResponse> compareSuppliers(@RequestParam Short categoryId) {
        return ResponseEntity.ok(rankingService.compareCategorySuppliers(categoryId));
    }

    @GetMapping("/categories/{id}/recommended-supplier")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get recommended supplier for category", description = "Top recommended supplier on PO creation form")
    public ResponseEntity<RecommendedSupplierDto> getRecommendedSupplier(@PathVariable Short id) {
        return ResponseEntity.ok(rankingService.getRecommendedSupplierForCategory(id));
    }
}

