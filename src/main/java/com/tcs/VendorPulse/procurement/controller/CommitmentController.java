package com.tcs.VendorPulse.procurement.controller;

import com.tcs.VendorPulse.procurement.entity.Commitment;
import com.tcs.VendorPulse.procurement.entity.CommitmentRevision;
import com.tcs.VendorPulse.procurement.service.CommitmentService;
import com.tcs.VendorPulse.security.SecurityUtils;
import com.tcs.VendorPulse.user.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/commitments")
public class CommitmentController {
    private final CommitmentService service;
    private final UserRepository users;

    public CommitmentController(CommitmentService s, UserRepository u) {
        this.service = s;
        this.users = u;
    }

    @PostMapping("/po/{poId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER', 'SUPPLIER')")
    public Commitment create(@PathVariable Long poId, @RequestParam LocalDate deliveryDate) {
        return service.createForPo(poId, deliveryDate);
    }

    @PostMapping("/{id}/request-revision")
    @PreAuthorize("hasRole('SUPPLIER')")
    public Commitment request(@PathVariable Long id,
                              @RequestParam LocalDate newDate,
                              @RequestParam String reason,
                              Authentication auth) {
        Long supplierUserId = SecurityUtils.getCurrentUserId(auth, users);
        return service.requestRevision(id, newDate, reason, supplierUserId);
    }

    @PutMapping("/revisions/{revisionId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public Commitment approve(@PathVariable Long revisionId, Authentication auth) {
        Long managerId = SecurityUtils.getCurrentUserId(auth, users);
        return service.approveRevision(revisionId, managerId);
    }

    @PutMapping("/revisions/{revisionId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public Commitment reject(@PathVariable Long revisionId, Authentication auth) {
        Long managerId = SecurityUtils.getCurrentUserId(auth, users);
        return service.rejectRevision(revisionId, managerId);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public List<CommitmentRevision> history(@PathVariable Long id) {
        return service.history(id);
    }
}
