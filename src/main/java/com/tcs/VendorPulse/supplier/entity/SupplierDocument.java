package com.tcs.VendorPulse.supplier.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_documents")
public class SupplierDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    @Column(nullable = false)
    private String docType;
    @Column(nullable = false)
    private String filePath;
    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    void onCreate() {
        uploadedAt = LocalDateTime.now();
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

    public String getDocType() {
        return docType;
    }

    public void setDocType(String d) {
        this.docType = d;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String f) {
        this.filePath = f;
    }
}