package com.tcs.VendorPulse.procurement.controller;

import com.tcs.VendorPulse.procurement.entity.Delivery;
import com.tcs.VendorPulse.procurement.service.DeliveryService;
import com.tcs.VendorPulse.security.SecurityUtils;
import com.tcs.VendorPulse.user.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
    private final DeliveryService service;
    private final UserRepository users;

    public DeliveryController(DeliveryService s, UserRepository u) {
        this.service = s;
        this.users = u;
    }

    @PostMapping("/po/{poId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public Delivery record(@PathVariable Long poId,
                           @RequestParam LocalDate actualDate,
                           @RequestParam int delivered,
                           @RequestParam int accepted,
                           @RequestParam int rejected,
                           @RequestParam(required = false) String remarks,
                           Authentication auth) {
        Long managerId = SecurityUtils.getCurrentUserId(auth, users);
        return service.record(poId, actualDate, delivered, accepted, rejected, remarks, managerId);
    }
}
