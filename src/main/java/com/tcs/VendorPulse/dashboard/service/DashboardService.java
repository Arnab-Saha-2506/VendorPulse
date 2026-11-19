package com.tcs.VendorPulse.dashboard.service;

import com.tcs.VendorPulse.alert.repository.AlertRepository;
import com.tcs.VendorPulse.alert.service.AlertService;
import com.tcs.VendorPulse.common.enums.AlertStatus;
import com.tcs.VendorPulse.common.enums.ApprovalStatus;
import com.tcs.VendorPulse.common.enums.PartySide;
import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.dashboard.dto.DashboardDtos.*;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.dto.PoDtos.PoSummaryDto;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.scoring.domain.SupplierScoreHistory;
import com.tcs.VendorPulse.scoring.repository.SupplierScoreHistoryRepository;
import com.tcs.VendorPulse.settings.service.ScoringSettingsService;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final AlertRepository alertRepository;
    private final AlertService alertService;
    private final SupplierScoreHistoryRepository scoreHistoryRepository;
    private final ScoringSettingsService settingsService;

    @Transactional(readOnly = true)
    public PmDashboardDto getPmDashboard() {
        long openPos = purchaseOrderRepository.countOpenPos();
        long pendingApprovals = supplierRepository.countByApprovalStatus(ApprovalStatus.PENDING);
        long activeAlerts = alertRepository.countByStatus(AlertStatus.ACTIVE);

        short highCdiThreshold = settingsService.getEntitySettings().getHighCdiThreshold();
        List<SupplierScoreHistory> latestScores = supplierRepository.findAll().stream()
                .map(s -> scoreHistoryRepository.findFirstBySupplierIdOrderByPeriodDesc(s.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        long highCdiCount = latestScores.stream()
                .filter(sh -> sh.getOrdersCounted() >= 3 && sh.getCdi().compareTo(BigDecimal.valueOf(highCdiThreshold)) >= 0)
                .count();

        // POs needing attention: awaiting manufacturer, or partially delivered, or near cutoff
        List<PurchaseOrder> allPos = purchaseOrderRepository.findAll();
        Map<UUID, String> supplierNames = supplierRepository.findAllById(
                allPos.stream().map(PurchaseOrder::getSupplierId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Supplier::getId, Supplier::getCompanyName));

        List<PoSummaryDto> posNeedingAttention = allPos.stream()
                .filter(po -> po.getAwaitingParty() == PartySide.MANUFACTURER ||
                              po.getStatus() == PoStatus.PARTIALLY_DELIVERED ||
                              po.getStatus() == PoStatus.SUBMITTED)
                .limit(5)
                .map(po -> PoSummaryDto.builder()
                        .id(po.getId())
                        .poNumber(po.getPoNumber())
                        .supplierId(po.getSupplierId())
                        .supplierName(supplierNames.getOrDefault(po.getSupplierId(), ""))
                        .status(po.getStatus())
                        .requiredDate(po.getRequiredDate())
                        .agreedDeliveryDate(po.getAgreedDeliveryDate())
                        .agreedPriceTotal(po.getAgreedPriceTotal())
                        .awaitingParty(po.getAwaitingParty())
                        .createdAt(po.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        var recentAlerts = alertService.searchAlerts(null, AlertStatus.ACTIVE, PageRequest.of(0, 5)).getContent();

        return PmDashboardDto.builder()
                .openPos(openPos)
                .pendingApprovals(pendingApprovals)
                .highCdiSuppliers(highCdiCount)
                .activeAlerts(activeAlerts)
                .posNeedingAttention(posNeedingAttention)
                .alerts(recentAlerts)
                .build();
    }

    @Transactional(readOnly = true)
    public SupplierDashboardDto getSupplierDashboard(UUID supplierId) {
        List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierId(supplierId);

        long proposalsAwaiting = pos.stream()
                .filter(p -> p.getAwaitingParty() == PartySide.SUPPLIER && p.getStatus() == PoStatus.SUBMITTED)
                .count();

        long openOrders = pos.stream()
                .filter(p -> p.getStatus() == PoStatus.CONFIRMED ||
                             p.getStatus() == PoStatus.IN_PROGRESS ||
                             p.getStatus() == PoStatus.PARTIALLY_DELIVERED)
                .count();

        Optional<SupplierScoreHistory> scoreOpt = scoreHistoryRepository.findFirstBySupplierIdOrderByPeriodDesc(supplierId);

        BigDecimal reliabilityScore = scoreOpt.map(SupplierScoreHistory::getDeliveryReliability).orElse(BigDecimal.ZERO);
        BigDecimal cdi = scoreOpt.map(SupplierScoreHistory::getCdi).orElse(BigDecimal.ZERO);

        String companyName = supplierRepository.findById(supplierId).map(Supplier::getCompanyName).orElse("");

        List<PoSummaryDto> needsAction = pos.stream()
                .filter(p -> p.getAwaitingParty() == PartySide.SUPPLIER ||
                             p.getStatus() == PoStatus.CONFIRMED ||
                             p.getStatus() == PoStatus.IN_PROGRESS)
                .limit(5)
                .map(po -> PoSummaryDto.builder()
                        .id(po.getId())
                        .poNumber(po.getPoNumber())
                        .supplierId(supplierId)
                        .supplierName(companyName)
                        .status(po.getStatus())
                        .requiredDate(po.getRequiredDate())
                        .agreedDeliveryDate(po.getAgreedDeliveryDate())
                        .agreedPriceTotal(po.getAgreedPriceTotal())
                        .awaitingParty(po.getAwaitingParty())
                        .createdAt(po.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return SupplierDashboardDto.builder()
                .proposalsAwaiting(proposalsAwaiting)
                .openOrders(openOrders)
                .reliabilityScore(reliabilityScore)
                .cdi(cdi)
                .needsAction(needsAction)
                .build();
    }
}

