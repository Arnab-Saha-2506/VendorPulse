package com.tcs.VendorPulse.common.event;

import com.tcs.VendorPulse.audit.service.AuditService;
import com.tcs.VendorPulse.common.event.DomainEvents.*;
import com.tcs.VendorPulse.notification.service.NotificationService;
import com.tcs.VendorPulse.scoring.service.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventListeners {

    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ScoringService scoringService;

    @EventListener
    public void onPoSubmitted(PoSubmittedEvent event) {
        log.info("Domain Event handled: PoSubmittedEvent for PO {}", event.getPoNumber());
    }

    @EventListener
    public void onPoCountered(PoCounteredEvent event) {
        log.info("Domain Event handled: PoCounteredEvent for PO {}, round {}", event.getPoNumber(), event.getRoundNo());
    }

    @EventListener
    public void onPoAgreed(PoAgreedEvent event) {
        log.info("Domain Event handled: PoAgreedEvent for PO {}", event.getPoNumber());
    }

    @EventListener
    public void onPoRevised(PoRevisedEvent event) {
        log.info("Domain Event handled: PoRevisedEvent for PO {}", event.getPoNumber());
    }

    @EventListener
    public void onPoDispatched(PoDispatchedEvent event) {
        log.info("Domain Event handled: PoDispatchedEvent for PO {}", event.getPoNumber());
    }

    @EventListener
    public void onPoCancelled(PoCancelledEvent event) {
        log.info("Domain Event handled: PoCancelledEvent for PO {}", event.getPoNumber());
        scoringService.recalculateSupplier(event.getSupplierId());
    }

    @EventListener
    public void onDeliveryRecorded(DeliveryRecordedEvent event) {
        log.info("Domain Event handled: DeliveryRecordedEvent for PO {}", event.getPoNumber());
    }
}

