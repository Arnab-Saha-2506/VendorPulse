package com.tcs.VendorPulse.inventory.repository;

import com.tcs.VendorPulse.inventory.entity.InventorySyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventorySyncLogRepository extends JpaRepository<InventorySyncLog, Long> {}
