package com.tcs.VendorPulse.inventory.service;

import com.tcs.VendorPulse.alert.service.AlertService;
import com.tcs.VendorPulse.common.enums.AlertType;
import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.inventory.dto.InventoryDtos.*;
import com.tcs.VendorPulse.material.domain.Material;
import com.tcs.VendorPulse.material.repository.MaterialRepository;
import com.tcs.VendorPulse.po.domain.PoLine;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PoLineRepository;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.settings.service.ScoringSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final MaterialRepository materialRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoLineRepository poLineRepository;
    private final AlertService alertService;
    private final ScoringSettingsService settingsService;

    private volatile Instant lastSnapshotAsOf;
    private volatile String lastResult = "OK";
    private volatile String lastMessage = "Inventory connector operational";

    @Transactional
    public InventorySnapshotResponse receiveSnapshot(InventorySnapshotRequest request) {
        Instant asOf = request.getAsOf() != null ? request.getAsOf() : Instant.now();

        // 1. Validate ALL items first (atomic validation BR-27)
        Map<String, Material> materialCache = new HashMap<>();

        for (InventorySnapshotItem item : request.getItems()) {
            if (item.getMaterialCode() == null || item.getMaterialCode().isBlank()) {
                recordFailure("Invalid material code in snapshot", asOf);
                throw new ApiException(ErrorCode.INVALID_SNAPSHOT, "Empty material code");
            }
            if (item.getStockQty() == null || item.getStockQty().compareTo(BigDecimal.ZERO) < 0) {
                recordFailure("Negative stock quantity for " + item.getMaterialCode(), asOf);
                throw new ApiException(ErrorCode.INVALID_SNAPSHOT, "Stock quantity cannot be negative");
            }
            if (item.getDailyUsage() == null || item.getDailyUsage().compareTo(BigDecimal.ZERO) < 0) {
                recordFailure("Negative daily usage for " + item.getMaterialCode(), asOf);
                throw new ApiException(ErrorCode.INVALID_SNAPSHOT, "Daily usage cannot be negative");
            }

            Optional<Material> matOpt = materialRepository.findByCodeIgnoreCase(item.getMaterialCode().trim());
            if (matOpt.isEmpty()) {
                recordFailure("Unknown material code: " + item.getMaterialCode(), asOf);
                throw new ApiException(ErrorCode.INVALID_SNAPSHOT, "Unknown material code: " + item.getMaterialCode());
            }
            materialCache.put(item.getMaterialCode().trim().toUpperCase(), matOpt.get());
        }

        // 2. All items valid: apply updates in single transaction
        List<Material> toUpdate = new ArrayList<>();
        for (InventorySnapshotItem item : request.getItems()) {
            Material mat = materialCache.get(item.getMaterialCode().trim().toUpperCase());
            mat.setStockQty(item.getStockQty());
            mat.setDailyUsage(item.getDailyUsage());
            mat.setStockUpdatedAt(asOf);
            toUpdate.add(mat);
        }
        materialRepository.saveAll(toUpdate);

        // 3. Evaluate Stock Risk Alerts (BR-26)
        evaluateStockRiskAlerts();

        lastSnapshotAsOf = asOf;
        lastResult = "OK";
        lastMessage = "Snapshot processed successfully (" + toUpdate.size() + " items updated)";

        return InventorySnapshotResponse.builder()
                .acceptedCount(toUpdate.size())
                .asOf(asOf)
                .build();
    }

    public void evaluateStockRiskAlerts() {
        short horizonDays = settingsService.getEntitySettings().getStockAlertHorizonDays();
        List<Material> materials = materialRepository.findByIsActiveTrue();

        // Sort: Critical materials first (BR-26)
        materials.sort(Comparator.comparing(Material::getIsCritical).reversed());

        List<PurchaseOrder> openPos = purchaseOrderRepository.findAll().stream()
                .filter(p -> p.getStatus() == PoStatus.CONFIRMED ||
                             p.getStatus() == PoStatus.IN_PROGRESS ||
                             p.getStatus() == PoStatus.PARTIALLY_DELIVERED)
                .collect(Collectors.toList());

        for (Material m : materials) {
            if (m.getDailyUsage().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal daysOfCover = m.getStockQty().divide(m.getDailyUsage(), 2, RoundingMode.HALF_UP);

            boolean nearOutOfStock = daysOfCover.compareTo(BigDecimal.valueOf(horizonDays)) < 0;

            // Pre-Delivery Gap check: nearest open PO delivery date
            boolean preDeliveryGap = false;
            LocalDate nearestPoDate = findNearestDeliveryDateForMaterial(m.getId(), openPos);
            if (nearestPoDate != null) {
                long daysUntilDelivery = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), nearestPoDate));
                if (daysOfCover.compareTo(BigDecimal.valueOf(daysUntilDelivery)) < 0) {
                    preDeliveryGap = true;
                }
            }

            if (nearOutOfStock || preDeliveryGap) {
                String reason = nearOutOfStock
                        ? String.format("Near Out-of-Stock: %s (%s) has %.1f days of cover (horizon: %d days)",
                        m.getName(), m.getCode(), daysOfCover.doubleValue(), horizonDays)
                        : String.format("Pre-Delivery Gap: %s (%s) cover (%.1f days) falls short before next delivery (%s)",
                        m.getName(), m.getCode(), daysOfCover.doubleValue(), nearestPoDate);

                alertService.raiseAlert(AlertType.STOCK, reason, null, m.getId(), null);
            } else {
                alertService.recoverAlertsForMaterial(m.getId(), AlertType.STOCK);
            }
        }
    }

    private LocalDate findNearestDeliveryDateForMaterial(UUID materialId, List<PurchaseOrder> openPos) {
        LocalDate nearest = null;
        for (PurchaseOrder po : openPos) {
            List<PoLine> lines = poLineRepository.findByPoIdOrderByLineNoAsc(po.getId());
            boolean containsMaterial = lines.stream().anyMatch(l -> l.getMaterialId().equals(materialId));
            if (containsMaterial) {
                LocalDate date = po.getAgreedDeliveryDate() != null ? po.getAgreedDeliveryDate() : po.getRequiredDate();
                if (nearest == null || date.isBefore(nearest)) {
                    nearest = date;
                }
            }
        }
        return nearest;
    }

    private void recordFailure(String reason, Instant asOf) {
        lastSnapshotAsOf = asOf;
        lastResult = "FAILED";
        lastMessage = reason;
    }

    public InventoryStatusDto getStatus() {
        if (lastSnapshotAsOf == null) {
            Material latestMat = materialRepository.findAll().stream()
                    .filter(m -> m.getStockUpdatedAt() != null)
                    .max(Comparator.comparing(Material::getStockUpdatedAt))
                    .orElse(null);
            if (latestMat != null) {
                lastSnapshotAsOf = latestMat.getStockUpdatedAt();
            } else {
                lastSnapshotAsOf = Instant.now();
            }
        }

        return InventoryStatusDto.builder()
                .asOf(lastSnapshotAsOf)
                .lastResult(lastResult)
                .message(lastMessage)
                .build();
    }
}
