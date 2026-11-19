package com.tcs.VendorPulse.delivery.api;

import com.tcs.VendorPulse.delivery.dto.DeliveryDtos.*;
import com.tcs.VendorPulse.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders/{id}/deliveries")
@RequiredArgsConstructor
@Tag(name = "Deliveries", description = "Delivery recording, quality inspection, and defect tracking APIs")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    @Operation(summary = "Get deliveries list", description = "Deliveries overview with ordered, delivered, and remaining quantities")
    public ResponseEntity<DeliveryOverviewDto> getDeliveries(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryService.getDeliveriesOverview(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Record delivery and quality inspection", description = "Record goods received, inspect quality (1-5), and update order status")
    public ResponseEntity<RecordDeliveryResponse> recordDelivery(
            @PathVariable UUID id,
            @Valid @RequestBody RecordDeliveryRequest request) {
        return ResponseEntity.ok(deliveryService.recordDelivery(id, request));
    }
}

