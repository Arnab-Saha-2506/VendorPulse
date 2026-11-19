package com.tcs.VendorPulse.phase1;

import com.tcs.VendorPulse.admin.service.AdminServiceImpl;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminVerificationTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private SupplierVerificationRepository verificationRepository;
    @Mock
    private SupplierDocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(supplierRepository, verificationRepository, documentRepository, userRepository);
    }

    @Test
    void testApprovePendingSupplier_Success() {
        Supplier s = new Supplier();
        s.setId(10L);
        s.setStatus(SupplierStatus.PENDING_VERIFICATION);

        User admin = new User();
        admin.setId(1L);

        when(supplierRepository.findById(10L)).thenReturn(Optional.of(s));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = adminService.approve(10L, 1L, "Documents verified");

        assertEquals(SupplierStatus.APPROVED, result.getStatus());
        ArgumentCaptor<SupplierVerification> captor = ArgumentCaptor.forClass(SupplierVerification.class);
        verify(verificationRepository).save(captor.capture());
        assertEquals(VerificationDecision.APPROVED, captor.getValue().getDecision());
        assertEquals("Documents verified", captor.getValue().getRemarks());
        assertEquals(admin, captor.getValue().getAdmin());
    }

    @Test
    void testApproveAlreadyApprovedSupplier_ThrowsException() {
        Supplier s = new Supplier();
        s.setId(10L);
        s.setStatus(SupplierStatus.APPROVED);

        when(supplierRepository.findById(10L)).thenReturn(Optional.of(s));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adminService.approve(10L, 1L, "Approve again"));
        assertTrue(ex.getMessage().contains("Only PENDING_VERIFICATION is allowed"));
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void testRejectPendingSupplier_Success() {
        Supplier s = new Supplier();
        s.setId(10L);
        s.setStatus(SupplierStatus.PENDING_VERIFICATION);

        User admin = new User();
        admin.setId(1L);

        when(supplierRepository.findById(10L)).thenReturn(Optional.of(s));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = adminService.reject(10L, 1L, "GSTIN mismatch");

        assertEquals(SupplierStatus.REJECTED, result.getStatus());
        ArgumentCaptor<SupplierVerification> captor = ArgumentCaptor.forClass(SupplierVerification.class);
        verify(verificationRepository).save(captor.capture());
        assertEquals(VerificationDecision.REJECTED, captor.getValue().getDecision());
        assertEquals("GSTIN mismatch", captor.getValue().getRemarks());
    }

    @Test
    void testRejectAlreadyRejectedSupplier_ThrowsException() {
        Supplier s = new Supplier();
        s.setId(10L);
        s.setStatus(SupplierStatus.REJECTED);

        when(supplierRepository.findById(10L)).thenReturn(Optional.of(s));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adminService.reject(10L, 1L, "Reject again"));
        assertTrue(ex.getMessage().contains("Only PENDING_VERIFICATION is allowed"));
        verify(supplierRepository, never()).save(any());
    }
}

