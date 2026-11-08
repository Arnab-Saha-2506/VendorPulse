package com.tcs.VendorPulse.inventory.service;

import com.tcs.VendorPulse.inventory.entity.*;
import com.tcs.VendorPulse.inventory.repository.*;
import com.tcs.VendorPulse.common.enums.AlertSeverity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class RiskAlertService {
    private final RiskAlertRepository alerts;
    private final InventorySnapshotRepository snapshots;

    public RiskAlertService(RiskAlertRepository a, InventorySnapshotRepository s) { alerts = a; snapshots = s; }

    @Transactional
    public void evaluate(String materialCode) {
        InventorySnapshot snap = snapshots.findTopByMaterialCodeOrderBySyncedAtDesc(materialCode).orElseThrow();
        RiskAlert existing = alerts.findByMaterialCodeAndActiveTrue(materialCode).orElse(null);
        if (snap.getAvailableStock() == 0) {
            upsert(existing, materialCode, AlertSeverity.CRITICAL, "Stock is zero");
        } else if (snap.getAvailableStock() <= snap.getSafetyStock()) {
            upsert(existing, materialCode, AlertSeverity.RISK, "Stock at or below safety stock");
        } else {
            if (existing != null) { existing.setActive(false); existing.setResolvedAt(LocalDateTime.now()); alerts.save(existing); }
        }
    }

    private void upsert(RiskAlert existing, String code, AlertSeverity sev, String msg) {
        if (existing != null) { existing.setSeverity(sev); existing.setMessage(msg); alerts.save(existing); }
        else {
            RiskAlert a = new RiskAlert(); a.setMaterialCode(code); a.setSeverity(sev); a.setMessage(msg);
            alerts.save(a);
        }
    }
}
