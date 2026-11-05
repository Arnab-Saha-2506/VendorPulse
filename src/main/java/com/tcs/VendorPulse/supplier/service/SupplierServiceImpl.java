package com.tcs.VendorPulse.supplier.service;

import com.tcs.VendorPulse.supplier.dto.SupplierRegistrationRequest;
import com.tcs.VendorPulse.supplier.entity.*;
import com.tcs.VendorPulse.supplier.repository.*;
import com.tcs.VendorPulse.user.entity.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import com.tcs.VendorPulse.common.enums.Role;
import com.tcs.VendorPulse.common.enums.SupplierStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;

@Service
public class SupplierServiceImpl implements SupplierService {
    private final UserRepository users;
    private final SupplierRepository suppliers;
    private final SupplierDocumentRepository docs;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public SupplierServiceImpl(UserRepository u, SupplierRepository s, SupplierDocumentRepository d) {
        users = u;
        suppliers = s;
        docs = d;
    }

    @Transactional
    public Supplier register(SupplierRegistrationRequest req, MultipartFile file) throws IOException {
        if (users.existsByEmail(req.getEmail()) || suppliers.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email already registered");
        if (suppliers.existsByGstin(req.getGstin()))
            throw new IllegalArgumentException("GSTIN already registered");
        User u = new User();
        u.setEmail(req.getEmail());
        u.setPasswordHash(encoder.encode(req.getPassword()));
        u.setRole(Role.SUPPLIER);
        users.save(u);
        Supplier s = new Supplier();
        s.setUser(u);
        s.setBusinessName(req.getBusinessName());
        s.setContactPerson(req.getContactPerson());
        s.setEmail(req.getEmail());
        s.setPhone(req.getPhone());
        s.setGstin(req.getGstin());
        s.setAddress(req.getAddress());
        s.setStatus(SupplierStatus.PENDING_VERIFICATION);
        suppliers.save(s);
        Path dir = Paths.get("uploads/gst");
        Files.createDirectories(dir);
        Path target = dir.resolve(s.getId() + "_" + file.getOriginalFilename());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        SupplierDocument d = new SupplierDocument();
        d.setSupplier(s);
        d.setDocType("GST_CERTIFICATE");
        d.setFilePath(target.toString());
        docs.save(d);
        return s;
    }
}