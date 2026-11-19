package com.tcs.VendorPulse.supplier.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.VendorPulse.supplier.dto.SupplierDtos.*;
import com.tcs.VendorPulse.supplier.service.SupplierRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/suppliers")
@RequiredArgsConstructor
@Tag(name = "Supplier Registration", description = "Public supplier self-registration APIs")
public class SupplierRegistrationController {

    private final SupplierRegistrationService registrationService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register supplier", description = "Self-service supplier onboarding with document upload")
    public ResponseEntity<SupplierRegistrationResponse> registerSupplier(
            @RequestParam("companyName") String companyName,
            @RequestParam("registrationNo") String registrationNo,
            @RequestParam("contactPerson") String contactPerson,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("address") String address,
            @RequestParam(value = "materialType", required = false) String materialType,
            @RequestParam(value = "businessUnit", required = false) String businessUnit,
            @RequestParam(value = "categoryIds", required = false) String categoryIdsStr,
            @RequestParam("password") String password,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        List<Short> categoryIds = new ArrayList<>();
        if (categoryIdsStr != null && !categoryIdsStr.isBlank()) {
            try {
                if (categoryIdsStr.trim().startsWith("[")) {
                    categoryIds = objectMapper.readValue(categoryIdsStr, new TypeReference<List<Short>>() {});
                } else {
                    for (String part : categoryIdsStr.split(",")) {
                        categoryIds.add(Short.parseShort(part.trim()));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        SupplierRegistrationRequest request = SupplierRegistrationRequest.builder()
                .companyName(companyName)
                .registrationNo(registrationNo)
                .contactPerson(contactPerson)
                .email(email)
                .phone(phone)
                .address(address)
                .materialType(materialType)
                .businessUnit(businessUnit)
                .categoryIds(categoryIds)
                .password(password)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request, files));
    }

    @GetMapping("/registrations/{supplierId}/status")
    @Operation(summary = "Check registration status", description = "Check status of submitted registration (no personal data)")
    public ResponseEntity<RegistrationStatusDto> getRegistrationStatus(@PathVariable UUID supplierId) {
        return ResponseEntity.ok(registrationService.getRegistrationStatus(supplierId));
    }
}

