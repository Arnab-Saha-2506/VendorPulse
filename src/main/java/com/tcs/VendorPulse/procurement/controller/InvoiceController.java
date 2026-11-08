package com.tcs.VendorPulse.procurement.controller;

import com.tcs.VendorPulse.procurement.entity.Invoice;
import com.tcs.VendorPulse.procurement.service.InvoiceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService service;

    public InvoiceController(InvoiceService s) {
        service = s;
    }

    @PostMapping("/generate/{poId}")
    public Invoice generate(@PathVariable Long poId, @RequestParam Long managerId) {
        return service.generate(poId, managerId);
    }

    @GetMapping("/{id}")
    public Invoice get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/confirm")
    public Invoice confirm(@PathVariable Long id, @RequestParam Long supplierUserId) {
        return service.confirm(id, supplierUserId);
    }

    @PostMapping("/{id}/finalize")
    public Invoice finalizeInv(@PathVariable Long id, @RequestParam String invoiceNumber,
            @RequestParam Long supplierUserId) {
        return service.finalizeInvoice(id, invoiceNumber, supplierUserId);
    }
}