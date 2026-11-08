package com.tcs.VendorPulse.inventory.entity;

import com.tcs.VendorPulse.common.enums.AlertSeverity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_alerts")
public class RiskAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String materialCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;
    @Column(nullable = false)
    private boolean active = true;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String m) { this.materialCode = m; }
    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity s) { this.severity = s; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime r) { this.resolvedAt = r; }
}
