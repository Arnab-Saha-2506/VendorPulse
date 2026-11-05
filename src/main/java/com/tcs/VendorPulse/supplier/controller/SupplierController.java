package com.tcs.VendorPulse.supplier.controller;

import com.tcs.VendorPulse.supplier.dto.SupplierRegistrationRequest;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService service;

    public SupplierController(SupplierService s) {
        service = s;
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(@RequestPart("data") @Valid SupplierRegistrationRequest data,
            @RequestPart("gstCertificate") MultipartFile file) throws Exception {
        Supplier s = service.register(data, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                java.util.Map.of("supplierId", s.getId(), "status", s.getStatus()));
    }
}