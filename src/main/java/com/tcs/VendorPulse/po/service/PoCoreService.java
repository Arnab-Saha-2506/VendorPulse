package com.tcs.VendorPulse.po.service;

import com.tcs.VendorPulse.audit.service.AuditService;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.commitment.repository.PoCommitmentRepository;
import com.tcs.VendorPulse.common.enums.*;
import com.tcs.VendorPulse.common.event.DomainEvents.PoSubmittedEvent;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.delivery.repository.DeliveryRepository;
import com.tcs.VendorPulse.material.domain.Material;
import com.tcs.VendorPulse.material.repository.MaterialRepository;
import com.tcs.VendorPulse.negotiation.domain.NegotiationRound;
import com.tcs.VendorPulse.negotiation.repository.NegotiationRoundRepository;
import com.tcs.VendorPulse.notification.service.NotificationService;
import com.tcs.VendorPulse.po.domain.PoLine;
import com.tcs.VendorPulse.po.domain.PoStatusMachine;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.dto.PoDtos.*;
import com.tcs.VendorPulse.po.repository.PoLineRepository;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.scoring.domain.SupplierScoreHistory;
import com.tcs.VendorPulse.scoring.repository.SupplierScoreHistoryRepository;
import com.tcs.VendorPulse.settings.service.ScoringSettingsService;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.domain.SupplierCategory;
import com.tcs.VendorPulse.supplier.repository.SupplierCategoryRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PoCoreService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoLineRepository poLineRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final SupplierCategoryRepository supplierCategoryRepository;
    private final NegotiationRoundRepository negotiationRoundRepository;
    private final PoCommitmentRepository poCommitmentRepository;
    private final DeliveryRepository deliveryRepository;
    private final SupplierScoreHistoryRepository scoreHistoryRepository;
    private final ScoringSettingsService scoringSettingsService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PurchaseOrder createDraft(CreatePoRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        if (supplier.getApprovalStatus() != ApprovalStatus.APPROVED || !Boolean.TRUE.equals(supplier.getIsActive())) {
            throw new ApiException(ErrorCode.SUPPLIER_NOT_ELIGIBLE, "Supplier is not approved and active");
        }

        // Validate supplier mapping to every material's category (BR-07)
        List<SupplierCategory> mappings = supplierCategoryRepository.findBySupplierId(supplier.getId());
        Set<Short> mappedCategoryIds = mappings.stream().map(SupplierCategory::getCategoryId).collect(Collectors.toSet());

        BigDecimal total = BigDecimal.ZERO;
        List<PoLine> linesToSave = new ArrayList<>();
        short lineNo = 1;

        for (PoLineRequest lr : request.getLines()) {
            Material material = materialRepository.findById(lr.getMaterialId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Material not found: " + lr.getMaterialId()));

            if (!Boolean.TRUE.equals(material.getIsActive())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Material is inactive: " + material.getName());
            }

            if (!mappedCategoryIds.contains(material.getCategoryId())) {
                throw new ApiException(ErrorCode.SUPPLIER_NOT_ELIGIBLE,
                        String.format("Supplier is not mapped to category of material: %s", material.getName()));
            }

            BigDecimal lineTotal = lr.getQuantity().multiply(lr.getUnitPrice());
            total = total.add(lineTotal);

            linesToSave.add(PoLine.builder()
                    .id(UUID.randomUUID())
                    .lineNo(lineNo++)
                    .materialId(material.getId())
                    .quantity(lr.getQuantity())
                    .unitPrice(lr.getUnitPrice())
                    .requiredDate(lr.getRequiredDate())
                    .build());
        }

        Short cutoffDays = request.getCutoffDays() != null ? request.getCutoffDays() :
                scoringSettingsService.getEntitySettings().getDefaultCutoffDays();

        Long nextSeq = purchaseOrderRepository.getNextPoSequence();
        String poNumber = String.format("PO-%d-%04d", Year.now().getValue(), nextSeq);

        CurrentUser current = CurrentUser.get();
        UUID createdBy = current != null ? current.getId() : UUID.randomUUID();

        PurchaseOrder po = PurchaseOrder.builder()
                .id(UUID.randomUUID())
                .poNumber(poNumber)
                .supplierId(supplier.getId())
                .status(PoStatus.DRAFT)
                .requiredDate(request.getRequiredDate())
                .agreedPriceTotal(total)
                .cancellationCutoffDays(cutoffDays)
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        po = purchaseOrderRepository.save(po);

        for (PoLine line : linesToSave) {
            line.setPoId(po.getId());
        }
        poLineRepository.saveAll(linesToSave);

        auditService.log(
                createdBy,
                current != null ? current.getFullName() : "System",
                "CREATE_PO_DRAFT",
                "PO",
                po.getId().toString(),
                po.getPoNumber()
        );

        return po;
    }

    @Transactional
    public PurchaseOrder editDraft(UUID id, CreatePoRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        if (po.getStatus() != PoStatus.DRAFT) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Only DRAFT purchase orders can be edited");
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        if (supplier.getApprovalStatus() != ApprovalStatus.APPROVED || !Boolean.TRUE.equals(supplier.getIsActive())) {
            throw new ApiException(ErrorCode.SUPPLIER_NOT_ELIGIBLE, "Supplier is not approved and active");
        }

        List<SupplierCategory> mappings = supplierCategoryRepository.findBySupplierId(supplier.getId());
        Set<Short> mappedCategoryIds = mappings.stream().map(SupplierCategory::getCategoryId).collect(Collectors.toSet());

        poLineRepository.deleteByPoId(po.getId());

        BigDecimal total = BigDecimal.ZERO;
        List<PoLine> linesToSave = new ArrayList<>();
        short lineNo = 1;

        for (PoLineRequest lr : request.getLines()) {
            Material material = materialRepository.findById(lr.getMaterialId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Material not found: " + lr.getMaterialId()));

            if (!mappedCategoryIds.contains(material.getCategoryId())) {
                throw new ApiException(ErrorCode.SUPPLIER_NOT_ELIGIBLE,
                        String.format("Supplier is not mapped to category of material: %s", material.getName()));
            }

            BigDecimal lineTotal = lr.getQuantity().multiply(lr.getUnitPrice());
            total = total.add(lineTotal);

            linesToSave.add(PoLine.builder()
                    .id(UUID.randomUUID())
                    .poId(po.getId())
                    .lineNo(lineNo++)
                    .materialId(material.getId())
                    .quantity(lr.getQuantity())
                    .unitPrice(lr.getUnitPrice())
                    .requiredDate(lr.getRequiredDate())
                    .build());
        }

        po.setSupplierId(supplier.getId());
        po.setRequiredDate(request.getRequiredDate());
        po.setAgreedPriceTotal(total);
        if (request.getCutoffDays() != null) {
            po.setCancellationCutoffDays(request.getCutoffDays());
        }
        po.setNotes(request.getNotes());

        po = purchaseOrderRepository.save(po);
        poLineRepository.saveAll(linesToSave);

        return po;
    }

    @Transactional
    public void deleteDraft(UUID id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        if (po.getStatus() != PoStatus.DRAFT) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Only DRAFT purchase orders can be deleted");
        }

        poLineRepository.deleteByPoId(po.getId());
        purchaseOrderRepository.delete(po);
    }

    @Transactional
    public PurchaseOrder submitPo(UUID id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        PoStatusMachine.validateTransition(po.getStatus(), PoStatus.SUBMITTED);

        CurrentUser current = CurrentUser.get();
        UUID userId = current != null ? current.getId() : po.getCreatedBy();
        String userName = current != null ? current.getFullName() : "Procurement Manager";

        po.setStatus(PoStatus.SUBMITTED);
        po.setAwaitingParty(PartySide.SUPPLIER);
        po = purchaseOrderRepository.save(po);

        // Create Negotiation Round 1 (PROPOSAL, MANUFACTURER)
        NegotiationRound round1 = NegotiationRound.builder()
                .id(UUID.randomUUID())
                .poId(po.getId())
                .roundNo((short) 1)
                .fromParty(PartySide.MANUFACTURER)
                .action(NegotiationAction.PROPOSAL)
                .proposedDate(po.getRequiredDate())
                .proposedPrice(po.getAgreedPriceTotal())
                .reason("Initial PO Proposal")
                .madeBy(userId)
                .build();
        negotiationRoundRepository.save(round1);

        // Notify Supplier
        notificationService.notify(
                po.getSupplierId(),
                null,
                NotificationType.PROPOSAL,
                "New purchase order proposal: " + po.getPoNumber(),
                po.getId()
        );

        auditService.log(
                userId,
                userName,
                "SUBMIT_PO_PROPOSAL",
                "PO",
                po.getId().toString(),
                po.getPoNumber()
        );

        eventPublisher.publishEvent(PoSubmittedEvent.builder()
                .poId(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplierId())
                .userId(userId)
                .userName(userName)
                .build());

        return po;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PoSummaryDto> searchPos(
            String q, UUID supplierId, PoStatus status, LocalDate from, LocalDate to, Pageable pageable) {

        CurrentUser current = CurrentUser.get();
        final UUID finalSupplierId = (current != null && current.isSupplier()) ? current.getSupplierId() : supplierId;

        Specification<PurchaseOrder> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (finalSupplierId != null) {
                predicates.add(cb.equal(root.get("supplierId"), finalSupplierId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requiredDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("requiredDate"), to));
            }
            if (q != null && !q.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("poNumber")), "%" + q.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PurchaseOrder> page = purchaseOrderRepository.findAll(spec, pageable);

        Map<UUID, String> supplierNames = supplierRepository.findAllById(
                page.getContent().stream().map(PurchaseOrder::getSupplierId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Supplier::getId, Supplier::getCompanyName));

        Page<PoSummaryDto> dtoPage = page.map(po -> {
            Optional<PoCommitment> latest = poCommitmentRepository.findFirstByPoIdOrderBySeqNoDesc(po.getId());
            long revCount = poCommitmentRepository.countByPoIdAndType(po.getId(), CommitmentType.REVISION);

            return PoSummaryDto.builder()
                    .id(po.getId())
                    .poNumber(po.getPoNumber())
                    .supplierId(po.getSupplierId())
                    .supplierName(supplierNames.getOrDefault(po.getSupplierId(), ""))
                    .status(po.getStatus())
                    .requiredDate(po.getRequiredDate())
                    .agreedDeliveryDate(po.getAgreedDeliveryDate())
                    .latestCommittedDate(latest.map(PoCommitment::getCommittedDate).orElse(po.getAgreedDeliveryDate()))
                    .agreedPriceTotal(po.getAgreedPriceTotal())
                    .awaitingParty(po.getAwaitingParty())
                    .revisions(revCount)
                    .createdAt(po.getCreatedAt())
                    .build();
        });

        return PagedResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public PoDetailDto getPoDetail(UUID id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CurrentUser current = CurrentUser.get();
        if (current != null && current.isSupplier() && !po.getSupplierId().equals(current.getSupplierId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        Supplier supplier = supplierRepository.findById(po.getSupplierId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        List<PoLine> lines = poLineRepository.findByPoIdOrderByLineNoAsc(po.getId());
        Map<UUID, Material> materialMap = materialRepository.findAllById(
                lines.stream().map(PoLine::getMaterialId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Material::getId, m -> m));

        List<PoLineDto> lineDtos = lines.stream().map(l -> {
            Material m = materialMap.get(l.getMaterialId());
            return PoLineDto.builder()
                    .id(l.getId())
                    .lineNo(l.getLineNo())
                    .materialId(l.getMaterialId())
                    .materialCode(m != null ? m.getCode() : "")
                    .materialName(m != null ? m.getName() : "")
                    .quantity(l.getQuantity())
                    .unitPrice(l.getUnitPrice())
                    .lineTotal(l.getQuantity().multiply(l.getUnitPrice()))
                    .requiredDate(l.getRequiredDate())
                    .build();
        }).collect(Collectors.toList());

        // Action flags calculations
        LocalDate effectiveDeliveryDate = po.getAgreedDeliveryDate() != null ? po.getAgreedDeliveryDate() : po.getRequiredDate();
        LocalDate cutoffDate = effectiveDeliveryDate.minusDays(po.getCancellationCutoffDays());
        boolean todayBeforeCutoff = !LocalDate.now().isAfter(cutoffDate);
        BigDecimal sumDelivered = deliveryRepository.sumDeliveredQtyByPoId(po.getId());
        boolean noDeliveries = sumDelivered.compareTo(BigDecimal.ZERO) == 0;
        boolean notDispatched = po.getDispatchedOn() == null;

        boolean canCancel = (current == null || current.isProcurement()) &&
                (po.getStatus() == PoStatus.DRAFT || po.getStatus() == PoStatus.SUBMITTED || po.getStatus() == PoStatus.CONFIRMED) &&
                todayBeforeCutoff && notDispatched && noDeliveries;

        boolean canRevise = (current == null || current.isSupplier()) &&
                (po.getStatus() == PoStatus.CONFIRMED || po.getStatus() == PoStatus.IN_PROGRESS || po.getStatus() == PoStatus.PARTIALLY_DELIVERED);

        boolean canRecordDelivery = (current == null || current.isProcurement()) &&
                (po.getStatus() == PoStatus.CONFIRMED || po.getStatus() == PoStatus.IN_PROGRESS || po.getStatus() == PoStatus.PARTIALLY_DELIVERED);

        return PoDetailDto.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(supplier.getId())
                .supplierName(supplier.getCompanyName())
                .supplierContact(supplier.getContactPerson())
                .supplierEmail(supplier.getEmail())
                .supplierPhone(supplier.getPhone())
                .status(po.getStatus())
                .requiredDate(po.getRequiredDate())
                .agreedPriceTotal(po.getAgreedPriceTotal())
                .agreedDeliveryDate(po.getAgreedDeliveryDate())
                .cancellationCutoffDays(po.getCancellationCutoffDays())
                .awaitingParty(po.getAwaitingParty())
                .notes(po.getNotes())
                .dispatchedOn(po.getDispatchedOn())
                .supplierMarkedDeliveredOn(po.getSupplierMarkedDeliveredOn())
                .cancelReason(po.getCancelReason())
                .cancelledBy(po.getCancelledBy())
                .cancelledAt(po.getCancelledAt())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .lines(lineDtos)
                .canCancel(canCancel)
                .canRevise(canRevise)
                .canRecordDelivery(canRecordDelivery)
                .build();
    }

    @Transactional(readOnly = true)
    public List<EligibleSupplierDto> getEligibleSuppliers(List<UUID> materialIds, Short categoryId) {
        Set<Short> requiredCategoryIds = new HashSet<>();
        if (categoryId != null) {
            requiredCategoryIds.add(categoryId);
        }
        if (materialIds != null && !materialIds.isEmpty()) {
            List<Material> materials = materialRepository.findAllById(materialIds);
            for (Material m : materials) {
                requiredCategoryIds.add(m.getCategoryId());
            }
        }

        List<Supplier> approvedActive = supplierRepository.findAll().stream()
                .filter(s -> s.getApprovalStatus() == ApprovalStatus.APPROVED && Boolean.TRUE.equals(s.getIsActive()))
                .collect(Collectors.toList());

        List<EligibleSupplierDto> result = new ArrayList<>();
        for (Supplier s : approvedActive) {
            Set<Short> supplierCats = supplierCategoryRepository.findBySupplierId(s.getId()).stream()
                    .map(SupplierCategory::getCategoryId).collect(Collectors.toSet());

            if (supplierCats.containsAll(requiredCategoryIds)) {
                Optional<SupplierScoreHistory> sh = scoreHistoryRepository.findFirstBySupplierIdOrderByPeriodDesc(s.getId());
                result.add(EligibleSupplierDto.builder()
                        .id(s.getId())
                        .companyName(s.getCompanyName())
                        .contactPerson(s.getContactPerson())
                        .email(s.getEmail())
                        .overallScore(sh.map(SupplierScoreHistory::getOverallScore).orElse(null))
                        .cdi(sh.map(SupplierScoreHistory::getCdi).orElse(null))
                        .isRecommended(false)
                        .build());
            }
        }

        // Mark best scored as recommended
        result.stream()
                .filter(dto -> dto.getOverallScore() != null)
                .max(Comparator.comparing(EligibleSupplierDto::getOverallScore))
                .ifPresent(best -> best.setRecommended(true));

        return result;
    }
}

