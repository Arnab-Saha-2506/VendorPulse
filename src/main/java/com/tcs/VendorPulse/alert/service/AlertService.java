package com.tcs.VendorPulse.alert.service;

import com.tcs.VendorPulse.alert.domain.Alert;
import com.tcs.VendorPulse.alert.dto.AlertDto;
import com.tcs.VendorPulse.alert.repository.AlertRepository;
import com.tcs.VendorPulse.common.enums.AlertStatus;
import com.tcs.VendorPulse.common.enums.AlertType;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.material.domain.Material;
import com.tcs.VendorPulse.material.repository.MaterialRepository;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MaterialRepository materialRepository;
    private final SupplierRepository supplierRepository;

    @Transactional
    public Alert raiseAlert(AlertType type, String message, UUID poId, UUID materialId, UUID supplierId) {
        // Avoid duplicate active alerts
        if (poId != null) {
            Optional<Alert> existing = alertRepository.findFirstByTypeAndPoIdAndStatus(type, poId, AlertStatus.ACTIVE);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (materialId != null) {
            Optional<Alert> existing = alertRepository.findFirstByTypeAndMaterialIdAndStatus(type, materialId, AlertStatus.ACTIVE);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (supplierId != null && poId == null && materialId == null) {
            Optional<Alert> existing = alertRepository.findFirstByTypeAndSupplierIdAndStatus(type, supplierId, AlertStatus.ACTIVE);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Alert alert = Alert.builder()
                .id(UUID.randomUUID())
                .type(type)
                .message(message)
                .status(AlertStatus.ACTIVE)
                .poId(poId)
                .materialId(materialId)
                .supplierId(supplierId)
                .build();

        return alertRepository.save(alert);
    }

    @Transactional
    public void recoverAlertsForPo(UUID poId) {
        List<Alert> alerts = alertRepository.findByPoIdAndStatus(poId, AlertStatus.ACTIVE);
        for (Alert alert : alerts) {
            alert.setStatus(AlertStatus.RECOVERED);
            alert.setResolvedAt(Instant.now());
        }
        alertRepository.saveAll(alerts);
    }

    @Transactional
    public void recoverAlertsForMaterial(UUID materialId, AlertType type) {
        alertRepository.findFirstByTypeAndMaterialIdAndStatus(type, materialId, AlertStatus.ACTIVE)
                .ifPresent(alert -> {
                    alert.setStatus(AlertStatus.RECOVERED);
                    alert.setResolvedAt(Instant.now());
                    alertRepository.save(alert);
                });
    }

    @Transactional
    public void recoverAlertsForSupplier(UUID supplierId, AlertType type) {
        alertRepository.findFirstByTypeAndSupplierIdAndStatus(type, supplierId, AlertStatus.ACTIVE)
                .ifPresent(alert -> {
                    alert.setStatus(AlertStatus.RECOVERED);
                    alert.setResolvedAt(Instant.now());
                    alertRepository.save(alert);
                });
    }

    @Transactional
    public Alert dismissAlert(UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Alert not found"));
        alert.setStatus(AlertStatus.DISMISSED);
        alert.setResolvedAt(Instant.now());
        return alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AlertDto> searchAlerts(AlertType type, AlertStatus status, Pageable pageable) {
        Page<Alert> page = alertRepository.searchAlerts(type, status, pageable);

        Set<UUID> poIds = page.getContent().stream().map(Alert::getPoId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> matIds = page.getContent().stream().map(Alert::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> supIds = page.getContent().stream().map(Alert::getSupplierId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<UUID, String> poMap = purchaseOrderRepository.findAllById(poIds).stream()
                .collect(Collectors.toMap(PurchaseOrder::getId, PurchaseOrder::getPoNumber));
        Map<UUID, String> matMap = materialRepository.findAllById(matIds).stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));
        Map<UUID, String> supMap = supplierRepository.findAllById(supIds).stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getCompanyName));

        Page<AlertDto> dtoPage = page.map(a -> AlertDto.builder()
                .id(a.getId())
                .type(a.getType())
                .message(a.getMessage())
                .status(a.getStatus())
                .poId(a.getPoId())
                .poNumber(poMap.get(a.getPoId()))
                .materialId(a.getMaterialId())
                .materialName(matMap.get(a.getMaterialId()))
                .supplierId(a.getSupplierId())
                .supplierName(supMap.get(a.getSupplierId()))
                .raisedAt(a.getRaisedAt())
                .resolvedAt(a.getResolvedAt())
                .build());

        return PagedResponse.from(dtoPage);
    }
}

