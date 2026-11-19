package com.tcs.VendorPulse.commitment.repository;

import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.common.enums.CommitmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PoCommitmentRepository extends JpaRepository<PoCommitment, UUID> {
    List<PoCommitment> findByPoIdOrderBySeqNoAsc(UUID poId);
    Optional<PoCommitment> findByPoIdAndSeqNo(UUID poId, Short seqNo);
    Optional<PoCommitment> findFirstByPoIdOrderBySeqNoDesc(UUID poId);
    long countByPoIdAndType(UUID poId, CommitmentType type);
    long countByPoId(UUID poId);
}

