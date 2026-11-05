package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.procurement.entity.*;
import com.tcs.VendorPulse.procurement.repository.*;
import com.tcs.VendorPulse.user.repository.UserRepository;
import com.tcs.VendorPulse.common.enums.CommitmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class CommitmentServiceImpl implements CommitmentService {
    private final CommitmentRepository commitments;
    private final CommitmentRevisionRepository revisions;
    private final PurchaseOrderRepository pos;
    private final UserRepository users;

    public CommitmentServiceImpl(CommitmentRepository c, CommitmentRevisionRepository r, PurchaseOrderRepository p,
            UserRepository u) {
        commitments = c;
        revisions = r;
        pos = p;
        users = u;
    }

    @Transactional
    public Commitment createForPo(Long poId, LocalDate deliveryDate) {
        PurchaseOrder po = pos.findById(poId).orElseThrow();
        Commitment c = new Commitment();
        c.setPurchaseOrder(po);
        c.setCommittedDeliveryDate(deliveryDate);
        c.setStatus(CommitmentStatus.ON_TRACK);
        return commitments.save(c);
    }

    @Transactional
    public Commitment requestRevision(Long commitmentId, LocalDate newDate, String reason, Long supplierUserId) {
        Commitment c = commitments.findById(commitmentId).orElseThrow();
        CommitmentRevision r = new CommitmentRevision();
        r.setCommitment(c);
        r.setOldDate(c.getCommittedDeliveryDate());
        r.setNewDate(newDate);
        r.setReason(reason);
        r.setRevisedBy(users.findById(supplierUserId).orElseThrow());
        revisions.save(r);
        c.setStatus(CommitmentStatus.REVISED); // pending manager decision
        return commitments.save(c);
    }

    @Transactional
    public Commitment approveRevision(Long revisionId, Long managerId) {
        CommitmentRevision r = revisions.findById(revisionId).orElseThrow();
        Commitment c = r.getCommitment();
        c.setCommittedDeliveryDate(r.getNewDate());
        c.setStatus(CommitmentStatus.ON_TRACK);
        return commitments.save(c);
    }

    @Transactional
    public Commitment rejectRevision(Long revisionId, Long managerId) {
        CommitmentRevision r = revisions.findById(revisionId).orElseThrow();
        Commitment c = r.getCommitment();
        c.setStatus(CommitmentStatus.ON_TRACK); // revert to on track, original date unchanged
        return commitments.save(c);
    }

    public List<CommitmentRevision> history(Long commitmentId) {
        return revisions.findByCommitmentId(commitmentId);
    }
}