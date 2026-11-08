package com.tcs.VendorPulse.scoring.controller;

import com.tcs.VendorPulse.scoring.entity.SupplierScore;
import com.tcs.VendorPulse.scoring.service.SupplierScoringService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierScoringController {
    private final SupplierScoringService service;

    public SupplierScoringController(SupplierScoringService s) {
        service = s;
    }

    @PostMapping("/{id}/score")
    public SupplierScore calculate(@PathVariable Long id) {
        return service.calculate(id);
    }

    @GetMapping("/{id}/score")
    public SupplierScore get(@PathVariable Long id) {
        return service.get(id);
    }
}