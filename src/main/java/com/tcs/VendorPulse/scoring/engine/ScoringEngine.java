package com.tcs.VendorPulse.scoring.engine;

import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.common.enums.CommitmentType;
import com.tcs.VendorPulse.delivery.domain.Delivery;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ScoringEngine {

    public static class ScoreResult {
        public BigDecimal drs;
        public BigDecimal css;
        public BigDecimal dcs;
        public BigDecimal qps;
        public BigDecimal overall;
        public BigDecimal cdi;
        public int ordersCounted;
        public boolean isRated;
    }

    public static ScoreResult computeScores(
            List<PurchaseOrder> deliveredPos,
            Map<UUID, List<PoCommitment>> commitmentsByPo,
            Map<UUID, List<Delivery>> deliveriesByPo,
            short wDrs, short wCss, short wDcs, short wQps) {

        ScoreResult result = new ScoreResult();
        result.ordersCounted = deliveredPos.size();

        if (deliveredPos.size() < 3) {
            result.isRated = false;
            result.drs = BigDecimal.ZERO;
            result.css = BigDecimal.ZERO;
            result.dcs = BigDecimal.ZERO;
            result.qps = BigDecimal.ZERO;
            result.overall = BigDecimal.ZERO;
            result.cdi = BigDecimal.ZERO;
            return result;
        }

        result.isRated = true;

        // 1. DRS (Delivery Reliability Score) - BR-16
        // Delivered POs finished on or before the latest committed date / all delivered POs x 100
        int onTimeCount = 0;
        List<Double> delayDaysList = new ArrayList<>();
        double totalRevisions = 0;
        double totalDriftPct = 0;

        for (PurchaseOrder po : deliveredPos) {
            List<PoCommitment> commitments = commitmentsByPo.getOrDefault(po.getId(), Collections.emptyList());
            List<Delivery> deliveries = deliveriesByPo.getOrDefault(po.getId(), Collections.emptyList());

            LocalDate originalDate = commitments.stream()
                    .filter(c -> c.getSeqNo() == 0)
                    .map(PoCommitment::getCommittedDate)
                    .findFirst()
                    .orElse(po.getRequiredDate());

            LocalDate latestDate = commitments.stream()
                    .max(Comparator.comparing(PoCommitment::getSeqNo))
                    .map(PoCommitment::getCommittedDate)
                    .orElse(po.getAgreedDeliveryDate() != null ? po.getAgreedDeliveryDate() : po.getRequiredDate());

            long revisions = commitments.stream().filter(c -> c.getType() == CommitmentType.REVISION).count();
            totalRevisions += revisions;

            LocalDate finalDeliveryDate = deliveries.stream()
                    .map(Delivery::getDeliveryDate)
                    .max(LocalDate::compareTo)
                    .orElse(po.getSupplierMarkedDeliveredOn() != null ? po.getSupplierMarkedDeliveredOn() : latestDate);

            if (!finalDeliveryDate.isAfter(latestDate)) {
                onTimeCount++;
            }

            long delay = Math.max(0, ChronoUnit.DAYS.between(latestDate, finalDeliveryDate));
            delayDaysList.add((double) delay);

            LocalDate poCreatedDate = po.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            long leadTime = Math.max(1, ChronoUnit.DAYS.between(poCreatedDate, originalDate));
            long driftDays = Math.max(0, ChronoUnit.DAYS.between(originalDate, latestDate));
            double driftPct = Math.min(100.0, ((double) driftDays / leadTime) * 100.0);
            totalDriftPct += driftPct;
        }

        double drsVal = ((double) onTimeCount / deliveredPos.size()) * 100.0;
        result.drs = BigDecimal.valueOf(drsVal).setScale(2, RoundingMode.HALF_UP);

        // 2. CSS (Commitment Stability Score) - BR-17
        // max(0, 100 - 25 x average number of revisions per PO)
        double avgRevisions = totalRevisions / deliveredPos.size();
        double cssVal = Math.max(0.0, 100.0 - (25.0 * avgRevisions));
        result.css = BigDecimal.valueOf(cssVal).setScale(2, RoundingMode.HALF_UP);

        // 3. DCS (Delivery Consistency Score) - BR-18
        // max(0, 100 - 10 x standard deviation of delay days)
        double meanDelay = delayDaysList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = delayDaysList.stream().mapToDouble(d -> Math.pow(d - meanDelay, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        double dcsVal = Math.max(0.0, 100.0 - (10.0 * stdDev));
        result.dcs = BigDecimal.valueOf(dcsVal).setScale(2, RoundingMode.HALF_UP);

        // 4. QPS (Quality Performance Score) - BR-19
        // 0.6 x acceptance rate (accepted / delivered x 100) + 0.4 x (average rating / 5 x 100)
        double totalDeliveredQty = 0;
        double totalAcceptedQty = 0;
        double totalRating = 0;
        int ratingCount = 0;

        for (List<Delivery> delList : deliveriesByPo.values()) {
            for (Delivery d : delList) {
                totalDeliveredQty += d.getDeliveredQty().doubleValue();
                totalAcceptedQty += d.getAcceptedQty().doubleValue();
                if (d.getQualityRating() != null) {
                    totalRating += d.getQualityRating();
                    ratingCount++;
                }
            }
        }

        double acceptanceRate = totalDeliveredQty > 0 ? (totalAcceptedQty / totalDeliveredQty) * 100.0 : 100.0;
        double avgRating = ratingCount > 0 ? (totalRating / ratingCount) : 5.0;
        double qpsVal = (0.6 * acceptanceRate) + (0.4 * (avgRating / 5.0 * 100.0));
        result.qps = BigDecimal.valueOf(qpsVal).setScale(2, RoundingMode.HALF_UP);

        // 5. CDI - BR-21
        double cdiVal = totalDriftPct / deliveredPos.size();
        result.cdi = BigDecimal.valueOf(cdiVal).setScale(2, RoundingMode.HALF_UP);

        // 6. Overall Score - BR-21
        double overallVal = (wDrs * drsVal + wCss * cssVal + wDcs * dcsVal + wQps * qpsVal) / 100.0;
        result.overall = BigDecimal.valueOf(overallVal).setScale(2, RoundingMode.HALF_UP);

        return result;
    }
}

