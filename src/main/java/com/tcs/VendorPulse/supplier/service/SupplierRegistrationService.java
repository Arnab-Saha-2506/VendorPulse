package com.tcs.VendorPulse.supplier.service;

import com.tcs.VendorPulse.common.enums.AccountStatus;
import com.tcs.VendorPulse.common.enums.ApprovalStatus;
import com.tcs.VendorPulse.common.enums.UserRole;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.storage.FileStorageService;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.domain.SupplierCategory;
import com.tcs.VendorPulse.supplier.domain.SupplierDocument;
import com.tcs.VendorPulse.supplier.dto.SupplierDtos.*;
import com.tcs.VendorPulse.supplier.repository.SupplierCategoryRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierDocumentRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.user.domain.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierRegistrationService {

    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final SupplierDocumentRepository documentRepository;
    private final SupplierCategoryRepository supplierCategoryRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SupplierRegistrationResponse register(SupplierRegistrationRequest request, List<MultipartFile> files) {
        String email = request.getEmail().trim().toLowerCase();
        String regNo = request.getRegistrationNo().trim();

        if (supplierRepository.existsByEmailIgnoreCase(email) || userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_EXISTS);
        }
        if (supplierRepository.existsByRegistrationNo(regNo)) {
            throw new ApiException(ErrorCode.REGISTRATION_NO_EXISTS);
        }

        // Validate files if present
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    throw new ApiException(ErrorCode.INVALID_FILE, "One or more uploaded files are empty");
                }
            }
        }

        // 1. Create Supplier (PENDING)
        UUID supplierId = UUID.randomUUID();
        Supplier supplier = Supplier.builder()
                .id(supplierId)
                .companyName(request.getCompanyName().trim())
                .registrationNo(regNo)
                .contactPerson(request.getContactPerson().trim())
                .email(email)
                .phone(request.getPhone().trim())
                .address(request.getAddress().trim())
                .materialType(request.getMaterialType())
                .businessUnit(request.getBusinessUnit())
                .approvalStatus(ApprovalStatus.PENDING)
                .isActive(true)
                .build();
        supplier = supplierRepository.save(supplier);

        // 2. Create User (PENDING - cannot log in until approved)
        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName(request.getContactPerson().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.SUPPLIER_MANAGER)
                .status(AccountStatus.PENDING)
                .supplierId(supplier.getId())
                .build();
        userRepository.save(user);

        // 3. Save Documents
        if (files != null && !files.isEmpty()) {
            List<SupplierDocument> docs = new ArrayList<>();
            for (MultipartFile file : files) {
                String storageKey = fileStorageService.storeDocument(file);
                docs.add(SupplierDocument.builder()
                        .id(UUID.randomUUID())
                        .supplierId(supplier.getId())
                        .fileName(file.getOriginalFilename())
                        .mimeType(file.getContentType())
                        .sizeBytes(file.getSize())
                        .storageKey(storageKey)
                        .reviewStatus(ApprovalStatus.PENDING)
                        .uploadedBy(user.getId())
                        .build());
            }
            documentRepository.saveAll(docs);
        }

        // 4. Map Categories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<SupplierCategory> categories = new ArrayList<>();
            for (Short catId : request.getCategoryIds()) {
                categories.add(SupplierCategory.builder()
                        .supplierId(supplier.getId())
                        .categoryId(catId)
                        .build());
            }
            supplierCategoryRepository.saveAll(categories);
        }

        return SupplierRegistrationResponse.builder()
                .supplierId(supplier.getId())
                .status(supplier.getApprovalStatus())
                .submittedAt(supplier.getSubmittedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public RegistrationStatusDto getRegistrationStatus(UUID supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        return RegistrationStatusDto.builder()
                .status(supplier.getApprovalStatus())
                .submittedAt(supplier.getSubmittedAt())
                .decidedAt(supplier.getDecidedAt())
                .build();
    }
}

