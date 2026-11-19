package com.tcs.VendorPulse.supplier.service;

import com.tcs.VendorPulse.common.enums.Role;
import com.tcs.VendorPulse.common.enums.SupplierStatus;
import com.tcs.VendorPulse.supplier.dto.SupplierRegistrationRequest;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.entity.SupplierDocument;
import com.tcs.VendorPulse.supplier.repository.SupplierDocumentRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.user.entity.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class SupplierServiceImpl implements SupplierService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".jpg", ".jpeg", ".png");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf", "image/jpeg", "image/png", "image/jpg"
    );

    private final UserRepository users;
    private final SupplierRepository suppliers;
    private final SupplierDocumentRepository docs;
    private final BCryptPasswordEncoder encoder;

    public SupplierServiceImpl(UserRepository u, SupplierRepository s, SupplierDocumentRepository d, BCryptPasswordEncoder enc) {
        this.users = u;
        this.suppliers = s;
        this.docs = d;
        this.encoder = enc;
    }

    @Override
    @Transactional
    public Supplier register(SupplierRegistrationRequest req, MultipartFile file) throws IOException {
        if (users.existsByEmail(req.getEmail()) || suppliers.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + req.getEmail());
        }
        if (suppliers.existsByGstin(req.getGstin())) {
            throw new IllegalArgumentException("GSTIN already registered: " + req.getGstin());
        }

        validateCertificateFile(file);

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

        String orig = file.getOriginalFilename();
        String ext = "";
        if (orig != null && orig.lastIndexOf('.') != -1) {
            ext = orig.substring(orig.lastIndexOf('.')).toLowerCase();
        }

        String safeFileName = "gst_" + s.getId() + "_" + UUID.randomUUID() + ext;
        Path uploadDir = Paths.get("uploads", "gst").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        Path targetPath = uploadDir.resolve(safeFileName).normalize();

        if (!targetPath.startsWith(uploadDir)) {
            throw new SecurityException("Potential path traversal attempt detected");
        }

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        SupplierDocument d = new SupplierDocument();
        d.setSupplier(s);
        d.setDocType("GST_CERTIFICATE");
        d.setFilePath(targetPath.toString());
        docs.save(d);

        return s;
    }

    @Override
    public SupplierDocument getSupplierCertificate(Long supplierId) {
        if (!suppliers.existsById(supplierId)) {
            throw new IllegalArgumentException("Supplier not found with ID: " + supplierId);
        }
        return docs.findTopBySupplierIdAndDocTypeOrderByUploadedAtDesc(supplierId, "GST_CERTIFICATE")
                .orElseThrow(() -> new IllegalArgumentException("No GST certificate found for supplier ID: " + supplierId));
    }

    private void validateCertificateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("GST certificate file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file content type: " + contentType + ". Allowed: PDF, JPG, PNG");
        }
        String orig = file.getOriginalFilename();
        if (orig == null || orig.lastIndexOf('.') == -1) {
            throw new IllegalArgumentException("Invalid file name: missing extension");
        }
        String ext = orig.substring(orig.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file extension: " + ext + ". Allowed: " + ALLOWED_EXTENSIONS);
        }
    }
}
