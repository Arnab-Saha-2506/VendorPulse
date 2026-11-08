package com.tcs.VendorPulse.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_sync_logs")
public class InventorySyncLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String materialCode;
    @Column(nullable = false)
    private boolean success;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Column(nullable = false)
    private LocalDateTime syncedAt;
    @PrePersist
    void onCreate() { syncedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String m) { this.materialCode = m; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean s) { this.success = s; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
}
