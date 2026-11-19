package com.tcs.VendorPulse.agreement.api;

import com.tcs.VendorPulse.agreement.dto.AgreementDto;
import com.tcs.VendorPulse.agreement.service.AgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders/{id}/agreement")
@RequiredArgsConstructor
@Tag(name = "Agreements", description = "Purchase agreement terms and PDF generation")
public class AgreementController {

    private final AgreementService agreementService;

    @GetMapping
    @Operation(summary = "Get agreement terms", description = "Get confirmed agreement terms and digital signature records")
    public ResponseEntity<AgreementDto> getAgreement(@PathVariable UUID id) {
        return ResponseEntity.ok(agreementService.getAgreementByPoId(id));
    }

    @GetMapping("/pdf")
    @Operation(summary = "Download agreement PDF", description = "Download generated legal agreement PDF document")
    public ResponseEntity<byte[]> downloadAgreementPdf(@PathVariable UUID id) {
        byte[] pdf = agreementService.getAgreementPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"agreement-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

