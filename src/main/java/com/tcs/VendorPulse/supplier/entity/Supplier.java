package com.tcs.VendorPulse.supplier.entity;

import com.tcs.VendorPulse.common.enums.SupplierStatus;
import com.tcs.VendorPulse.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false)
    private String businessName;
    @Column(nullable = false)
    private String contactPerson;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String phone;
    @Column(unique = true, nullable = false)
    private String gstin;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierStatus status = SupplierStatus.PENDING_VERIFICATION;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User u) {
        this.user = u;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String b) {
        this.businessName = b;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String c) {
        this.contactPerson = c;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String e) {
        this.email = e;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String p) {
        this.phone = p;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String g) {
        this.gstin = g;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String a) {
        this.address = a;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public void setStatus(SupplierStatus s) {
        this.status = s;
    }
}