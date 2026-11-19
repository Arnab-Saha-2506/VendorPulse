package com.tcs.VendorPulse.negotiation.repository;

import com.tcs.VendorPulse.negotiation.domain.NegotiationRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NegotiationRoundRepository extends JpaRepository<NegotiationRound, UUID> {
    List<NegotiationRound> findByPoIdOrderByRoundNoAsc(UUID poId);
    Optional<NegotiationRound> findFirstByPoIdOrderByRoundNoDesc(UUID poId);
}

