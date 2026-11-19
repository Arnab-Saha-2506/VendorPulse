package com.tcs.VendorPulse.admin.service;

import com.tcs.VendorPulse.common.enums.SupplierStatus;
import com.tcs.VendorPulse.common.enums.VerificationDecision;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.entity.SupplierDocument;
import com.tcs.VendorPulse.supplier.entity.SupplierVerification;
import com.tcs.VendorPulse.supplier.repository.SupplierDocumentRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierVerificationRepository;
import com.tcs.VendorPulse.user.entity.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    private final SupplierRepository suppliers;
    private final SupplierVerificationRepository verifications;
    private final SupplierDocumentRepository documents;
    private final UserRepository users;

    public AdminServiceImpl(SupplierRepository s,
                            SupplierVerificationRepository v,
                            SupplierDocumentRepository d,
                            UserRepository u) {
        this.suppliers = s;
        this.verifications = v;
        this.documents = d;
        this.users = u;
    }

    @Override
    public List<Supplier> pendingSuppliers() {
        return suppliers.findByStatus(SupplierStatus.PENDING_VERIFICATION);
    }

    @Override
    public Supplier getSupplier(Long id) {
        return suppliers.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Supplier not found with ID: " + id));
    }

    @Override
    @Transactional
    public Supplier approve(Long id, Long adminId, String remarks) {
        Supplier s = getSupplier(id);
        if (s.getStatus() != SupplierStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Supplier cannot be approved from status: " + s.getStatus() + ". Only PENDING_VERIFICATION is allowed.");
        }
        s.setStatus(SupplierStatus.APPROVED);
        suppliers.save(s);
        saveVerification(s, adminId, VerificationDecision.APPROVED, remarks);
        return s;
    }

    @Override
    @Transactional
    public Supplier reject(Long id, Long adminId, String remarks) {
        Supplier s = getSupplier(id);
        if (s.getStatus() != SupplierStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Supplier cannot be rejected from status: " + s.getStatus() + ". Only PENDING_VERIFICATION is allowed.");
        }
        s.setStatus(SupplierStatus.REJECTED);
        suppliers.save(s);
        saveVerification(s, adminId, VerificationDecision.REJECTED, remarks);
        return s;
    }

    @Override
    public SupplierDocument getSupplierCertificate(Long id) {
        Supplier s = getSupplier(id);
        return documents.findTopBySupplierIdAndDocTypeOrderByUploadedAtDesc(s.getId(), "GST_CERTIFICATE")
                .orElseThrow(() -> new IllegalArgumentException("No GST certificate found for supplier ID: " + id));
    }

    private void saveVerification(Supplier s, Long adminId, VerificationDecision d, String remarks) {
        User admin = users.findById(adminId).orElseThrow(
                () -> new IllegalArgumentException("Admin user not found with ID: " + adminId));
        SupplierVerification v = new SupplierVerification();
        v.setSupplier(s);
        v.setAdmin(admin);
        v.setDecision(d);
        v.setRemarks(remarks);
        verifications.save(v);
    }
}
