package com.tcs.VendorPulse.inventory.api;

import com.tcs.VendorPulse.inventory.dto.InventoryDtos.*;
import com.tcs.VendorPulse.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "ERP/connector inventory stock push and risk monitoring APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/snapshots")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Receive stock snapshot", description = "Atomic push of inventory balances and daily consumption rates")
    public ResponseEntity<InventorySnapshotResponse> receiveSnapshot(
            @Valid @RequestBody InventorySnapshotRequest request) {
        return ResponseEntity.ok(inventoryService.receiveSnapshot(request));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get inventory connector status", description = "Status and last synchronization timestamp")
    public ResponseEntity<InventoryStatusDto> getStatus() {
        return ResponseEntity.ok(inventoryService.getStatus());
    }
}

