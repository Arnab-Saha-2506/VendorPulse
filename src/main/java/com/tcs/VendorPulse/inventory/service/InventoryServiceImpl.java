package com.tcs.VendorPulse.inventory.service;

import com.tcs.VendorPulse.inventory.entity.InventorySnapshot;
import com.tcs.VendorPulse.inventory.entity.InventorySyncLog;
import com.tcs.VendorPulse.inventory.repository.InventorySnapshotRepository;
import com.tcs.VendorPulse.inventory.repository.InventorySyncLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final InventorySnapshotRepository repo;
    private final RiskAlertService riskAlerts;
    private final InventorySyncLogRepository syncLogs;

    public InventoryServiceImpl(InventorySnapshotRepository r, RiskAlertService risk, InventorySyncLogRepository l) {
        repo = r; riskAlerts = risk; syncLogs = l;
    }

    @Transactional
    public InventorySnapshot sync(String materialCode, int available, int safety, double consumption) {
        InventorySyncLog log = new InventorySyncLog();
        log.setMaterialCode(materialCode);
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
            log.setSuccess(true);
            log.setMessage("Synced successfully");
            syncLogs.save(log);
            return saved;
        } catch (Exception e) {
            log.setSuccess(false);
            log.setMessage("Sync failed: " + e.getMessage());
            syncLogs.save(log);
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
