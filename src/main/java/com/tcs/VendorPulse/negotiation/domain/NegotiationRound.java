package com.tcs.VendorPulse.negotiation.domain;

import com.tcs.VendorPulse.common.enums.NegotiationAction;
import com.tcs.VendorPulse.common.enums.PartySide;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "negotiation_rounds", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"po_id", "round_no"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationRound {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "po_id", nullable = false)
    private UUID poId;

    @Column(name = "round_no", nullable = false)
    private Short roundNo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "from_party", nullable = false)
    private PartySide fromParty;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action", nullable = false)
    private NegotiationAction action;

    @Column(name = "proposed_date")
    private LocalDate proposedDate;

    @Column(name = "proposed_price", precision = 14, scale = 2)
    private BigDecimal proposedPrice;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "made_by", nullable = false)
    private UUID madeBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}

