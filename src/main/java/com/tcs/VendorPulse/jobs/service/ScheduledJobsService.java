package com.tcs.VendorPulse.jobs.service;

import com.tcs.VendorPulse.alert.repository.AlertRepository;
import com.tcs.VendorPulse.alert.service.AlertService;
import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.commitment.repository.PoCommitmentRepository;
import com.tcs.VendorPulse.common.enums.AlertStatus;
import com.tcs.VendorPulse.common.enums.AlertType;
import com.tcs.VendorPulse.common.enums.NotificationType;
import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.inventory.service.InventoryService;
import com.tcs.VendorPulse.notification.service.NotificationService;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.scoring.service.ScoringService;
import com.tcs.VendorPulse.settings.service.ScoringSettingsService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledJobsService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoCommitmentRepository commitmentRepository;
    private final AlertService alertService;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;
    private final ScoringService scoringService;
    private final ScoringSettingsService settingsService;

    @Data
    @Builder
    public static class RunJobsResponse {
        private int jobsRun;
        private long alertsRaised;
        private String message;
    }

    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1:00 AM
    @Transactional
    public void dailyDelayCheck() {
        log.info("Running daily delay check job...");
        List<PurchaseOrder> openPos = purchaseOrderRepository.findAll().stream()
                .filter(p -> p.getStatus() == PoStatus.CONFIRMED || p.getStatus() == PoStatus.IN_PROGRESS)
                .toList();

        LocalDate today = LocalDate.now();

        for (PurchaseOrder po : openPos) {
            Optional<PoCommitment> latest = commitmentRepository.findFirstByPoIdOrderBySeqNoDesc(po.getId());
            LocalDate committedDate = latest.map(PoCommitment::getCommittedDate)
                    .orElse(po.getAgreedDeliveryDate() != null ? po.getAgreedDeliveryDate() : po.getRequiredDate());

            long daysUntilDelivery = ChronoUnit.DAYS.between(today, committedDate);

            // If within 3 days and not dispatched, or past committed date and not delivered
            if (daysUntilDelivery < 0) {
                alertService.raiseAlert(
                        AlertType.DELAY,
                        String.format("Order %s is OVERDUE by %d days (committed date: %s)",
                                po.getPoNumber(), Math.abs(daysUntilDelivery), committedDate),
                        po.getId(), null, po.getSupplierId()
                );
            } else if (daysUntilDelivery <= 3 && po.getDispatchedOn() == null) {
                alertService.raiseAlert(
                        AlertType.DELAY,
                        String.format("Order %s delivery is due in %d days (%s) and has not been dispatched",
                                po.getPoNumber(), daysUntilDelivery, committedDate),
                        po.getId(), null, po.getSupplierId()
                );
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2:00 AM
    @Transactional
    public void dailyReminderCheck() {
        log.info("Running daily reminder check job...");
        List<PurchaseOrder> confirmedPos = purchaseOrderRepository.findAll().stream()
                .filter(p -> p.getStatus() == PoStatus.CONFIRMED && p.getDispatchedOn() == null)
                .toList();

        LocalDate today = LocalDate.now();

        for (PurchaseOrder po : confirmedPos) {
            Optional<PoCommitment> latest = commitmentRepository.findFirstByPoIdOrderBySeqNoDesc(po.getId());
            LocalDate committedDate = latest.map(PoCommitment::getCommittedDate)
                    .orElse(po.getAgreedDeliveryDate() != null ? po.getAgreedDeliveryDate() : po.getRequiredDate());

            long daysUntil = ChronoUnit.DAYS.between(today, committedDate);
            if (daysUntil == 3) {
                notificationService.notify(
                        po.getSupplierId(),
                        null,
                        NotificationType.REMINDER,
                        String.format("Reminder: Delivery for PO %s is scheduled in 3 days on %s",
                                po.getPoNumber(), committedDate),
                        po.getId()
                );
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3:00 AM
    public void stockRiskEvaluation() {
        log.info("Running stock risk evaluation job...");
        inventoryService.evaluateStockRiskAlerts();
    }

    @Scheduled(cron = "0 0 4 1 * ?") // Monthly on the 1st
    public void monthlyScoreSnapshot() {
        log.info("Running monthly score snapshot job...");
        scoringService.recalculateAll(null);
    }

    @Transactional
    public RunJobsResponse runAllJobsNow() {
        long alertsBefore = alertRepository.countByStatus(AlertStatus.ACTIVE);

        dailyDelayCheck();
        dailyReminderCheck();
        stockRiskEvaluation();
        monthlyScoreSnapshot();

        long alertsAfter = alertRepository.countByStatus(AlertStatus.ACTIVE);
        long newAlerts = Math.max(0, alertsAfter - alertsBefore);

        return RunJobsResponse.builder()
                .jobsRun(5)
                .alertsRaised(newAlerts)
                .message("All background jobs (Delay Check, Reminders, Stock Risk, Scoring, CDI) executed successfully.")
                .build();
    }
}

