package com.tcs.VendorPulse.commitment.service;

import com.tcs.VendorPulse.alert.service.AlertService;
import com.tcs.VendorPulse.audit.service.AuditService;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.commitment.dto.CommitmentDtos.*;
import com.tcs.VendorPulse.commitment.repository.PoCommitmentRepository;
import com.tcs.VendorPulse.common.enums.AlertType;
import com.tcs.VendorPulse.common.enums.CommitmentType;
import com.tcs.VendorPulse.common.enums.NotificationType;
import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.event.DomainEvents.PoDispatchedEvent;
import com.tcs.VendorPulse.common.event.DomainEvents.PoRevisedEvent;
import com.tcs.VendorPulse.delivery.domain.Delivery;
import com.tcs.VendorPulse.delivery.repository.DeliveryRepository;
import com.tcs.VendorPulse.notification.service.NotificationService;
import com.tcs.VendorPulse.po.domain.PoStatusMachine;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.scoring.service.ScoringService;
import com.tcs.VendorPulse.user.domain.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommitmentService {

    private final PoCommitmentRepository commitmentRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;
    private final ScoringService scoringService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public CommitmentTimelineDto getCommitmentTimeline(UUID poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CurrentUser current = CurrentUser.get();
        if (current != null && current.isSupplier() && !po.getSupplierId().equals(current.getSupplierId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        List<PoCommitment> commitments = commitmentRepository.findByPoIdOrderBySeqNoAsc(poId);
        Map<UUID, String> userNames = userRepository.findAllById(
                commitments.stream().map(PoCommitment::getCreatedBy).filter(Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getFullName));

        LocalDate originalDate = commitments.stream()
                .filter(c -> c.getSeqNo() == 0)
                .map(PoCommitment::getCommittedDate)
                .findFirst()
                .orElse(po.getRequiredDate());

        LocalDate latestDate = commitments.stream()
                .max(Comparator.comparing(PoCommitment::getSeqNo))
                .map(PoCommitment::getCommittedDate)
                .orElse(po.getAgreedDeliveryDate() != null ? po.getAgreedDeliveryDate() : po.getRequiredDate());

        List<Delivery> deliveries = deliveryRepository.findByPoIdOrderByDeliveryDateAscCreatedAtAsc(poId);
        LocalDate actualDelivery = deliveries.stream()
                .map(Delivery::getDeliveryDate)
                .max(LocalDate::compareTo)
                .orElse(po.getSupplierMarkedDeliveredOn());

        List<CommitmentRevisionDto> revisionDtos = commitments.stream()
                .map(c -> CommitmentRevisionDto.builder()
                        .seq(c.getSeqNo())
                        .type(c.getType())
                        .date(c.getCommittedDate())
                        .reason(c.getReason())
                        .user(c.getCreatedBy())
                        .userName(userNames.getOrDefault(c.getCreatedBy(), "Supplier"))
                        .time(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return CommitmentTimelineDto.builder()
                .poId(po.getId())
                .poNumber(po.getPoNumber())
                .originalDate(originalDate)
                .latestCommittedDate(latestDate)
                .actualDeliveryDate(actualDelivery)
                .revisions(revisionDtos)
                .build();
    }

    @Transactional
    public PoCommitment reviseDeliveryDate(UUID poId, ReviseCommitmentRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CurrentUser current = CurrentUser.get();
        if (current == null || (current.isSupplier() && !po.getSupplierId().equals(current.getSupplierId()))) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        boolean validStatus = po.getStatus() == PoStatus.CONFIRMED ||
                              po.getStatus() == PoStatus.IN_PROGRESS ||
                              po.getStatus() == PoStatus.PARTIALLY_DELIVERED;

        if (!validStatus) {
            throw new ApiException(ErrorCode.REVISION_NOT_ALLOWED,
                    "Revisions only allowed for CONFIRMED, IN_PROGRESS or PARTIALLY_DELIVERED orders");
        }

        if (request.getNewDate().isBefore(LocalDate.now())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "New delivery date must be in the future");
        }

        List<PoCommitment> commitments = commitmentRepository.findByPoIdOrderBySeqNoAsc(poId);
        short lastSeq = (short) (commitments.isEmpty() ? 0 : commitments.get(commitments.size() - 1).getSeqNo());
        LocalDate latestDate = commitments.isEmpty() ? po.getRequiredDate() :
                commitments.get(commitments.size() - 1).getCommittedDate();

        if (request.getNewDate().equals(latestDate)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "New delivery date must differ from the latest committed date");
        }

        short nextSeq = (short) (lastSeq + 1);

        PoCommitment revision = PoCommitment.builder()
                .id(UUID.randomUUID())
                .poId(po.getId())
                .seqNo(nextSeq)
                .type(CommitmentType.REVISION)
                .committedDate(request.getNewDate())
                .reason(request.getReason().trim())
                .createdBy(current.getId())
                .build();

        revision = commitmentRepository.save(revision);

        // Raise REVISION alert (BR-24)
        alertService.raiseAlert(
                AlertType.REVISION,
                String.format("Delivery date revised for PO %s from %s to %s. Reason: %s",
                        po.getPoNumber(), latestDate, request.getNewDate(), request.getReason()),
                po.getId(), null, po.getSupplierId()
        );

        // Notify procurement manager
        notificationService.notify(
                null,
                po.getCreatedBy(),
                NotificationType.REMINDER,
                String.format("Supplier revised delivery date for PO %s to %s", po.getPoNumber(), request.getNewDate()),
                po.getId()
        );

        auditService.log(
                current.getId(),
                current.getFullName(),
                "REVISE_PO_COMMITMENT",
                "PO",
                po.getId().toString(),
                String.format("Seq %d: %s -> %s (%s)", nextSeq, latestDate, request.getNewDate(), request.getReason())
        );

        scoringService.recalculateSupplier(po.getSupplierId());

        eventPublisher.publishEvent(PoRevisedEvent.builder()
                .poId(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplierId())
                .seqNo(nextSeq)
                .oldDate(latestDate)
                .newDate(request.getNewDate())
                .reason(request.getReason())
                .userId(current.getId())
                .userName(current.getFullName())
                .build());

        return revision;
    }

    @Transactional
    public PurchaseOrder updateStatus(UUID poId, StatusUpdateRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CurrentUser current = CurrentUser.get();
        if (current == null || (current.isSupplier() && !po.getSupplierId().equals(current.getSupplierId()))) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        if (request.getDate().isAfter(LocalDate.now())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Date cannot be in the future");
        }

        String action = request.getStatus().trim().toUpperCase();

        if ("DISPATCHED".equals(action)) {
            PoStatusMachine.validateTransition(po.getStatus(), PoStatus.IN_PROGRESS);
            po.setDispatchedOn(request.getDate());
            po.setStatus(PoStatus.IN_PROGRESS);
            po = purchaseOrderRepository.save(po);

            auditService.log(current.getId(), current.getFullName(), "DISPATCH_PO", "PO", po.getId().toString(), request.getDate().toString());
            notificationService.notify(null, po.getCreatedBy(), NotificationType.MESSAGE, "PO " + po.getPoNumber() + " has been dispatched", po.getId());

            eventPublisher.publishEvent(PoDispatchedEvent.builder()
                    .poId(po.getId())
                    .poNumber(po.getPoNumber())
                    .supplierId(po.getSupplierId())
                    .dispatchedOn(request.getDate())
                    .userId(current.getId())
                    .userName(current.getFullName())
                    .build());

        } else if ("DELIVERED".equals(action)) {
            if (po.getStatus() != PoStatus.IN_PROGRESS && po.getStatus() != PoStatus.PARTIALLY_DELIVERED) {
                throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION,
                        "Order must be IN_PROGRESS or PARTIALLY_DELIVERED to mark delivered by supplier");
            }
            po.setSupplierMarkedDeliveredOn(request.getDate());
            po = purchaseOrderRepository.save(po);

            auditService.log(current.getId(), current.getFullName(), "SUPPLIER_MARKED_DELIVERED", "PO", po.getId().toString(), request.getDate().toString());
            notificationService.notify(null, po.getCreatedBy(), NotificationType.MESSAGE, "Supplier marked PO " + po.getPoNumber() + " as delivered", po.getId());
        } else {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Status must be DISPATCHED or DELIVERED");
        }

        return po;
    }
}

