package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.procurement.entity.Commitment;
import com.tcs.VendorPulse.procurement.repository.CommitmentRepository;
import com.tcs.VendorPulse.common.enums.CommitmentStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class CommitmentBreachScheduler {
    private final CommitmentRepository commitments;

    public CommitmentBreachScheduler(CommitmentRepository c) { commitments = c; }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void detectBreaches() {
        LocalDate today = LocalDate.now();
        List<Commitment> all = commitments.findAll();
        for (Commitment c : all) {
            if (c.getStatus() != CommitmentStatus.BREACHED
                    && c.getCommittedDeliveryDate().isBefore(today)) {
                c.setStatus(CommitmentStatus.BREACHED);
                commitments.save(c);
            }
        }
    }
}
