package com.tcs.VendorPulse.inventory.service;

import com.tcs.VendorPulse.inventory.client.InventoryClient;
import com.tcs.VendorPulse.inventory.repository.InventorySyncLogRepository;
import com.tcs.VendorPulse.inventory.entity.InventorySyncLog;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class InventorySyncScheduler {
    private final InventoryClient client;
    private final InventoryService inventoryService;
    private final InventorySyncLogRepository logs;
    private final RiskAlertService riskAlerts;

    public InventorySyncScheduler(InventoryClient c, InventoryService i, InventorySyncLogRepository l, RiskAlertService r) {
        client = c; inventoryService = i; logs = l; riskAlerts = r;
    }

    @Scheduled(cron = "${inventory.sync.cron:0 0 9,21 * * *}")
    @Transactional
    public void syncAll() {
        // demo: sync a single material code; extend to list of materials
        String code = "MAT-101";
        InventorySyncLog log = new InventorySyncLog();
        log.setMaterialCode(code);
        try {
            Map<String, Object> data = client.fetch(code);
            int available = ((Number) data.get("availableQuantity")).intValue();
            int safety = ((Number) data.get("safetyStock")).intValue();
            double consumption = ((Number) data.get("averageDailyConsumption")).doubleValue();
            inventoryService.sync(code, available, safety, consumption);
            riskAlerts.evaluate(code);
            log.setSuccess(true);
            log.setMessage("Synced successfully");
        } catch (Exception e) {
            log.setSuccess(false);
            log.setMessage("Sync failed: " + e.getMessage());
        }
        logs.save(log);
    }
}
