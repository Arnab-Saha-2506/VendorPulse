package com.tcs.VendorPulse.inventory.service;

import com.tcs.VendorPulse.inventory.entity.InventorySnapshot;
import java.util.List;

public interface InventoryService {
    InventorySnapshot sync(String materialCode, int available, int safety, double consumption);
    InventorySnapshot latest(String materialCode);
    List<InventorySnapshot> all();
}
