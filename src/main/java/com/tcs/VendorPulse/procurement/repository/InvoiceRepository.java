package com.tcs.VendorPulse.procurement.repository;

import com.tcs.VendorPulse.procurement.entity.Invoice;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPurchaseOrderId(Long poId);
}
