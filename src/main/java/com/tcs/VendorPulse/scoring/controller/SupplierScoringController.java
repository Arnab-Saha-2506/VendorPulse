package com.tcs.VendorPulse.scoring.controller;

import com.tcs.VendorPulse.scoring.entity.SupplierScore;
import com.tcs.VendorPulse.scoring.service.SupplierScoringService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierScoringController {
    private final SupplierScoringService service;

    public SupplierScoringController(SupplierScoringService s) {
        this.service = s;
    }

    @PostMapping("/{id}/score")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public SupplierScore calculate(@PathVariable Long id) {
        return service.calculate(id);
    }

    @GetMapping("/{id}/score")
    @PreAuthorize("isAuthenticated()")
    public SupplierScore get(@PathVariable Long id) {
        return service.get(id);
    }
}
