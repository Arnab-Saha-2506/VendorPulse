package com.tcs.VendorPulse.message.repository;

import com.tcs.VendorPulse.common.enums.PartySide;
import com.tcs.VendorPulse.message.domain.PoMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PoMessageRepository extends JpaRepository<PoMessage, UUID> {
    List<PoMessage> findByPoIdOrderByCreatedAtAsc(UUID poId);

    @Query("SELECT m FROM PoMessage m WHERE m.poId = :poId AND (:afterCreatedAt IS NULL OR m.createdAt > :afterCreatedAt) ORDER BY m.createdAt ASC")
    List<PoMessage> findMessagesAfter(@Param("poId") UUID poId, @Param("afterCreatedAt") Instant afterCreatedAt);

    long countByPoIdAndSenderSideNotAndReadAtIsNull(UUID poId, PartySide side);

    @Modifying
    @Query("UPDATE PoMessage m SET m.readAt = :readAt WHERE m.poId = :poId AND m.senderSide <> :callerSide AND m.readAt IS NULL")
    void markOtherSideMessagesAsRead(@Param("poId") UUID poId, @Param("callerSide") PartySide callerSide, @Param("readAt") Instant readAt);
}

