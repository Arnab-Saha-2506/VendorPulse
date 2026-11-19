package com.tcs.VendorPulse.dashboard.api;

import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.dashboard.dto.DashboardDtos.*;
import com.tcs.VendorPulse.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "Role-based executive and operational dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/procurement-manager")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Procurement Manager dashboard", description = "Open POs, pending approvals, high-CDI suppliers, and active risk alerts")
    public ResponseEntity<PmDashboardDto> getPmDashboard() {
        return ResponseEntity.ok(dashboardService.getPmDashboard());
    }

    @GetMapping("/supplier")
    @PreAuthorize("hasAnyRole('SUPPLIER_MANAGER', 'SUPPLIER_USER')")
    @Operation(summary = "Supplier dashboard", description = "Proposals awaiting reply, open orders, reliability rating, and CDI")
    public ResponseEntity<SupplierDashboardDto> getSupplierDashboard() {
        CurrentUser current = CurrentUser.get();
        if (current == null || current.getSupplierId() == null) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(dashboardService.getSupplierDashboard(current.getSupplierId()));
    }
}

