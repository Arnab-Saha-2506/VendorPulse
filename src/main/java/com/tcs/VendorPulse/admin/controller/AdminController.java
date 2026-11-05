package com.tcs.VendorPulse.admin.controller;

import com.tcs.VendorPulse.admin.service.AdminService;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.user.entity.User;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/suppliers")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService s) {
        service = s;
    }

    @GetMapping("/pending")
    public List<Supplier> pending() {
        return service.pendingSuppliers();
    }

    @GetMapping("/{id}")
    public Supplier get(@PathVariable Long id) {
        return service.getSupplier(id);
    }

    @PutMapping("/{id}/approve")
    public Supplier approve(@PathVariable Long id, @RequestParam(required = false) String remarks,
            @AuthenticationPrincipal User admin) {
        return service.approve(id, admin.getId(), remarks);
    }

    @PutMapping("/{id}/reject")
    public Supplier reject(@PathVariable Long id, @RequestParam(required = false) String remarks,
            @AuthenticationPrincipal User admin) {
        return service.reject(id, admin.getId(), remarks);
    }
}