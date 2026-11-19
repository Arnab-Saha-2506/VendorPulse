package com.tcs.VendorPulse.supplier.service;

import com.tcs.VendorPulse.audit.service.AuditService;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.commitment.repository.PoCommitmentRepository;
import com.tcs.VendorPulse.common.enums.ApprovalStatus;
import com.tcs.VendorPulse.common.enums.CommitmentType;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.common.storage.FileStorageService;
import com.tcs.VendorPulse.material.domain.MaterialCategory;
import com.tcs.VendorPulse.material.repository.MaterialCategoryRepository;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.scoring.domain.SupplierScoreHistory;
import com.tcs.VendorPulse.scoring.repository.SupplierScoreHistoryRepository;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.domain.SupplierCategory;
import com.tcs.VendorPulse.supplier.domain.SupplierDocument;
import com.tcs.VendorPulse.supplier.dto.SupplierDtos.*;
import com.tcs.VendorPulse.supplier.repository.SupplierCategoryRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierDocumentRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierProfileService {

    private final SupplierRepository supplierRepository;
    private final SupplierDocumentRepository documentRepository;
    private final SupplierCategoryRepository supplierCategoryRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final SupplierScoreHistoryRepository scoreHistoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoCommitmentRepository poCommitmentRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<SupplierSummaryDto> searchSuppliers(String q, Short categoryId, Boolean status, Pageable pageable) {
        Page<Supplier> page;
        if (categoryId != null) {
            page = supplierRepository.searchApprovedSuppliersWithCategory(q, categoryId, status, pageable);
        } else {
            page = supplierRepository.searchApprovedSuppliers(q, status, pageable);
        }

        Map<Short, String> catMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(MaterialCategory::getId, MaterialCategory::getName));

        Page<SupplierSummaryDto> dtoPage = page.map(s -> {
            List<String> catNames = supplierCategoryRepository.findBySupplierId(s.getId()).stream()
                    .map(sc -> catMap.getOrDefault(sc.getCategoryId(), ""))
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toList());

            Optional<SupplierScoreHistory> latestScore = scoreHistoryRepository.findFirstBySupplierIdOrderByPeriodDesc(s.getId());
            var builder = SupplierSummaryDto.builder()
                    .id(s.getId())
                    .companyName(s.getCompanyName())
                    .registrationNo(s.getRegistrationNo())
                    .contactPerson(s.getContactPerson())
                    .email(s.getEmail())
                    .phone(s.getPhone())
                    .categories(catNames)
                    .approvalStatus(s.getApprovalStatus())
                    .isActive(s.getIsActive())
                    .submittedAt(s.getSubmittedAt());

            if (latestScore.isPresent() && latestScore.get().getOrdersCounted() >= 3) {
                builder.overallScore(latestScore.get().getOverallScore());
                builder.cdi(latestScore.get().getCdi());
                builder.ratingLabel(String.format("%.1f", latestScore.get().getOverallScore()));
            } else {
                builder.ratingLabel("Not rated");
            }

            return builder.build();
        });

        return PagedResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public SupplierDetailDto getSupplierDetail(UUID id) {
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

        Optional<SupplierScoreHistory> latestScore = scoreHistoryRepository.findFirstBySupplierIdOrderByPeriodDesc(supplier.getId());

        var builder = SupplierDetailDto.builder()
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
                .decidedAt(supplier.getDecidedAt());

        latestScore.ifPresent(sh -> {
            builder.overallScore(sh.getOverallScore());
            builder.cdi(sh.getCdi());
        });

        return builder.build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<SupplierOrderHistoryDto> getSupplierOrders(UUID supplierId, Pageable pageable) {
        List<PurchaseOrder> allPos = purchaseOrderRepository.findBySupplierId(supplierId);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allPos.size());
        List<PurchaseOrder> subList = start <= end ? allPos.subList(start, end) : Collections.emptyList();

        List<SupplierOrderHistoryDto> dtoList = subList.stream().map(po -> {
            Optional<PoCommitment> orig = poCommitmentRepository.findByPoIdAndSeqNo(po.getId(), (short) 0);
            Optional<PoCommitment> latest = poCommitmentRepository.findFirstByPoIdOrderBySeqNoDesc(po.getId());
            long revCount = poCommitmentRepository.countByPoIdAndType(po.getId(), CommitmentType.REVISION);

            return SupplierOrderHistoryDto.builder()
                    .id(po.getId())
                    .poNumber(po.getPoNumber())
                    .status(po.getStatus().name())
                    .originalDate(orig.map(PoCommitment::getCommittedDate).orElse(po.getRequiredDate()))
                    .latestDate(latest.map(PoCommitment::getCommittedDate).orElse(po.getAgreedDeliveryDate()))
                    .revisions(revCount)
                    .build();
        }).collect(Collectors.toList());

        Page<SupplierOrderHistoryDto> page = new PageImpl<>(dtoList, pageable, allPos.size());
        return PagedResponse.from(page);
    }

    @Transactional
    public Supplier updateSupplier(UUID id, UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        supplier.setContactPerson(request.getContactPerson().trim());
        supplier.setPhone(request.getPhone().trim());
        supplier.setAddress(request.getAddress().trim());
        supplier.setMaterialType(request.getMaterialType());
        supplier.setBusinessUnit(request.getBusinessUnit());

        if (request.getCategoryIds() != null) {
            supplierCategoryRepository.deleteBySupplierId(supplier.getId());
            List<SupplierCategory> mappings = new ArrayList<>();
            for (Short cId : request.getCategoryIds()) {
                mappings.add(SupplierCategory.builder()
                        .supplierId(supplier.getId())
                        .categoryId(cId)
                        .build());
            }
            supplierCategoryRepository.saveAll(mappings);
        }

        supplier = supplierRepository.save(supplier);

        CurrentUser current = CurrentUser.get();
        auditService.log(
                current != null ? current.getId() : null,
                current != null ? current.getFullName() : "System",
                "UPDATE_SUPPLIER_PROFILE",
                "SUPPLIER",
                supplier.getId().toString(),
                supplier.getCompanyName()
        );

        return supplier;
    }

    @Transactional
    public Supplier updateSupplierStatus(UUID id, Boolean isActive) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        supplier.setIsActive(Boolean.TRUE.equals(isActive));
        supplier = supplierRepository.save(supplier);

        CurrentUser current = CurrentUser.get();
        auditService.log(
                current != null ? current.getId() : null,
                current != null ? current.getFullName() : "System",
                Boolean.TRUE.equals(isActive) ? "ACTIVATE_SUPPLIER" : "DEACTIVATE_SUPPLIER",
                "SUPPLIER",
                supplier.getId().toString(),
                supplier.getCompanyName()
        );

        return supplier;
    }

    @Transactional(readOnly = true)
    public SupplierDetailDto getMyProfile() {
        CurrentUser current = CurrentUser.get();
        if (current == null || current.getSupplierId() == null) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return getSupplierDetail(current.getSupplierId());
    }

    @Transactional
    public SupplierDetailDto updateMyProfile(UpdateMyProfileRequest request) {
        CurrentUser current = CurrentUser.get();
        if (current == null || current.getSupplierId() == null) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        Supplier supplier = supplierRepository.findById(current.getSupplierId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        // Registration number and company name cannot change (BR/API-024)
        supplier.setContactPerson(request.getContactPerson().trim());
        supplier.setEmail(request.getEmail().trim().toLowerCase());
        supplier.setPhone(request.getPhone().trim());
        if (request.getAddress() != null) {
            supplier.setAddress(request.getAddress().trim());
        }

        if (request.getCategoryIds() != null) {
            supplierCategoryRepository.deleteBySupplierId(supplier.getId());
            List<SupplierCategory> mappings = new ArrayList<>();
            for (Short cId : request.getCategoryIds()) {
                mappings.add(SupplierCategory.builder()
                        .supplierId(supplier.getId())
                        .categoryId(cId)
                        .build());
            }
            supplierCategoryRepository.saveAll(mappings);
        }

        supplier = supplierRepository.save(supplier);
        return getSupplierDetail(supplier.getId());
    }

    @Transactional
    public SupplierDocumentDto addOrReplaceDocument(UUID supplierId, MultipartFile file, UUID replacesDocumentId) {
        String storageKey = fileStorageService.storeDocument(file);

        SupplierDocument doc = SupplierDocument.builder()
                .id(UUID.randomUUID())
                .supplierId(supplierId)
                .fileName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .sizeBytes(file.getSize())
                .storageKey(storageKey)
                .reviewStatus(ApprovalStatus.PENDING)
                .replacesDocumentId(replacesDocumentId)
                .uploadedBy(CurrentUser.get() != null ? CurrentUser.get().getId() : null)
                .build();

        doc = documentRepository.save(doc);
        return toDocDto(doc);
    }

    @Transactional(readOnly = true)
    public List<SupplierDocumentDto> getSupplierDocuments(UUID supplierId) {
        CurrentUser current = CurrentUser.get();
        if (current.isSupplier() && !supplierId.equals(current.getSupplierId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        return documentRepository.findBySupplierId(supplierId).stream()
                .map(this::toDocDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource downloadDocument(UUID docId) {
        SupplierDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Document not found"));

        CurrentUser current = CurrentUser.get();
        if (current.isSupplier() && !doc.getSupplierId().equals(current.getSupplierId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        return fileStorageService.loadFileAsResource(doc.getStorageKey());
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

