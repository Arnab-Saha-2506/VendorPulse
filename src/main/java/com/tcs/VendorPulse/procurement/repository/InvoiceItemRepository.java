package com.tcs.VendorPulse.procurement.repository;

import com.tcs.VendorPulse.procurement.entity.InvoiceItem;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    List<InvoiceItem> findByInvoiceId(Long invoiceId);
}
