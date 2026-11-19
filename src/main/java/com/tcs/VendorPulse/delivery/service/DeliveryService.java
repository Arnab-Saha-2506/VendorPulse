package com.tcs.VendorPulse.delivery.service;

import com.tcs.VendorPulse.alert.service.AlertService;
import com.tcs.VendorPulse.audit.service.AuditService;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.enums.DeliveryStatus;
import com.tcs.VendorPulse.common.enums.NotificationType;
import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.event.DomainEvents.DeliveryRecordedEvent;
import com.tcs.VendorPulse.delivery.domain.Delivery;
import com.tcs.VendorPulse.delivery.dto.DeliveryDtos.*;
import com.tcs.VendorPulse.delivery.repository.DeliveryRepository;
import com.tcs.VendorPulse.material.domain.Material;
import com.tcs.VendorPulse.material.repository.MaterialRepository;
import com.tcs.VendorPulse.notification.service.NotificationService;
import com.tcs.VendorPulse.po.domain.PoLine;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PoLineRepository;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.scoring.service.ScoringService;
import com.tcs.VendorPulse.user.domain.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoLineRepository poLineRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;
    private final ScoringService scoringService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public DeliveryOverviewDto getDeliveriesOverview(UUID poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CurrentUser current = CurrentUser.get();
        if (current != null && current.isSupplier() && !po.getSupplierId().equals(current.getSupplierId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        List<PoLine> lines = poLineRepository.findByPoIdOrderByLineNoAsc(poId);
        BigDecimal orderedQty = lines.stream()
                .map(PoLine::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Delivery> deliveries = deliveryRepository.findByPoIdOrderByDeliveryDateAscCreatedAtAsc(poId);
        BigDecimal deliveredQty = deliveries.stream()
                .map(Delivery::getDeliveredQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingQty = orderedQty.subtract(deliveredQty).max(BigDecimal.ZERO);

        Map<UUID, String> userNames = userRepository.findAllById(
                deliveries.stream().map(Delivery::getRecordedBy).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getFullName));

        List<DeliveryDto> deliveryDtos = deliveries.stream().map(d -> DeliveryDto.builder()
                .id(d.getId())
                .poId(d.getPoId())
                .deliveryDate(d.getDeliveryDate())
                .deliveredQty(d.getDeliveredQty())
                .acceptedQty(d.getAcceptedQty())
                .rejectedQty(d.getRejectedQty())
                .completion(d.getCompletion())
                .qualityRating(d.getQualityRating())
                .inspection(d.getInspection())
                .remarks(d.getRemarks())
                .recordedBy(d.getRecordedBy())
                .recordedByName(userNames.getOrDefault(d.getRecordedBy(), "Procurement Manager"))
                .createdAt(d.getCreatedAt())
                .build()).collect(Collectors.toList());

        return DeliveryOverviewDto.builder()
                .poId(po.getId())
                .poNumber(po.getPoNumber())
                .orderedQty(orderedQty)
                .deliveredQty(deliveredQty)
                .remainingQty(remainingQty)
                .deliveries(deliveryDtos)
                .build();
    }

    @Transactional
    public RecordDeliveryResponse recordDelivery(UUID poId, RecordDeliveryRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        // Only CONFIRMED, IN_PROGRESS, or PARTIALLY_DELIVERED orders can receive deliveries
        if (po.getStatus() != PoStatus.CONFIRMED &&
            po.getStatus() != PoStatus.IN_PROGRESS &&
            po.getStatus() != PoStatus.PARTIALLY_DELIVERED) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Deliveries can only be recorded for CONFIRMED, IN_PROGRESS or PARTIALLY_DELIVERED orders");
        }

        List<PoLine> lines = poLineRepository.findByPoIdOrderByLineNoAsc(poId);
        BigDecimal totalOrdered = lines.stream()
                .map(PoLine::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal alreadyDelivered = deliveryRepository.sumDeliveredQtyByPoId(poId);
        BigDecimal remaining = totalOrdered.subtract(alreadyDelivered).max(BigDecimal.ZERO);

        // Validation rule BR-13: delivered quantity cannot exceed remaining
        if (request.getDeliveredQty().compareTo(remaining) > 0) {
            throw new ApiException(ErrorCode.QUANTITY_EXCEEDS_REMAINING,
                    String.format("Delivered quantity (%s) exceeds remaining quantity (%s)",
                            request.getDeliveredQty(), remaining));
        }

        // Accepted + rejected must not exceed delivered
        BigDecimal splitTotal = request.getAcceptedQty().add(request.getRejectedQty());
        if (splitTotal.compareTo(request.getDeliveredQty()) > 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Sum of accepted and rejected quantity cannot exceed delivered quantity");
        }

        CurrentUser current = CurrentUser.get();
        UUID recordedBy = current != null ? current.getId() : po.getCreatedBy();
        String recordedByName = current != null ? current.getFullName() : "Procurement Manager";

        Delivery delivery = Delivery.builder()
                .id(UUID.randomUUID())
                .poId(po.getId())
                .deliveryDate(request.getDeliveryDate())
                .deliveredQty(request.getDeliveredQty())
                .acceptedQty(request.getAcceptedQty())
                .rejectedQty(request.getRejectedQty())
                .completion(request.getCompletion())
                .qualityRating(request.getQualityRating())
                .inspection(request.getInspection())
                .remarks(request.getRemarks())
                .recordedBy(recordedBy)
                .build();

        delivery = deliveryRepository.save(delivery);

        // Update Material Stock
        if (!lines.isEmpty()) {
            UUID primaryMaterialId = lines.get(0).getMaterialId();
            materialRepository.findById(primaryMaterialId).ifPresent(mat -> {
                mat.setStockQty(mat.getStockQty().add(request.getAcceptedQty()));
                mat.setStockUpdatedAt(Instant.now());
                materialRepository.save(mat);
            });
        }

        BigDecimal newTotalDelivered = alreadyDelivered.add(request.getDeliveredQty());
        boolean isFullyDelivered = request.getCompletion() == DeliveryStatus.COMPLETE ||
                                   newTotalDelivered.compareTo(totalOrdered) >= 0;

        PoStatus newPoStatus = isFullyDelivered ? PoStatus.DELIVERED : PoStatus.PARTIALLY_DELIVERED;
        po.setStatus(newPoStatus);
        purchaseOrderRepository.save(po);

        // If completed, recover REVISION and DELAY alerts (BR-24)
        if (isFullyDelivered) {
            alertService.recoverAlertsForPo(po.getId());
        }

        // Calculate defect percent
        BigDecimal defectPercent = BigDecimal.ZERO;
        if (request.getDeliveredQty().compareTo(BigDecimal.ZERO) > 0) {
            defectPercent = request.getRejectedQty()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(request.getDeliveredQty(), 2, RoundingMode.HALF_UP);
        }

        // Notify supplier
        notificationService.notify(
                po.getSupplierId(),
                null,
                NotificationType.APPROVAL,
                String.format("Delivery recorded for PO %s: %s units (Status: %s)",
                        po.getPoNumber(), request.getDeliveredQty(), newPoStatus),
                po.getId()
        );

        auditService.log(
                recordedBy,
                recordedByName,
                "RECORD_DELIVERY",
                "PO",
                po.getId().toString(),
                String.format("Delivered: %s, Accepted: %s, Rating: %s",
                        request.getDeliveredQty(), request.getAcceptedQty(), request.getQualityRating())
        );

        // Trigger score recalculation (BR-22)
        scoringService.recalculateSupplier(po.getSupplierId());

        eventPublisher.publishEvent(DeliveryRecordedEvent.builder()
                .poId(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplierId())
                .deliveryId(delivery.getId())
                .deliveredQty(request.getDeliveredQty())
                .acceptedQty(request.getAcceptedQty())
                .rejectedQty(request.getRejectedQty())
                .qualityRating(request.getQualityRating())
                .newPoStatus(newPoStatus)
                .userId(recordedBy)
                .userName(recordedByName)
                .build());

        DeliveryDto dto = DeliveryDto.builder()
                .id(delivery.getId())
                .poId(delivery.getPoId())
                .deliveryDate(delivery.getDeliveryDate())
                .deliveredQty(delivery.getDeliveredQty())
                .acceptedQty(delivery.getAcceptedQty())
                .rejectedQty(delivery.getRejectedQty())
                .completion(delivery.getCompletion())
                .qualityRating(delivery.getQualityRating())
                .inspection(delivery.getInspection())
                .remarks(delivery.getRemarks())
                .recordedBy(delivery.getRecordedBy())
                .recordedByName(recordedByName)
                .createdAt(delivery.getCreatedAt())
                .build();

        return RecordDeliveryResponse.builder()
                .delivery(dto)
                .defectPercent(defectPercent)
                .build();
    }
}

