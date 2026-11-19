package com.tcs.VendorPulse.phase1;

import com.tcs.VendorPulse.common.enums.Role;
import com.tcs.VendorPulse.common.enums.SupplierStatus;
import com.tcs.VendorPulse.supplier.dto.SupplierRegistrationRequest;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.entity.SupplierDocument;
import com.tcs.VendorPulse.supplier.repository.SupplierDocumentRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.supplier.service.SupplierServiceImpl;
import com.tcs.VendorPulse.user.entity.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierRegistrationAndDocumentSecurityTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private SupplierDocumentRepository documentRepository;

    private SupplierServiceImpl supplierService;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        supplierService = new SupplierServiceImpl(userRepository, supplierRepository, documentRepository, encoder);
    }

    private SupplierRegistrationRequest createValidRequest() {
        SupplierRegistrationRequest req = new SupplierRegistrationRequest();
        req.setEmail("supplier@example.com");
        req.setPassword("SecretPass123!");
        req.setBusinessName("Acme Supplies Pvt Ltd");
        req.setContactPerson("John Doe");
        req.setPhone("9876543210");
        req.setGstin("27AAPFU0939F1ZV");
        req.setAddress("123 Industrial Area, Mumbai");
        return req;
    }

    @Test
    void testRegisterSupplier_Success() throws IOException {
        SupplierRegistrationRequest req = createValidRequest();
        MockMultipartFile validPdf = new MockMultipartFile(
                "gstCertificate", "gst_certificate.pdf", "application/pdf", "%PDF-1.4 test certificate".getBytes());

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(supplierRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(supplierRepository.existsByGstin(req.getGstin())).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(i -> {
            Supplier s = i.getArgument(0);
            s.setId(10L);
            return s;
        });

        Supplier registered = supplierService.register(req, validPdf);

        assertNotNull(registered);
        assertEquals(SupplierStatus.PENDING_VERIFICATION, registered.getStatus());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("supplier@example.com", savedUser.getEmail());
        assertEquals(Role.SUPPLIER, savedUser.getRole());
        assertTrue(encoder.matches("SecretPass123!", savedUser.getPasswordHash()));

        ArgumentCaptor<SupplierDocument> docCaptor = ArgumentCaptor.forClass(SupplierDocument.class);
        verify(documentRepository).save(docCaptor.capture());
        SupplierDocument doc = docCaptor.getValue();
        assertEquals("GST_CERTIFICATE", doc.getDocType());
        assertTrue(doc.getFilePath().endsWith(".pdf"));
        assertFalse(doc.getFilePath().contains("..")); // No path traversal
    }

    @Test
    void testRegisterSupplier_RejectsInvalidFileType() {
        SupplierRegistrationRequest req = createValidRequest();
        MockMultipartFile executable = new MockMultipartFile(
                "gstCertificate", "malicious.exe", "application/x-msdownload", "bad".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> supplierService.register(req, executable));
        assertTrue(ex.getMessage().contains("Unsupported file content type"));
    }

    @Test
    void testRegisterSupplier_RejectsOversizedFile() {
        SupplierRegistrationRequest req = createValidRequest();
        byte[] oversized = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "gstCertificate", "large.pdf", "application/pdf", oversized);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> supplierService.register(req, file));
        assertTrue(ex.getMessage().contains("File size exceeds 5MB limit"));
    }
}

