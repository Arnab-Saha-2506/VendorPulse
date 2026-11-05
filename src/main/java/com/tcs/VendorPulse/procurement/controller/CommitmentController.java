package com.tcs.VendorPulse.procurement.controller;

import com.tcs.VendorPulse.procurement.entity.*;
import com.tcs.VendorPulse.procurement.service.CommitmentService;
import com.tcs.VendorPulse.user.repository.UserRepository;
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
        service = s;
        users = u;
    }

    private Long currentUserId(Authentication auth) {
        return users.findByEmail(auth.getName()).orElseThrow().getId();
    }

    @PostMapping("/po/{poId}")
    public Commitment create(@PathVariable Long poId, @RequestParam LocalDate deliveryDate) {
        return service.createForPo(poId, deliveryDate);
    }

    @PostMapping("/{id}/request-revision")
    public Commitment request(@PathVariable Long id, @RequestParam LocalDate newDate, @RequestParam String reason,
            Authentication auth) {
        return service.requestRevision(id, newDate, reason, currentUserId(auth));
    }

    @PutMapping("/revisions/{revisionId}/approve")
    public Commitment approve(@PathVariable Long revisionId, Authentication auth) {
        return service.approveRevision(revisionId, currentUserId(auth));
    }

    @PutMapping("/revisions/{revisionId}/reject")
    public Commitment reject(@PathVariable Long revisionId, Authentication auth) {
        return service.rejectRevision(revisionId, currentUserId(auth));
    }

    @GetMapping("/{id}/history")
    public List<CommitmentRevision> history(@PathVariable Long id) {
        return service.history(id);
    }
}