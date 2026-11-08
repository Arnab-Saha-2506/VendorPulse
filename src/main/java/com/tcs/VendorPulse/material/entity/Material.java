package com.tcs.VendorPulse.material.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "materials")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String code;
    @Column(nullable = false)
    private String name;
    @ManyToOne
    @JoinColumn(name = "category_id")
    private MaterialCategory category;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private String unit;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String c) { this.code = c; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public MaterialCategory getCategory() { return category; }
    public void setCategory(MaterialCategory c) { this.category = c; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getUnit() { return unit; }
    public void setUnit(String u) { this.unit = u; }
}
