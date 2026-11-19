package com.tcs.VendorPulse.po.service;

import com.tcs.VendorPulse.audit.service.AuditService;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.enums.NotificationType;
import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.event.DomainEvents.PoCancelledEvent;
import com.tcs.VendorPulse.delivery.repository.DeliveryRepository;
import com.tcs.VendorPulse.notification.service.NotificationService;
import com.tcs.VendorPulse.po.domain.PoStatusMachine;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.dto.PoDtos.CancellationWindowDto;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PoCancelService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public CancellationWindowDto getCancellationWindow(UUID poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        LocalDate effectiveDeliveryDate = po.getAgreedDeliveryDate() != null ? po.getAgreedDeliveryDate() : po.getRequiredDate();
        LocalDate cutoffDate = effectiveDeliveryDate.minusDays(po.getCancellationCutoffDays());

        boolean validStatus = po.getStatus() == PoStatus.DRAFT ||
                              po.getStatus() == PoStatus.SUBMITTED ||
                              po.getStatus() == PoStatus.CONFIRMED;

        boolean beforeCutoff = !LocalDate.now().isAfter(cutoffDate);
        boolean notDispatched = po.getDispatchedOn() == null;
        BigDecimal delivered = deliveryRepository.sumDeliveredQtyByPoId(po.getId());
        boolean noDeliveries = delivered.compareTo(BigDecimal.ZERO) == 0;

        boolean allowed = validStatus && beforeCutoff && notDispatched && noDeliveries;
        String blockedReason = null;

        if (!validStatus) {
            blockedReason = "Cancellation not allowed for current status: " + po.getStatus();
        } else if (!beforeCutoff) {
            blockedReason = "Cancellation cutoff date passed (" + cutoffDate + ")";
        } else if (!notDispatched) {
            blockedReason = "Order has already been dispatched";
        } else if (!noDeliveries) {
            blockedReason = "Deliveries have already been recorded for this order";
        }

        return CancellationWindowDto.builder()
                .cutoffDate(cutoffDate)
                .allowed(allowed)
                .blockedReason(blockedReason)
                .build();
    }

    @Transactional
    public PurchaseOrder cancelPo(UUID poId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Cancellation reason is required");
        }

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CancellationWindowDto window = getCancellationWindow(poId);
        if (!window.isAllowed()) {
            throw new ApiException(ErrorCode.CANCELLATION_NOT_ALLOWED, window.getBlockedReason());
        }

        PoStatusMachine.validateTransition(po.getStatus(), PoStatus.CANCELLED);

        CurrentUser current = CurrentUser.get();
        UUID userId = current != null ? current.getId() : po.getCreatedBy();
        String userName = current != null ? current.getFullName() : "Procurement Manager";

        po.setStatus(PoStatus.CANCELLED);
        po.setCancelReason(reason.trim());
        po.setCancelledBy(userId);
        po.setCancelledAt(Instant.now());
        po.setAwaitingParty(null);

        po = purchaseOrderRepository.save(po);

        // Notify Supplier
        notificationService.notify(
                po.getSupplierId(),
                null,
                NotificationType.CANCELLED,
                String.format("Purchase order %s has been cancelled: %s", po.getPoNumber(), reason),
                po.getId()
        );

        auditService.log(
                userId,
                userName,
                "CANCEL_PURCHASE_ORDER",
                "PO",
                po.getId().toString(),
                reason
        );

        eventPublisher.publishEvent(PoCancelledEvent.builder()
                .poId(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplierId())
                .cancelReason(reason)
                .userId(userId)
                .userName(userName)
                .build());

        return po;
    }
}

