package com.tcs.VendorPulse.settings.api;

import com.tcs.VendorPulse.settings.dto.ScoringSettingsDto;
import com.tcs.VendorPulse.settings.service.ScoringSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "System scoring configuration APIs")
public class SettingsController {

    private final ScoringSettingsService scoringSettingsService;

    @GetMapping("/scoring")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN', 'PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE')")
    @Operation(summary = "Get scoring rules")
    public ResponseEntity<ScoringSettingsDto> getScoringSettings() {
        return ResponseEntity.ok(scoringSettingsService.getSettings());
    }

    @PutMapping("/scoring")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Save scoring rules", description = "Update scoring weights (must total 100) and thresholds")
    public ResponseEntity<ScoringSettingsDto> updateScoringSettings(@Valid @RequestBody ScoringSettingsDto request) {
        return ResponseEntity.ok(scoringSettingsService.updateSettings(request));
    }
}

