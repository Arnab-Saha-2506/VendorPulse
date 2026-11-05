package com.tcs.VendorPulse.admin.service;

import com.tcs.VendorPulse.supplier.entity.*;
import com.tcs.VendorPulse.supplier.repository.*;
import com.tcs.VendorPulse.user.entity.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import com.tcs.VendorPulse.common.enums.SupplierStatus;
import com.tcs.VendorPulse.common.enums.VerificationDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    private final SupplierRepository suppliers;
    private final SupplierVerificationRepository verifications;
    private final UserRepository users;

    public AdminServiceImpl(SupplierRepository s, SupplierVerificationRepository v, UserRepository u) {
        suppliers = s;
        verifications = v;
        users = u;
    }

    public List<Supplier> pendingSuppliers() {
        return suppliers.findByStatus(SupplierStatus.PENDING_VERIFICATION);
    }

    public Supplier getSupplier(Long id) {
        return suppliers.findById(id).orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    @Transactional
    public Supplier approve(Long id, Long adminId, String remarks) {
        Supplier s = getSupplier(id);
        s.setStatus(SupplierStatus.APPROVED);
        suppliers.save(s);
        saveVerification(s, adminId, VerificationDecision.APPROVED, remarks);
        return s;
    }

    @Transactional
    public Supplier reject(Long id, Long adminId, String remarks) {
        Supplier s = getSupplier(id);
        s.setStatus(SupplierStatus.REJECTED);
        suppliers.save(s);
        saveVerification(s, adminId, VerificationDecision.REJECTED, remarks);
        return s;
    }

    private void saveVerification(Supplier s, Long adminId, VerificationDecision d, String remarks) {
        User admin = users.findById(adminId).orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        SupplierVerification v = new SupplierVerification();
        v.setSupplier(s);
        v.setAdmin(admin);
        v.setDecision(d);
        v.setRemarks(remarks);
        verifications.save(v);
    }
}