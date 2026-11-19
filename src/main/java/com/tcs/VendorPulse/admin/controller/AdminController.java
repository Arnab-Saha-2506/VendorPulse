package com.tcs.VendorPulse.admin.controller;

import com.tcs.VendorPulse.admin.service.AdminService;
import com.tcs.VendorPulse.security.SecurityUtils;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.entity.SupplierDocument;
import com.tcs.VendorPulse.user.repository.UserRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/admin/suppliers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService service;
    private final UserRepository users;

    public AdminController(AdminService s, UserRepository u) {
        this.service = s;
        this.users = u;
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
    public Supplier approve(@PathVariable Long id,
                            @RequestParam(required = false) String remarks,
                            Authentication auth) {
        Long adminId = SecurityUtils.getCurrentUserId(auth, users);
        return service.approve(id, adminId, remarks);
    }

    @PutMapping("/{id}/reject")
    public Supplier reject(@PathVariable Long id,
                           @RequestParam(required = false) String remarks,
                           Authentication auth) {
        Long adminId = SecurityUtils.getCurrentUserId(auth, users);
        return service.reject(id, adminId, remarks);
    }

    @GetMapping("/{id}/certificate")
    public ResponseEntity<Resource> getCertificate(@PathVariable Long id) {
        SupplierDocument doc = service.getSupplierCertificate(id);
        Path path = Paths.get(doc.getFilePath());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(path);
        String contentType;
        try {
            contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName().toString() + "\"")
                .body(resource);
    }
}
