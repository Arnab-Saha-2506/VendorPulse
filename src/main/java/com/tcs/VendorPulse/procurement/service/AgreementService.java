package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.procurement.dto.AgreementRequestDTO;
import com.tcs.VendorPulse.procurement.entity.Agreement;

import java.util.List;

public interface AgreementService {
    Agreement create(AgreementRequestDTO req, Long managerId);
    Agreement get(Long id);
    Agreement send(Long id, Long managerId);
    Agreement acceptBySupplier(Long id, Long supplierUserId);
    Agreement finalizeAgreement(Long id, Long adminId);
    List<Agreement> bySupplier(Long supplierId);
    List<Agreement> getAll();
}
