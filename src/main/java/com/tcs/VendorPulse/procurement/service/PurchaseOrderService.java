package com.tcs.VendorPulse.procurement.service;
import com.tcs.VendorPulse.procurement.entity.PurchaseOrder;
import com.tcs.VendorPulse.common.enums.PoStatus;
public interface PurchaseOrderService {
    PurchaseOrder generateFromAgreement(Long agreementId);
    PurchaseOrder get(Long id);
    PurchaseOrder updateStatus(Long id, PoStatus newStatus);
}