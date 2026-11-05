package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.procurement.entity.Commitment;
import com.tcs.VendorPulse.procurement.entity.CommitmentRevision;
import java.time.LocalDate;
import java.util.List;

public interface CommitmentService {
    Commitment createForPo(Long poId, LocalDate deliveryDate);

    Commitment requestRevision(Long commitmentId, LocalDate newDate, String reason, Long supplierUserId);

    Commitment approveRevision(Long revisionId, Long managerId);

    Commitment rejectRevision(Long revisionId, Long managerId);

    List<CommitmentRevision> history(Long commitmentId);
}