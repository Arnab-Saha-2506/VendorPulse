package com.tcs.VendorPulse.procurement.controller;

import com.tcs.VendorPulse.procurement.entity.QualityFeedback;
import com.tcs.VendorPulse.procurement.service.QualityFeedbackService;
import com.tcs.VendorPulse.security.SecurityUtils;
import com.tcs.VendorPulse.user.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/quality-feedback")
public class QualityFeedbackController {
    private final QualityFeedbackService service;
    private final UserRepository users;

    public QualityFeedbackController(QualityFeedbackService s, UserRepository u) {
        this.service = s;
        this.users = u;
    }

    @PostMapping("/delivery/{deliveryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public QualityFeedback submit(@PathVariable Long deliveryId,
                                 @RequestParam int rating,
                                 @RequestParam(required = false) String comments,
                                 Authentication auth) {
        Long managerId = SecurityUtils.getCurrentUserId(auth, users);
        return service.submit(deliveryId, rating, comments, managerId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public QualityFeedback get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/delivery/{deliveryId}")
    @PreAuthorize("isAuthenticated()")
    public List<QualityFeedback> byDelivery(@PathVariable Long deliveryId) {
        return service.byDelivery(deliveryId);
    }
}
