package com.tcs.VendorPulse.procurement.entity;

import com.tcs.VendorPulse.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "commitment_revisions")
public class CommitmentRevision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "commitment_id")
    private Commitment commitment;
    @Column(nullable = false)
    private LocalDate oldDate;
    @Column(nullable = false)
    private LocalDate newDate;
    @Column(columnDefinition = "TEXT")
    private String reason;
    @ManyToOne(optional = false)
    @JoinColumn(name = "revised_by")
    private User revisedBy;
    @Column(nullable = false)
    private LocalDateTime revisedAt;

    @PrePersist
    void onCreate() {
        revisedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Commitment getCommitment() {
        return commitment;
    }

    public void setCommitment(Commitment c) {
        this.commitment = c;
    }

    public LocalDate getOldDate() {
        return oldDate;
    }

    public void setOldDate(LocalDate d) {
        this.oldDate = d;
    }

    public LocalDate getNewDate() {
        return newDate;
    }

    public void setNewDate(LocalDate d) {
        this.newDate = d;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String r) {
        this.reason = r;
    }

    public User getRevisedBy() {
        return revisedBy;
    }

    public void setRevisedBy(User u) {
        this.revisedBy = u;
    }
}