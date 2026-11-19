package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.common.enums.AgreementStatus;
import com.tcs.VendorPulse.procurement.dto.AgreementRequestDTO;
import com.tcs.VendorPulse.procurement.entity.Agreement;
import com.tcs.VendorPulse.procurement.repository.AgreementRepository;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgreementServiceImpl implements AgreementService {
    private final AgreementRepository agreements;
    private final SupplierRepository suppliers;

    public AgreementServiceImpl(AgreementRepository a, SupplierRepository s) {
        this.agreements = a;
        this.suppliers = s;
    }

    @Override
    @Transactional
    public Agreement create(AgreementRequestDTO req, Long managerId) {
        Supplier sup = suppliers.findById(req.getSupplierId()).orElseThrow(
                () -> new IllegalArgumentException("Supplier not found with ID: " + req.getSupplierId()));
        Agreement a = new Agreement();
        a.setSupplier(sup);
        a.setTitle(req.getTitle());
        a.setTerms(req.getTerms());
        a.setMaterial(req.getMaterial());
        a.setQuantity(req.getQuantity());
        a.setUnitPrice(req.getUnitPrice());
        a.setTotalAmount(req.getTotalAmount());
        a.setDeliveryDate(req.getDeliveryDate());
        a.setPaymentTerms(req.getPaymentTerms());
        a.setStatus(AgreementStatus.DRAFT);
        a.setCreatedByManagerId(managerId);
        return agreements.save(a);
    }

    @Override
    public Agreement get(Long id) {
        return agreements.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Agreement not found with ID: " + id));
    }

    @Override
    public List<Agreement> getAll() {
        return agreements.findAll();
    }

    @Override
    @Transactional
    public Agreement send(Long id, Long managerId) {
        Agreement a = get(id);
        if (a.getStatus() != AgreementStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT agreement can be sent. Current status: " + a.getStatus());
        }
        a.setStatus(AgreementStatus.SENT);
        a.setSentByManagerId(managerId);
        return agreements.save(a);
    }

    @Override
    @Transactional
    public Agreement acceptBySupplier(Long id, Long supplierUserId) {
        Agreement a = get(id);
        if (a.getStatus() != AgreementStatus.SENT) {
            throw new IllegalStateException("Agreement is not in SENT state. Current status: " + a.getStatus());
        }
        if (a.getSupplier().getUser() == null || !a.getSupplier().getUser().getId().equals(supplierUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Supplier user is not authorized to accept this agreement");
        }
        a.setStatus(AgreementStatus.ACCEPTED_BY_SUPPLIER);
        a.setAcceptedBySupplierAt(LocalDateTime.now());
        return agreements.save(a);
    }

    @Override
    @Transactional
    public Agreement finalizeAgreement(Long id, Long adminId) {
        Agreement a = get(id);
        if (a.getStatus() != AgreementStatus.ACCEPTED_BY_SUPPLIER) {
            throw new IllegalStateException("Supplier must accept the agreement first. Current status: " + a.getStatus());
        }
        a.setStatus(AgreementStatus.ACTIVE);
        a.setAcceptedByAdminAt(LocalDateTime.now());
        a.setFinalizedByAdminId(adminId);
        return agreements.save(a);
    }

    @Override
    public List<Agreement> bySupplier(Long supplierId) {
        return agreements.findBySupplierId(supplierId);
    }
}
