package com.tcs.VendorPulse.alert.api;

import com.tcs.VendorPulse.alert.domain.Alert;
import com.tcs.VendorPulse.alert.dto.AlertDto;
import com.tcs.VendorPulse.alert.service.AlertService;
import com.tcs.VendorPulse.common.enums.AlertStatus;
import com.tcs.VendorPulse.common.enums.AlertType;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Risk alerts for delays, revisions, stock and CDI")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Search risk alerts")
    public ResponseEntity<PagedResponse<AlertDto>> searchAlerts(
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(alertService.searchAlerts(type, status, pageable));
    }

    @PatchMapping("/{id}/dismiss")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Dismiss an active alert")
    public ResponseEntity<Alert> dismissAlert(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.dismissAlert(id));
    }
}

