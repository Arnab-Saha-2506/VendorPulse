package com.tcs.VendorPulse.supplier.service;

import com.tcs.VendorPulse.audit.service.AuditService;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.enums.AccountStatus;
import com.tcs.VendorPulse.common.enums.ApprovalStatus;
import com.tcs.VendorPulse.common.enums.NotificationType;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.material.domain.MaterialCategory;
import com.tcs.VendorPulse.material.repository.MaterialCategoryRepository;
import com.tcs.VendorPulse.notification.service.NotificationService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierApprovalService {

    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final SupplierDocumentRepository documentRepository;
    private final SupplierCategoryRepository supplierCategoryRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<SupplierSummaryDto> getRegistrations(ApprovalStatus status, Pageable pageable) {
        ApprovalStatus filterStatus = status != null ? status : ApprovalStatus.PENDING;
        Page<Supplier> page = supplierRepository.findByApprovalStatus(filterStatus, pageable);

        Map<Short, String> catMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(MaterialCategory::getId, MaterialCategory::getName));

        Page<SupplierSummaryDto> dtoPage = page.map(s -> {
            List<String> catNames = supplierCategoryRepository.findBySupplierId(s.getId()).stream()
                    .map(sc -> catMap.getOrDefault(sc.getCategoryId(), ""))
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toList());

            return SupplierSummaryDto.builder()
                    .id(s.getId())
                    .companyName(s.getCompanyName())
                    .registrationNo(s.getRegistrationNo())
                    .contactPerson(s.getContactPerson())
                    .email(s.getEmail())
                    .phone(s.getPhone())
                    .categories(catNames)
                    .approvalStatus(s.getApprovalStatus())
                    .isActive(s.getIsActive())
                    .submittedAt(s.getSubmittedAt())
                    .build();
        });

        return PagedResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public SupplierDetailDto getRegistrationDetail(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        Map<Short, String> catMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(MaterialCategory::getId, MaterialCategory::getName));

        List<SupplierCategory> categories = supplierCategoryRepository.findBySupplierId(supplier.getId());
        List<Short> catIds = categories.stream().map(SupplierCategory::getCategoryId).collect(Collectors.toList());
        List<String> catNames = catIds.stream().map(cid -> catMap.getOrDefault(cid, "")).filter(n -> !n.isEmpty()).collect(Collectors.toList());

        List<SupplierDocumentDto> docs = documentRepository.findBySupplierId(supplier.getId()).stream()
                .map(this::toDocDto)
                .collect(Collectors.toList());

        return SupplierDetailDto.builder()
                .id(supplier.getId())
                .companyName(supplier.getCompanyName())
                .registrationNo(supplier.getRegistrationNo())
                .contactPerson(supplier.getContactPerson())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .materialType(supplier.getMaterialType())
                .businessUnit(supplier.getBusinessUnit())
                .approvalStatus(supplier.getApprovalStatus())
                .isActive(supplier.getIsActive())
                .rejectionReason(supplier.getRejectionReason())
                .categoryIds(catIds)
                .categoryNames(catNames)
                .documents(docs)
                .submittedAt(supplier.getSubmittedAt())
                .decidedAt(supplier.getDecidedAt())
                .build();
    }

    @Transactional
    public Supplier approveRegistration(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        if (supplier.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ApiException(ErrorCode.ALREADY_PROCESSED);
        }

        CurrentUser current = CurrentUser.get();
        UUID deciderId = current != null ? current.getId() : null;
        String deciderName = current != null ? current.getFullName() : "Procurement Manager";

        supplier.setApprovalStatus(ApprovalStatus.APPROVED);
        supplier.setIsActive(true);
        supplier.setDecidedAt(Instant.now());
        supplier.setDecidedBy(deciderId);
        supplier = supplierRepository.save(supplier);

        // Activate user
        User user = userRepository.findBySupplierId(supplier.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier user not found"));
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // Send APPROVAL notification to supplier
        notificationService.notify(
                supplier.getId(),
                user.getId(),
                NotificationType.APPROVAL,
                "Your supplier registration for " + supplier.getCompanyName() + " has been approved.",
                null
        );

        // Write audit
        auditService.log(
                deciderId,
                deciderName,
                "APPROVE_SUPPLIER_REGISTRATION",
                "SUPPLIER",
                supplier.getId().toString(),
                supplier.getCompanyName()
        );

        return supplier;
    }

    @Transactional
    public Supplier rejectRegistration(UUID id, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Rejection reason is required");
        }

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        if (supplier.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ApiException(ErrorCode.ALREADY_PROCESSED);
        }

        CurrentUser current = CurrentUser.get();
        UUID deciderId = current != null ? current.getId() : null;
        String deciderName = current != null ? current.getFullName() : "Procurement Manager";

        supplier.setApprovalStatus(ApprovalStatus.REJECTED);
        supplier.setRejectionReason(reason.trim());
        supplier.setDecidedAt(Instant.now());
        supplier.setDecidedBy(deciderId);
        supplier = supplierRepository.save(supplier);

        // Set user to REJECTED
        userRepository.findBySupplierId(supplier.getId()).ifPresent(u -> {
            u.setStatus(AccountStatus.REJECTED);
            userRepository.save(u);
        });

        auditService.log(
                deciderId,
                deciderName,
                "REJECT_SUPPLIER_REGISTRATION",
                "SUPPLIER",
                supplier.getId().toString(),
                reason
        );

        return supplier;
    }

    @Transactional
    public SupplierDocumentDto reviewDocument(UUID docId, DocumentReviewRequest request) {
        SupplierDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Document not found"));

        if (request.getReviewStatus() == ApprovalStatus.REJECTED &&
                (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Rejection reason is required when rejecting a document");
        }

        CurrentUser current = CurrentUser.get();
        doc.setReviewStatus(request.getReviewStatus());
        doc.setRejectionReason(request.getRejectionReason());
        doc.setReviewedBy(current != null ? current.getId() : null);
        doc.setReviewedAt(Instant.now());

        doc = documentRepository.save(doc);
        return toDocDto(doc);
    }

    private SupplierDocumentDto toDocDto(SupplierDocument d) {
        return SupplierDocumentDto.builder()
                .id(d.getId())
                .supplierId(d.getSupplierId())
                .fileName(d.getFileName())
                .mimeType(d.getMimeType())
                .sizeBytes(d.getSizeBytes())
                .storageKey(d.getStorageKey())
                .reviewStatus(d.getReviewStatus())
                .rejectionReason(d.getRejectionReason())
                .reviewedBy(d.getReviewedBy())
                .reviewedAt(d.getReviewedAt())
                .replacesDocumentId(d.getReplacesDocumentId())
                .uploadedAt(d.getUploadedAt())
                .build();
    }
}

