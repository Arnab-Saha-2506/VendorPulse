package com.tcs.VendorPulse.report.service;

import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.commitment.repository.PoCommitmentRepository;
import com.tcs.VendorPulse.common.enums.CommitmentType;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.report.dto.ReportDtos.*;
import com.tcs.VendorPulse.scoring.domain.SupplierScoreHistory;
import com.tcs.VendorPulse.scoring.repository.SupplierScoreHistoryRepository;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.domain.SupplierCategory;
import com.tcs.VendorPulse.supplier.repository.SupplierCategoryRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.user.domain.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SupplierScoreHistoryRepository scoreHistoryRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierCategoryRepository supplierCategoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoCommitmentRepository commitmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SupplierPerformanceReportRow> getSupplierPerformanceReport(
            LocalDate from, LocalDate to, Short categoryId, UUID supplierId) {

        List<SupplierScoreHistory> list = scoreHistoryRepository.findAll();

        if (from != null) {
            list = list.stream().filter(h -> !h.getPeriod().isBefore(from)).collect(Collectors.toList());
        }
        if (to != null) {
            list = list.stream().filter(h -> !h.getPeriod().isAfter(to)).collect(Collectors.toList());
        }
        if (supplierId != null) {
            list = list.stream().filter(h -> h.getSupplierId().equals(supplierId)).collect(Collectors.toList());
        }
        if (categoryId != null) {
            Set<UUID> matchedSuppliers = supplierCategoryRepository.findByCategoryId(categoryId).stream()
                    .map(SupplierCategory::getSupplierId).collect(Collectors.toSet());
            list = list.stream().filter(h -> matchedSuppliers.contains(h.getSupplierId())).collect(Collectors.toList());
        }

        Map<UUID, String> supMap = supplierRepository.findAllById(
                list.stream().map(SupplierScoreHistory::getSupplierId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Supplier::getId, Supplier::getCompanyName));

        return list.stream().map(h -> SupplierPerformanceReportRow.builder()
                .supplierId(h.getSupplierId())
                .supplierName(supMap.getOrDefault(h.getSupplierId(), "Supplier"))
                .period(h.getPeriod())
                .overallScore(h.getOverallScore())
                .deliveryReliability(h.getDeliveryReliability())
                .commitmentStability(h.getCommitmentStability())
                .deliveryConsistency(h.getDeliveryConsistency())
                .qualityPerformance(h.getQualityPerformance())
                .cdi(h.getCdi())
                .ordersCounted(h.getOrdersCounted())
                .build()).collect(Collectors.toList());
    }

    public String generateSupplierPerformanceCsv(List<SupplierPerformanceReportRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("Supplier,Period,Overall Score,Delivery Reliability,Commitment Stability,Delivery Consistency,Quality Performance,CDI,Orders Counted\n");
        for (SupplierPerformanceReportRow r : rows) {
            sb.append(String.format("\"%s\",%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d\n",
                    r.getSupplierName(), r.getPeriod(), r.getOverallScore(),
                    r.getDeliveryReliability(), r.getCommitmentStability(),
                    r.getDeliveryConsistency(), r.getQualityPerformance(),
                    r.getCdi(), r.getOrdersCounted()));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public List<CommitmentHistoryReportRow> getCommitmentHistoryReport(
            LocalDate from, LocalDate to, Short categoryId, UUID supplierId) {

        List<PurchaseOrder> pos = purchaseOrderRepository.findAll();

        if (supplierId != null) {
            pos = pos.stream().filter(p -> p.getSupplierId().equals(supplierId)).collect(Collectors.toList());
        }
        if (categoryId != null) {
            Set<UUID> matchedSuppliers = supplierCategoryRepository.findByCategoryId(categoryId).stream()
                    .map(SupplierCategory::getSupplierId).collect(Collectors.toSet());
            pos = pos.stream().filter(p -> matchedSuppliers.contains(p.getSupplierId())).collect(Collectors.toList());
        }

        Map<UUID, String> supMap = supplierRepository.findAllById(
                pos.stream().map(PurchaseOrder::getSupplierId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Supplier::getId, Supplier::getCompanyName));

        List<CommitmentHistoryReportRow> rows = new ArrayList<>();

        for (PurchaseOrder po : pos) {
            List<PoCommitment> commitments = commitmentRepository.findByPoIdOrderBySeqNoAsc(po.getId());
            LocalDate originalDate = commitments.stream()
                    .filter(c -> c.getSeqNo() == 0)
                    .map(PoCommitment::getCommittedDate)
                    .findFirst().orElse(po.getRequiredDate());

            List<PoCommitment> revisions = commitments.stream()
                    .filter(c -> c.getType() == CommitmentType.REVISION)
                    .collect(Collectors.toList());

            for (PoCommitment rev : revisions) {
                if (from != null && rev.getCommittedDate().isBefore(from)) continue;
                if (to != null && rev.getCommittedDate().isAfter(to)) continue;

                String userName = rev.getCreatedBy() != null ?
                        userRepository.findById(rev.getCreatedBy()).map(User::getFullName).orElse("Supplier") : "Supplier";

                rows.add(CommitmentHistoryReportRow.builder()
                        .poNumber(po.getPoNumber())
                        .supplierName(supMap.getOrDefault(po.getSupplierId(), "Supplier"))
                        .originalDate(originalDate)
                        .revisedDate(rev.getCommittedDate())
                        .reason(rev.getReason())
                        .revisedBy(userName)
                        .build());
            }
        }

        return rows;
    }

    public String generateCommitmentHistoryCsv(List<CommitmentHistoryReportRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("PO Number,Supplier,Original Date,Revised Date,Reason,Revised By\n");
        for (CommitmentHistoryReportRow r : rows) {
            sb.append(String.format("\"%s\",\"%s\",%s,%s,\"%s\",\"%s\"\n",
                    r.getPoNumber(), r.getSupplierName(), r.getOriginalDate(),
                    r.getRevisedDate(), r.getReason() != null ? r.getReason() : "", r.getRevisedBy()));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public List<SupplierComparisonReportRow> getSupplierComparisonReport(Short categoryId) {
        List<Supplier> suppliers = supplierRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .collect(Collectors.toList());

        if (categoryId != null) {
            Set<UUID> matched = supplierCategoryRepository.findByCategoryId(categoryId).stream()
                    .map(SupplierCategory::getSupplierId).collect(Collectors.toSet());
            suppliers = suppliers.stream().filter(s -> matched.contains(s.getId())).collect(Collectors.toList());
        }

        List<SupplierComparisonReportRow> rows = new ArrayList<>();
        for (Supplier s : suppliers) {
            Optional<SupplierScoreHistory> sh = scoreHistoryRepository.findFirstBySupplierIdOrderByPeriodDesc(s.getId());
            if (sh.isPresent() && sh.get().getOrdersCounted() >= 3) {
                rows.add(SupplierComparisonReportRow.builder()
                        .supplierName(s.getCompanyName())
                        .overallScore(sh.get().getOverallScore())
                        .cdi(sh.get().getCdi())
                        .deliveryConsistency(sh.get().getDeliveryConsistency())
                        .qualityPerformance(sh.get().getQualityPerformance())
                        .build());
            } else {
                rows.add(SupplierComparisonReportRow.builder()
                        .supplierName(s.getCompanyName())
                        .overallScore(null)
                        .cdi(null)
                        .deliveryConsistency(null)
                        .qualityPerformance(null)
                        .build());
            }
        }

        // Sort by overall score desc (not rated last)
        rows.sort((a, b) -> {
            if (a.getOverallScore() == null && b.getOverallScore() == null) return 0;
            if (a.getOverallScore() == null) return 1;
            if (b.getOverallScore() == null) return -1;
            return b.getOverallScore().compareTo(a.getOverallScore());
        });

        int rank = 1;
        for (SupplierComparisonReportRow r : rows) {
            if (r.getOverallScore() != null) {
                r.setRank(rank++);
            } else {
                r.setRank(rows.size());
            }
        }

        return rows;
    }

    public String generateSupplierComparisonCsv(List<SupplierComparisonReportRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rank,Supplier,Overall Score,CDI,Delivery Consistency,Quality Performance\n");
        for (SupplierComparisonReportRow r : rows) {
            String overall = r.getOverallScore() != null ? r.getOverallScore().toPlainString() : "Not rated";
            String cdi = r.getCdi() != null ? r.getCdi().toPlainString() : "N/A";
            String dc = r.getDeliveryConsistency() != null ? r.getDeliveryConsistency().toPlainString() : "N/A";
            String qp = r.getQualityPerformance() != null ? r.getQualityPerformance().toPlainString() : "N/A";
            sb.append(String.format("%d,\"%s\",%s,%s,%s,%s\n",
                    r.getRank(), r.getSupplierName(), overall, cdi, dc, qp));
        }
        return sb.toString();
    }
}

