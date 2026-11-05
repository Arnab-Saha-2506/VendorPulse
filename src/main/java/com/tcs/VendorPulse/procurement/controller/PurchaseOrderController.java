package com.tcs.VendorPulse.procurement.controller;
import com.tcs.VendorPulse.procurement.entity.PurchaseOrder;
import com.tcs.VendorPulse.procurement.service.PurchaseOrderService;
import com.tcs.VendorPulse.common.enums.PoStatus;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService service;
    public PurchaseOrderController(PurchaseOrderService s){ service=s; }
    @PostMapping("/from-agreement/{agreementId}") public ResponseEntity<PurchaseOrder> generate(@PathVariable Long agreementId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generateFromAgreement(agreementId));
    }
    @GetMapping("/{id}") public PurchaseOrder get(@PathVariable Long id){ return service.get(id); }
    @PutMapping("/{id}/status") public PurchaseOrder updateStatus(@PathVariable Long id, @RequestParam PoStatus status) {
        return service.updateStatus(id, status);
    }
}