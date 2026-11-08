package com.tcs.VendorPulse.inventory.service;

import com.tcs.VendorPulse.inventory.entity.InventorySnapshot;
import com.tcs.VendorPulse.inventory.repository.InventorySnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final InventorySnapshotRepository repo;
    private final RiskAlertService riskAlerts;

    public InventoryServiceImpl(InventorySnapshotRepository r, RiskAlertService risk) { repo = r; riskAlerts = risk; }

    @Transactional
    public InventorySnapshot sync(String materialCode, int available, int safety, double consumption) {
        try {
            InventorySnapshot snap = new InventorySnapshot();
            snap.setMaterialCode(materialCode);
            snap.setAvailableStock(available);
            snap.setSafetyStock(safety);
            snap.setAvgDailyConsumption(consumption);
            snap.setSyncedAt(LocalDateTime.now());
            snap.setSuccessful(true);
            if (available <= safety && consumption > 0) {
                snap.setEstimatedStockoutDays((int) Math.ceil((double) available / consumption));
            }
            InventorySnapshot saved = repo.save(snap);
            riskAlerts.evaluate(materialCode);
            return saved;
        } catch (Exception e) {
            return repo.findTopByMaterialCodeOrderBySyncedAtDesc(materialCode).orElseThrow();
        }
    }

    public InventorySnapshot latest(String code) {
        return repo.findTopByMaterialCodeOrderBySyncedAtDesc(code).orElseThrow();
    }

    public List<InventorySnapshot> all() {
        return repo.findAll();
    }
}
