package com.tcs.VendorPulse.scoring.api;

import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.scoring.dto.ScoreDtos.*;
import com.tcs.VendorPulse.scoring.service.ScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Scores", description = "Supplier scorecard, performance history, and calculation APIs")
public class ScoreController {

    private final ScoringService scoringService;

    @PostMapping("/scores/recalculate")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Recalculate supplier scores", description = "Recalculate scores for one or all suppliers and snapshot monthly history")
    public ResponseEntity<RecalculateResponse> recalculateScores(
            @RequestParam(required = false) UUID supplierId) {
        return ResponseEntity.ok(scoringService.recalculateAll(supplierId));
    }

    @GetMapping("/suppliers/me/scorecard")
    @PreAuthorize("hasAnyRole('SUPPLIER_MANAGER', 'SUPPLIER_USER')")
    @Operation(summary = "Get own company scorecard (for supplier)", description = "View 4 score components, overall rating, and CDI")
    public ResponseEntity<ScorecardDto> getMyScorecard() {
        CurrentUser current = CurrentUser.get();
        if (current == null || current.getSupplierId() == null) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(scoringService.getScorecard(current.getSupplierId()));
    }

    @GetMapping("/suppliers/{id}/scorecard")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "View supplier scorecard (for manager)")
    public ResponseEntity<ScorecardDto> getSupplierScorecard(@PathVariable UUID id) {
        return ResponseEntity.ok(scoringService.getScorecard(id));
    }

    @GetMapping("/suppliers/{id}/performance-history")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get supplier performance history", description = "Delivery stats and monthly trend within date range")
    public ResponseEntity<SupplierPerformanceHistoryDto> getPerformanceHistory(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(scoringService.getPerformanceHistory(id, from, to));
    }
}

