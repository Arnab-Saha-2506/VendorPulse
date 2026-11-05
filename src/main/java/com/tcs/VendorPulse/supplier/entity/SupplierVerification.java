package com.tcs.VendorPulse.supplier.entity;

import com.tcs.VendorPulse.common.enums.VerificationDecision;
import com.tcs.VendorPulse.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_verification")
public class SupplierVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    @ManyToOne(optional = false)
    @JoinColumn(name = "admin_id")
    private User admin;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationDecision decision;
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Column(nullable = false)
    private LocalDateTime decidedAt;

    @PrePersist
    void onCreate() {
        decidedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier s) {
        this.supplier = s;
    }

    public User getAdmin() {
        return admin;
    }

    public void setAdmin(User a) {
        this.admin = a;
    }

    public VerificationDecision getDecision() {
        return decision;
    }

    public void setDecision(VerificationDecision d) {
        this.decision = d;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String r) {
        this.remarks = r;
    }
}