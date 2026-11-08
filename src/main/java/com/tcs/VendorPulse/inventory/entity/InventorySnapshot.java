package com.tcs.VendorPulse.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_snapshots")
public class InventorySnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String materialCode;
    @Column(nullable = false)
    private int availableStock;
    @Column(nullable = false)
    private int safetyStock;
    @Column(nullable = false)
    private double avgDailyConsumption;
    private Integer estimatedStockoutDays;
    @Column(nullable = false)
    private LocalDateTime syncedAt;
    @Column(nullable = false)
    private boolean successful = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String m) { this.materialCode = m; }
    public int getAvailableStock() { return availableStock; }
    public void setAvailableStock(int a) { this.availableStock = a; }
    public int getSafetyStock() { return safetyStock; }
    public void setSafetyStock(int s) { this.safetyStock = s; }
    public double getAvgDailyConsumption() { return avgDailyConsumption; }
    public void setAvgDailyConsumption(double a) { this.avgDailyConsumption = a; }
    public Integer getEstimatedStockoutDays() { return estimatedStockoutDays; }
    public void setEstimatedStockoutDays(Integer e) { this.estimatedStockoutDays = e; }
    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime s) { this.syncedAt = s; }
    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean s) { this.successful = s; }
}
