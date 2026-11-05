package com.tcs.VendorPulse.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcs.VendorPulse.procurement.entity.CommitmentRevision;

public interface CommitmentRevisionRepository extends JpaRepository<CommitmentRevision, Long> {
    List<CommitmentRevision> findByCommitmentId(Long cId);
}
