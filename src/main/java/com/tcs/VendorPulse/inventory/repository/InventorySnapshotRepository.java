package com.tcs.VendorPulse.inventory.repository;

import com.tcs.VendorPulse.inventory.entity.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, Long> {
    Optional<InventorySnapshot> findTopByMaterialCodeOrderBySyncedAtDesc(String code);
}
