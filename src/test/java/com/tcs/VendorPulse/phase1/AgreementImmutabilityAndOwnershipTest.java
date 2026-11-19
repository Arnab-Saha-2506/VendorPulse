package com.tcs.VendorPulse.phase1;

import com.tcs.VendorPulse.common.enums.AgreementStatus;
import com.tcs.VendorPulse.procurement.dto.AgreementRequestDTO;
import com.tcs.VendorPulse.procurement.entity.Agreement;
import com.tcs.VendorPulse.procurement.repository.AgreementRepository;
import com.tcs.VendorPulse.procurement.service.AgreementServiceImpl;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgreementImmutabilityAndOwnershipTest {

    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private SupplierRepository supplierRepository;

    private AgreementServiceImpl agreementService;

    @BeforeEach
    void setUp() {
        agreementService = new AgreementServiceImpl(agreementRepository, supplierRepository);
    }

    @Test
    void testCreateAgreement_SetsDraftAndManagerId() {
        AgreementRequestDTO dto = new AgreementRequestDTO();
        dto.setSupplierId(100L);
        dto.setTitle("Steel Supply 2026");
        dto.setMaterial("Steel Rods");
        dto.setQuantity(500);
        dto.setUnitPrice(new BigDecimal("25.00"));
        dto.setTotalAmount(new BigDecimal("12500.00"));
        dto.setDeliveryDate(LocalDate.now().plusMonths(1));

        Supplier supplier = new Supplier();
        supplier.setId(100L);

        when(supplierRepository.findById(100L)).thenReturn(Optional.of(supplier));
        when(agreementRepository.save(any(Agreement.class))).thenAnswer(i -> i.getArgument(0));

        Agreement created = agreementService.create(dto, 42L);

        assertEquals(AgreementStatus.DRAFT, created.getStatus());
        assertEquals(42L, created.getCreatedByManagerId());
        assertEquals("Steel Supply 2026", created.getTitle());
    }

    @Test
    void testSendAgreement_DraftToSentSuccess() {
        Agreement a = new Agreement();
        a.setId(1L);
        a.setStatus(AgreementStatus.DRAFT);

        when(agreementRepository.findById(1L)).thenReturn(Optional.of(a));
        when(agreementRepository.save(any(Agreement.class))).thenAnswer(i -> i.getArgument(0));

        Agreement sent = agreementService.send(1L, 42L);

        assertEquals(AgreementStatus.SENT, sent.getStatus());
        assertEquals(42L, sent.getSentByManagerId());
    }

    @Test
    void testSendAgreement_NonDraftFails() {
        Agreement a = new Agreement();
        a.setId(1L);
        a.setStatus(AgreementStatus.ACTIVE);

        when(agreementRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThrows(IllegalStateException.class, () -> agreementService.send(1L, 42L));
    }

    @Test
    void testAcceptBySupplier_AuthorizedSupplierUser_Success() {
        User supplierUser = new User();
        supplierUser.setId(77L);

        Supplier supplier = new Supplier();
        supplier.setId(100L);
        supplier.setUser(supplierUser);

        Agreement a = new Agreement();
        a.setId(1L);
        a.setSupplier(supplier);
        a.setStatus(AgreementStatus.SENT);

        when(agreementRepository.findById(1L)).thenReturn(Optional.of(a));
        when(agreementRepository.save(any(Agreement.class))).thenAnswer(i -> i.getArgument(0));

        Agreement accepted = agreementService.acceptBySupplier(1L, 77L);

        assertEquals(AgreementStatus.ACCEPTED_BY_SUPPLIER, accepted.getStatus());
        assertNotNull(accepted.getAcceptedBySupplierAt());
    }

    @Test
    void testAcceptBySupplier_UnauthorizedSupplierUser_AccessDenied() {
        User ownerUser = new User();
        ownerUser.setId(77L);

        Supplier supplier = new Supplier();
        supplier.setId(100L);
        supplier.setUser(ownerUser);

        Agreement a = new Agreement();
        a.setId(1L);
        a.setSupplier(supplier);
        a.setStatus(AgreementStatus.SENT);

        when(agreementRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThrows(AccessDeniedException.class, () -> agreementService.acceptBySupplier(1L, 999L));
    }

    @Test
    void testFinalizeAgreement_AcceptedToActiveSuccess() {
        Agreement a = new Agreement();
        a.setId(1L);
        a.setStatus(AgreementStatus.ACCEPTED_BY_SUPPLIER);

        when(agreementRepository.findById(1L)).thenReturn(Optional.of(a));
        when(agreementRepository.save(any(Agreement.class))).thenAnswer(i -> i.getArgument(0));

        Agreement active = agreementService.finalizeAgreement(1L, 5L);

        assertEquals(AgreementStatus.ACTIVE, active.getStatus());
        assertEquals(5L, active.getFinalizedByAdminId());
        assertNotNull(active.getAcceptedByAdminAt());
    }

    @Test
    void testFinalizeAgreement_NotAcceptedFails() {
        Agreement a = new Agreement();
        a.setId(1L);
        a.setStatus(AgreementStatus.SENT);

        when(agreementRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThrows(IllegalStateException.class, () -> agreementService.finalizeAgreement(1L, 5L));
    }
}

