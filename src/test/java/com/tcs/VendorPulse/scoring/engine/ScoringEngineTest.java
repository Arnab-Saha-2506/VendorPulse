package com.tcs.VendorPulse.scoring.engine;

import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.common.enums.CommitmentType;
import com.tcs.VendorPulse.common.enums.DeliveryStatus;
import com.tcs.VendorPulse.common.enums.InspectionResult;
import com.tcs.VendorPulse.delivery.domain.Delivery;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ScoringEngineTest {

    private PurchaseOrder createPo(UUID id, LocalDate requiredDate, LocalDate agreedDate) {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(id);
        po.setRequiredDate(requiredDate);
        po.setAgreedDeliveryDate(agreedDate);
        po.setCreatedAt(Instant.now().minusSeconds(86400 * 30));
        return po;
    }

    private PoCommitment createCommitment(PurchaseOrder po, short seqNo, CommitmentType type, LocalDate date) {
        PoCommitment c = new PoCommitment();
        c.setId(UUID.randomUUID());
        c.setPoId(po.getId());
        c.setSeqNo(seqNo);
        c.setType(type);
        c.setCommittedDate(date);
        return c;
    }

    private Delivery createDelivery(PurchaseOrder po, LocalDate date, double delivered, double accepted, short rating) {
        Delivery d = new Delivery();
        d.setId(UUID.randomUUID());
        d.setPoId(po.getId());
        d.setDeliveryDate(date);
        d.setDeliveredQty(BigDecimal.valueOf(delivered));
        d.setAcceptedQty(BigDecimal.valueOf(accepted));
        d.setRejectedQty(BigDecimal.valueOf(delivered - accepted));
        d.setCompletion(DeliveryStatus.COMPLETE);
        d.setQualityRating(rating);
        d.setInspection(InspectionResult.PASS);
        return d;
    }

    @Test
    void testUnderThresholdReturnsUnrated() {
        // Less than 3 orders
        List<PurchaseOrder> pos = List.of(
                createPo(UUID.randomUUID(), LocalDate.now(), LocalDate.now()),
                createPo(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        );

        ScoringEngine.ScoreResult result = ScoringEngine.computeScores(
                pos, Collections.emptyMap(), Collections.emptyMap(), (short) 35, (short) 25, (short) 20, (short) 20);

        assertFalse(result.isRated);
        assertEquals(2, result.ordersCounted);
        assertEquals(BigDecimal.ZERO, result.overall);
    }

    @Test
    void testPerfectOrdersScore100() {
        UUID po1 = UUID.randomUUID();
        UUID po2 = UUID.randomUUID();
        UUID po3 = UUID.randomUUID();

        LocalDate baseDate = LocalDate.now();
        List<PurchaseOrder> pos = List.of(
                createPo(po1, baseDate, baseDate),
                createPo(po2, baseDate, baseDate),
                createPo(po3, baseDate, baseDate)
        );

        Map<UUID, List<PoCommitment>> commitments = new HashMap<>();
        Map<UUID, List<Delivery>> deliveries = new HashMap<>();

        for (PurchaseOrder po : pos) {
            commitments.put(po.getId(), List.of(
                    createCommitment(po, (short) 0, CommitmentType.ORIGINAL, baseDate)
            ));
            deliveries.put(po.getId(), List.of(
                    createDelivery(po, baseDate, 100.0, 100.0, (short) 5)
            ));
        }

        ScoringEngine.ScoreResult result = ScoringEngine.computeScores(
                pos, commitments, deliveries, (short) 35, (short) 25, (short) 20, (short) 20);

        assertTrue(result.isRated);
        assertEquals(3, result.ordersCounted);
        assertEquals(new BigDecimal("100.00"), result.drs);
        assertEquals(new BigDecimal("100.00"), result.css);
        assertEquals(new BigDecimal("100.00"), result.dcs);
        assertEquals(new BigDecimal("100.00"), result.qps);
        assertEquals(new BigDecimal("100.00"), result.overall);
        assertEquals(new BigDecimal("0.00"), result.cdi);
    }

    @Test
    void testCssDecreasesWithRevisions() {
        // 3 orders, total 3 revisions = avg 1 revision per order
        // CSS = 100 - (25 * 1) = 75
        UUID po1 = UUID.randomUUID();
        UUID po2 = UUID.randomUUID();
        UUID po3 = UUID.randomUUID();

        LocalDate baseDate = LocalDate.now();
        List<PurchaseOrder> pos = List.of(
                createPo(po1, baseDate, baseDate),
                createPo(po2, baseDate, baseDate),
                createPo(po3, baseDate, baseDate)
        );

        Map<UUID, List<PoCommitment>> commitments = new HashMap<>();
        Map<UUID, List<Delivery>> deliveries = new HashMap<>();

        for (PurchaseOrder po : pos) {
            commitments.put(po.getId(), List.of(
                    createCommitment(po, (short) 0, CommitmentType.ORIGINAL, baseDate),
                    createCommitment(po, (short) 1, CommitmentType.REVISION, baseDate)
            ));
            deliveries.put(po.getId(), List.of(
                    createDelivery(po, baseDate, 100.0, 100.0, (short) 5)
            ));
        }

        ScoringEngine.ScoreResult result = ScoringEngine.computeScores(
                pos, commitments, deliveries, (short) 35, (short) 25, (short) 20, (short) 20);

        assertEquals(new BigDecimal("75.00"), result.css);
    }
}
