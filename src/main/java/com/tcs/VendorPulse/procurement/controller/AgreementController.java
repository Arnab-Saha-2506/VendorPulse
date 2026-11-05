package com.tcs.VendorPulse.procurement.controller;

import com.tcs.VendorPulse.procurement.dto.AgreementRequestDTO;
import com.tcs.VendorPulse.procurement.entity.Agreement;
import com.tcs.VendorPulse.procurement.service.AgreementService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agreements")
public class AgreementController {
    private final AgreementService service;

    public AgreementController(AgreementService s) {
        service = s;
    }

    @PostMapping
    public ResponseEntity<Agreement> create(@Valid @RequestBody AgreementRequestDTO req, Authentication auth) {
        // managerId resolved from auth; placeholder 1L
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, 1L));
    }

    @GetMapping("/{id}")
    public Agreement get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/accept")
    public Agreement accept(@PathVariable Long id, Authentication auth) {
        return service.acceptBySupplier(id, /* supplier user id from auth */ 1L);
    }

    @PostMapping("/{id}/finalize")
    public Agreement finalizeAgreement(@PathVariable Long id, Authentication auth) {
        return service.finalizeAgreement(id, 1L);
    }
}