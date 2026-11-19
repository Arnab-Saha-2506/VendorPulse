package com.tcs.VendorPulse.procurement.controller;

import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.procurement.entity.PurchaseOrder;
import com.tcs.VendorPulse.procurement.service.PurchaseOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService service;

    public PurchaseOrderController(PurchaseOrderService s) {
        this.service = s;
    }

    @PostMapping("/from-agreement/{agreementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public ResponseEntity<PurchaseOrder> generate(@PathVariable Long agreementId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generateFromAgreement(agreementId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PurchaseOrder get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public PurchaseOrder updateStatus(@PathVariable Long id, @RequestParam PoStatus status) {
        return service.updateStatus(id, status);
    }
}
