package com.tcs.VendorPulse.scoring.service;

import com.tcs.VendorPulse.scoring.entity.*;
import com.tcs.VendorPulse.scoring.repository.*;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.procurement.repository.*;
import com.tcs.VendorPulse.procurement.entity.*;
import com.tcs.VendorPulse.common.enums.RatingStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SupplierScoringServiceImpl implements SupplierScoringService {
    private final SupplierScoreRepository scores;
    private final SupplierScoreHistoryRepository history;
    private final SupplierRepository suppliers;
    private final DeliveryRepository deliveries;
    private final CommitmentRepository commitments;
    private final CommitmentRevisionRepository revisions;
    private final QualityFeedbackRepository feedbacks;
    private static final int MIN_DELIVERIES = 3;

    public SupplierScoringServiceImpl(SupplierScoreRepository s, SupplierScoreHistoryRepository h,
            SupplierRepository sup,
            DeliveryRepository d, CommitmentRepository c, CommitmentRevisionRepository r, QualityFeedbackRepository q) {
        scores = s;
        history = h;
        suppliers = sup;
        deliveries = d;
        commitments = c;
        revisions = r;
        feedbacks = q;
    }

    @Transactional
    public SupplierScore calculate(Long supplierId) {
        Supplier supplier = suppliers.findById(supplierId).orElseThrow();
        List<Delivery> ds = deliveries.findBySupplierId(supplierId);
        if (ds.size() < MIN_DELIVERIES) {
            return saveNotRated(supplier);
        }
        double deliveryReliability = ds.stream()
                .mapToDouble(d -> d.getDeliveredQty() == 0 ? 0 : (double) d.getAcceptedQty() / d.getDeliveredQty() * 100)
                .average().orElse(0);
        List<Commitment> cs = commitments.findBySupplierId(supplierId);
        int revisionCount = cs.stream().mapToInt(c -> revisions.findByCommitmentId(c.getId()).size()).sum();
        double commitmentStability = Math.max(0, 100 - revisionCount * 10.0);
        List<Long> deliveryIds = ds.stream().map(Delivery::getId).toList();
        List<QualityFeedback> fs = feedbacks.findAll().stream()
                .filter(q -> deliveryIds.contains(q.getDelivery().getId())).toList();
        double qualityPerformance = fs.isEmpty() ? 0
                : fs.stream().mapToInt(QualityFeedback::getRating).average().orElse(0) * 20;
        double deliveryConsistency = ds.isEmpty() ? 0
                : (double) ds.stream().filter(d -> !d.getActualDeliveryDate().isAfter(d.getPurchaseOrder().getIssueDate())).count()
                        / ds.size() * 100;
        double cdi = revisionCount * 5.0;
        double overall = deliveryReliability * 0.3 + commitmentStability * 0.2 + deliveryConsistency * 0.3
                + qualityPerformance * 0.2;
        SupplierScore sc = scores.findBySupplierId(supplierId).orElse(new SupplierScore());
        sc.setSupplier(supplier);
        sc.setDeliveryReliability(deliveryReliability);
        sc.setCommitmentStability(commitmentStability);
        sc.setDeliveryConsistency(deliveryConsistency);
        sc.setQualityPerformance(qualityPerformance);
        sc.setOverallScore(overall);
        sc.setCdi(cdi);
        sc.setRatingStatus(RatingStatus.RATED);
        scores.save(sc);
        // history
        SupplierScoreHistory h = new SupplierScoreHistory();
        h.setSupplier(supplier);
        h.setDeliveryReliability(deliveryReliability);
        h.setCommitmentStability(commitmentStability);
        h.setDeliveryConsistency(deliveryConsistency);
        h.setQualityPerformance(qualityPerformance);
        h.setOverallScore(overall);
        h.setCdi(cdi);
        h.setRatingStatus(RatingStatus.RATED);
        history.save(h);
        return sc;
    }

    private SupplierScore saveNotRated(Supplier supplier) {
        SupplierScore sc = scores.findBySupplierId(supplier.getId()).orElse(new SupplierScore());
        sc.setSupplier(supplier);
        sc.setRatingStatus(RatingStatus.NOT_RATED);
        sc.setCdi(0.0);
        scores.save(sc);
        return sc;
    }

    public SupplierScore get(Long supplierId) {
        return scores.findBySupplierId(supplierId).orElseThrow();
    }
}