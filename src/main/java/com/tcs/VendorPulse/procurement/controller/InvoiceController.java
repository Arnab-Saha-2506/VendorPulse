package com.tcs.VendorPulse.procurement.controller;

import com.tcs.VendorPulse.procurement.entity.Invoice;
import com.tcs.VendorPulse.procurement.service.InvoiceService;
import com.tcs.VendorPulse.security.SecurityUtils;
import com.tcs.VendorPulse.user.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService service;
    private final UserRepository users;

    public InvoiceController(InvoiceService s, UserRepository u) {
        this.service = s;
        this.users = u;
    }

    @PostMapping("/generate/{poId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public Invoice generate(@PathVariable Long poId, Authentication auth) {
        Long managerId = SecurityUtils.getCurrentUserId(auth, users);
        return service.generate(poId, managerId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Invoice get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('SUPPLIER')")
    public Invoice confirm(@PathVariable Long id, Authentication auth) {
        Long supplierUserId = SecurityUtils.getCurrentUserId(auth, users);
        return service.confirm(id, supplierUserId);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public Invoice approve(@PathVariable Long id, Authentication auth) {
        Long managerId = SecurityUtils.getCurrentUserId(auth, users);
        return service.approve(id, managerId);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public Invoice reject(@PathVariable Long id, @RequestParam String reason, Authentication auth) {
        Long managerId = SecurityUtils.getCurrentUserId(auth, users);
        return service.reject(id, managerId, reason);
    }
}
