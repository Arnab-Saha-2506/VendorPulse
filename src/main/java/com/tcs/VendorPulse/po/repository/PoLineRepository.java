package com.tcs.VendorPulse.po.repository;

import com.tcs.VendorPulse.po.domain.PoLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PoLineRepository extends JpaRepository<PoLine, UUID> {
    List<PoLine> findByPoIdOrderByLineNoAsc(UUID poId);
    void deleteByPoId(UUID poId);
}

