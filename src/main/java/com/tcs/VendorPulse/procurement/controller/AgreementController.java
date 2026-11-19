package com.tcs.VendorPulse.procurement.controller;

import com.tcs.VendorPulse.common.enums.Role;
import com.tcs.VendorPulse.procurement.dto.AgreementRequestDTO;
import com.tcs.VendorPulse.procurement.entity.Agreement;
import com.tcs.VendorPulse.procurement.service.AgreementService;
import com.tcs.VendorPulse.security.SecurityUtils;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.user.entity.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agreements")
public class AgreementController {
    private final AgreementService service;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;

    public AgreementController(AgreementService s, UserRepository u, SupplierRepository sr) {
        this.service = s;
        this.userRepository = u;
        this.supplierRepository = sr;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public ResponseEntity<Agreement> create(@Valid @RequestBody AgreementRequestDTO req, Authentication auth) {
        Long managerId = SecurityUtils.getCurrentUserId(auth, userRepository);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, managerId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public List<Agreement> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agreement> get(@PathVariable Long id, Authentication auth) {
        Agreement a = service.get(id);
        User user = SecurityUtils.getCurrentUser(auth, userRepository);
        if (user.getRole() == Role.SUPPLIER) {
            if (a.getSupplier().getUser() == null || !a.getSupplier().getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Suppliers may only view their own agreements");
            }
        }
        return ResponseEntity.ok(a);
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public Agreement send(@PathVariable Long id, Authentication auth) {
        Long managerId = SecurityUtils.getCurrentUserId(auth, userRepository);
        return service.send(id, managerId);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('SUPPLIER')")
    public Agreement accept(@PathVariable Long id, Authentication auth) {
        Long supplierUserId = SecurityUtils.getCurrentUserId(auth, userRepository);
        return service.acceptBySupplier(id, supplierUserId);
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public Agreement finalizeAgreement(@PathVariable Long id, Authentication auth) {
        Long adminId = SecurityUtils.getCurrentUserId(auth, userRepository);
        return service.finalizeAgreement(id, adminId);
    }

    @GetMapping("/supplier/{supplierId}")
    public List<Agreement> bySupplier(@PathVariable Long supplierId, Authentication auth) {
        User user = SecurityUtils.getCurrentUser(auth, userRepository);
        if (user.getRole() == Role.SUPPLIER) {
            Supplier s = supplierRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException("No supplier associated with current user"));
            if (!s.getId().equals(supplierId)) {
                throw new AccessDeniedException("Suppliers may only view their own agreements");
            }
        }
        return service.bySupplier(supplierId);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SUPPLIER')")
    public List<Agreement> myAgreements(Authentication auth) {
        Long userId = SecurityUtils.getCurrentUserId(auth, userRepository);
        Supplier s = supplierRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No supplier profile associated with this account"));
        return service.bySupplier(s.getId());
    }
}
