package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.procurement.entity.Invoice;

public interface InvoiceService {
    Invoice generate(Long poId, Long managerId);
    Invoice get(Long id);
    Invoice confirm(Long id, Long supplierUserId);
    Invoice finalizeInvoice(Long id, String invoiceNumber, Long supplierUserId);
    Invoice approve(Long id, Long managerId);
    Invoice reject(Long id, Long managerId, String reason);
}
