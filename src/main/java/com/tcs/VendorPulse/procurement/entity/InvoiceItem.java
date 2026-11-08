package com.tcs.VendorPulse.procurement.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="invoice_items")
public class InvoiceItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="invoice_id") private Invoice invoice;
    @Column(nullable=false) private String itemName;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal unitPrice;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal lineTotal;
    @Column(nullable=false, precision=5, scale=2) private BigDecimal taxRate = BigDecimal.ZERO;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal taxAmount = BigDecimal.ZERO;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Invoice getInvoice(){return invoice;} public void setInvoice(Invoice i){this.invoice=i;}
    public String getItemName(){return itemName;} public void setItemName(String n){this.itemName=n;}
    public int getQuantity(){return quantity;} public void setQuantity(int q){this.quantity=q;}
    public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal u){this.unitPrice=u;}
    public BigDecimal getLineTotal(){return lineTotal;} public void setLineTotal(BigDecimal l){this.lineTotal=l;}
    public BigDecimal getTaxRate(){return taxRate;} public void setTaxRate(BigDecimal r){this.taxRate=r;}
    public BigDecimal getTaxAmount(){return taxAmount;} public void setTaxAmount(BigDecimal t){this.taxAmount=t;}
}