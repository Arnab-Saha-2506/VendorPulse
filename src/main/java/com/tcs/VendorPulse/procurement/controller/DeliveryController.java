package com.tcs.VendorPulse.procurement.controller;

import com.tcs.VendorPulse.procurement.entity.Delivery;
import com.tcs.VendorPulse.procurement.service.DeliveryService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
    private final DeliveryService service;

    public DeliveryController(DeliveryService s) {
        service = s;
    }

    @PostMapping("/po/{poId}")
    public Delivery record(@PathVariable Long poId, @RequestParam LocalDate actualDate,
            @RequestParam int delivered, @RequestParam int accepted, @RequestParam int rejected,
            @RequestParam(required = false) String remarks, @RequestParam Long managerId) {
        return service.record(poId, actualDate, delivered, accepted, rejected, remarks, managerId);
    }
}