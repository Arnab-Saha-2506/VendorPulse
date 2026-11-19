package com.tcs.VendorPulse.procurement.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AgreementRequestDTO {
    @NotNull
    private Long supplierId;
    @NotBlank
    private String title;
    private String terms;
    @NotBlank
    private String material;
    @NotNull
    @Min(1)
    private int quantity;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal unitPrice;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal totalAmount;
    @NotNull
    private LocalDate deliveryDate;
    private String paymentTerms;

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
}
