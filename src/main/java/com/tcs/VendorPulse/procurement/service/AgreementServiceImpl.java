package com.tcs.VendorPulse.procurement.service;
import com.tcs.VendorPulse.procurement.dto.AgreementRequestDTO;
import com.tcs.VendorPulse.procurement.entity.Agreement;
import com.tcs.VendorPulse.procurement.repository.AgreementRepository;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.common.enums.AgreementStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class AgreementServiceImpl implements AgreementService {
    private final AgreementRepository agreements;
    private final SupplierRepository suppliers;
    public AgreementServiceImpl(AgreementRepository a, SupplierRepository s){ agreements=a; suppliers=s; }
    @Transactional
    public Agreement create(AgreementRequestDTO req, Long managerId) {
        Supplier sup = suppliers.findById(req.getSupplierId()).orElseThrow();
        Agreement a = new Agreement();
        a.setSupplier(sup); a.setTitle(req.getTitle()); a.setTerms(req.getTerms());
        a.setPrice(req.getPrice()); a.setCurrency(req.getCurrency());
        a.setStartDate(req.getStartDate()); a.setEndDate(req.getEndDate());
        a.setStatus(AgreementStatus.DRAFT);
        return agreements.save(a);
    }
    public Agreement get(Long id){ return agreements.findById(id).orElseThrow(); }
    @Transactional
    public Agreement acceptBySupplier(Long id, Long supplierUserId) {
        Agreement a = get(id);
        if (a.getStatus() != AgreementStatus.SENT) throw new IllegalStateException("Agreement not in SENT state");
        a.setStatus(AgreementStatus.ACCEPTED_BY_SUPPLIER);
        a.setAcceptedBySupplierAt(LocalDateTime.now());
        return agreements.save(a);
    }
    @Transactional
    public Agreement finalizeAgreement(Long id, Long adminId) {
        Agreement a = get(id);
        if (a.getStatus() != AgreementStatus.ACCEPTED_BY_SUPPLIER) throw new IllegalStateException("Supplier must accept first");
        a.setStatus(AgreementStatus.ACTIVE);
        a.setAcceptedByAdminAt(LocalDateTime.now());
        return agreements.save(a);
    }
    public List<Agreement> bySupplier(Long supplierId){ return agreements.findBySupplierId(supplierId); }

    @Transactional
    public Agreement send(Long id, Long managerId) {
        Agreement a = get(id);
        if (a.getStatus() != AgreementStatus.DRAFT) throw new IllegalStateException("Only DRAFT can be sent");
        a.setStatus(AgreementStatus.SENT);
        return agreements.save(a);
    }
}