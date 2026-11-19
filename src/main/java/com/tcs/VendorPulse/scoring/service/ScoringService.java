package com.tcs.VendorPulse.scoring.service;

import com.tcs.VendorPulse.alert.service.AlertService;
import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.commitment.repository.PoCommitmentRepository;
import com.tcs.VendorPulse.common.enums.AlertType;
import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.delivery.domain.Delivery;
import com.tcs.VendorPulse.delivery.repository.DeliveryRepository;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.scoring.domain.SupplierScoreHistory;
import com.tcs.VendorPulse.scoring.dto.ScoreDtos.*;
import com.tcs.VendorPulse.scoring.engine.ScoringEngine;
import com.tcs.VendorPulse.scoring.repository.SupplierScoreHistoryRepository;
import com.tcs.VendorPulse.settings.domain.ScoringSettings;
import com.tcs.VendorPulse.settings.service.ScoringSettingsService;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final SupplierScoreHistoryRepository scoreHistoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoCommitmentRepository commitmentRepository;
    private final DeliveryRepository deliveryRepository;
    private final SupplierRepository supplierRepository;
    private final ScoringSettingsService settingsService;
    private final AlertService alertService;

    @Transactional
    public void recalculateSupplier(UUID supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        if (supplier == null) return;

        List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierId(supplierId).stream()
                .filter(p -> p.getStatus() == PoStatus.DELIVERED)
                .collect(Collectors.toList());

        Map<UUID, List<PoCommitment>> commitments = new HashMap<>();
        Map<UUID, List<Delivery>> deliveries = new HashMap<>();

        for (PurchaseOrder po : pos) {
            commitments.put(po.getId(), commitmentRepository.findByPoIdOrderBySeqNoAsc(po.getId()));
            deliveries.put(po.getId(), deliveryRepository.findByPoIdOrderByDeliveryDateAscCreatedAtAsc(po.getId()));
        }

        ScoringSettings settings = settingsService.getEntitySettings();

        ScoringEngine.ScoreResult result = ScoringEngine.computeScores(
                pos, commitments, deliveries,
                settings.getWDeliveryReliability(),
                settings.getWCommitmentStability(),
                settings.getWDeliveryConsistency(),
                settings.getWQualityPerformance()
        );

        LocalDate period = LocalDate.now().withDayOfMonth(1);
        SupplierScoreHistory history = scoreHistoryRepository.findBySupplierIdAndPeriod(supplierId, period)
                .orElseGet(() -> SupplierScoreHistory.builder()
                        .id(UUID.randomUUID())
                        .supplierId(supplierId)
                        .period(period)
                        .build());

        history.setOverallScore(result.overall);
        history.setDeliveryReliability(result.drs);
        history.setCommitmentStability(result.css);
        history.setDeliveryConsistency(result.dcs);
        history.setQualityPerformance(result.qps);
        history.setCdi(result.cdi);
        history.setOrdersCounted(result.ordersCounted);

        scoreHistoryRepository.save(history);

        // Check High CDI Alert threshold (BR-21, BR-24)
        if (result.isRated && result.cdi.compareTo(BigDecimal.valueOf(settings.getHighCdiThreshold())) >= 0) {
            alertService.raiseAlert(
                    AlertType.CDI,
                    String.format("Supplier %s has high Commitment Drift Index (CDI: %.1f%%, threshold: %d%%)",
                            supplier.getCompanyName(), result.cdi.doubleValue(), settings.getHighCdiThreshold()),
                    null, null, supplier.getId()
            );
        } else {
            alertService.recoverAlertsForSupplier(supplier.getId(), AlertType.CDI);
        }
    }

    @Transactional
    public RecalculateResponse recalculateAll(UUID optionalSupplierId) {
        if (optionalSupplierId != null) {
            recalculateSupplier(optionalSupplierId);
            return RecalculateResponse.builder()
                    .count(1)
                    .snapshotPeriod(LocalDate.now().withDayOfMonth(1))
                    .build();
        }

        List<Supplier> suppliers = supplierRepository.findAll();
        for (Supplier s : suppliers) {
            recalculateSupplier(s.getId());
        }

        return RecalculateResponse.builder()
                .count(suppliers.size())
                .snapshotPeriod(LocalDate.now().withDayOfMonth(1))
                .build();
    }

    @Transactional(readOnly = true)
    public ScorecardDto getScorecard(UUID supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Supplier not found"));

        Optional<SupplierScoreHistory> latest = scoreHistoryRepository.findFirstBySupplierIdOrderByPeriodDesc(supplierId);
        List<SupplierScoreHistory> histories = scoreHistoryRepository.findBySupplierIdOrderByPeriodAsc(supplierId);

        List<MonthlyScoreDto> historyDtos = histories.stream().map(h -> MonthlyScoreDto.builder()
                .period(h.getPeriod())
                .overallScore(h.getOverallScore())
                .deliveryReliability(h.getDeliveryReliability())
                .commitmentStability(h.getCommitmentStability())
                .deliveryConsistency(h.getDeliveryConsistency())
                .qualityPerformance(h.getQualityPerformance())
                .cdi(h.getCdi())
                .ordersCounted(h.getOrdersCounted())
                .build()).collect(Collectors.toList());

        var builder = ScorecardDto.builder()
                .supplierId(supplier.getId())
                .supplierName(supplier.getCompanyName())
                .history(historyDtos);

        if (latest.isPresent() && latest.get().getOrdersCounted() >= 3) {
            SupplierScoreHistory sh = latest.get();
            builder.overallScore(sh.getOverallScore())
                    .deliveryReliability(sh.getDeliveryReliability())
                    .commitmentStability(sh.getCommitmentStability())
                    .deliveryConsistency(sh.getDeliveryConsistency())
                    .qualityPerformance(sh.getQualityPerformance())
                    .cdi(sh.getCdi())
                    .ordersCounted(sh.getOrdersCounted())
                    .isRated(true)
                    .ratingLabel(String.format("%.1f", sh.getOverallScore()));
        } else {
            builder.isRated(false)
                    .ordersCounted(latest.map(SupplierScoreHistory::getOrdersCounted).orElse(0))
                    .ratingLabel("Not rated");
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public SupplierPerformanceHistoryDto getPerformanceHistory(UUID supplierId, LocalDate from, LocalDate to) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Supplier not found");
        }

        List<Delivery> deliveries = deliveryRepository.findAllBySupplierId(supplierId);
        if (from != null) {
            deliveries = deliveries.stream().filter(d -> !d.getDeliveryDate().isBefore(from)).collect(Collectors.toList());
        }
        if (to != null) {
            deliveries = deliveries.stream().filter(d -> !d.getDeliveryDate().isAfter(to)).collect(Collectors.toList());
        }

        BigDecimal totalDelivered = BigDecimal.ZERO;
        BigDecimal totalAccepted = BigDecimal.ZERO;
        BigDecimal totalRejected = BigDecimal.ZERO;
        double ratingSum = 0;
        int ratingCount = 0;

        for (Delivery d : deliveries) {
            totalDelivered = totalDelivered.add(d.getDeliveredQty());
            totalAccepted = totalAccepted.add(d.getAcceptedQty());
            totalRejected = totalRejected.add(d.getRejectedQty());
            if (d.getQualityRating() != null) {
                ratingSum += d.getQualityRating();
                ratingCount++;
            }
        }

        DeliveryStatDto statDto = DeliveryStatDto.builder()
                .totalDeliveries(deliveries.size())
                .totalDeliveredQty(totalDelivered)
                .totalAcceptedQty(totalAccepted)
                .totalRejectedQty(totalRejected)
                .averageRating(ratingCount > 0 ? ratingSum / ratingCount : 0.0)
                .build();

        List<SupplierScoreHistory> histories = scoreHistoryRepository.findBySupplierIdOrderByPeriodAsc(supplierId);
        List<MonthlyScoreDto> monthlyList = histories.stream()
                .map(h -> MonthlyScoreDto.builder()
                        .period(h.getPeriod())
                        .overallScore(h.getOverallScore())
                        .deliveryReliability(h.getDeliveryReliability())
                        .commitmentStability(h.getCommitmentStability())
                        .deliveryConsistency(h.getDeliveryConsistency())
                        .qualityPerformance(h.getQualityPerformance())
                        .cdi(h.getCdi())
                        .ordersCounted(h.getOrdersCounted())
                        .build())
                .collect(Collectors.toList());

        return SupplierPerformanceHistoryDto.builder()
                .supplierId(supplierId)
                .deliveryStats(statDto)
                .monthlyTrend(monthlyList)
                .build();
    }
}

