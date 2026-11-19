package com.tcs.VendorPulse.dev;

import com.tcs.VendorPulse.agreement.domain.Agreement;
import com.tcs.VendorPulse.agreement.repository.AgreementRepository;
import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.commitment.repository.PoCommitmentRepository;
import com.tcs.VendorPulse.common.enums.*;
import com.tcs.VendorPulse.delivery.domain.Delivery;
import com.tcs.VendorPulse.delivery.repository.DeliveryRepository;
import com.tcs.VendorPulse.material.domain.Material;
import com.tcs.VendorPulse.material.domain.MaterialCategory;
import com.tcs.VendorPulse.material.repository.MaterialCategoryRepository;
import com.tcs.VendorPulse.material.repository.MaterialRepository;
import com.tcs.VendorPulse.negotiation.domain.NegotiationRound;
import com.tcs.VendorPulse.negotiation.repository.NegotiationRoundRepository;
import com.tcs.VendorPulse.po.domain.PoLine;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PoLineRepository;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.scoring.service.ScoringService;
import com.tcs.VendorPulse.settings.domain.ScoringSettings;
import com.tcs.VendorPulse.settings.repository.ScoringSettingsRepository;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.domain.SupplierCategory;
import com.tcs.VendorPulse.supplier.domain.SupplierDocument;
import com.tcs.VendorPulse.supplier.repository.SupplierCategoryRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierDocumentRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.user.domain.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierCategoryRepository supplierCategoryRepository;
    private final SupplierDocumentRepository documentRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final ScoringSettingsRepository settingsRepository;
    private final PurchaseOrderRepository poRepository;
    private final PoLineRepository poLineRepository;
    private final NegotiationRoundRepository roundRepository;
    private final AgreementRepository agreementRepository;
    private final PoCommitmentRepository commitmentRepository;
    private final DeliveryRepository deliveryRepository;
    private final ScoringService scoringService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded with demo data.");
            return;
        }

        log.info("Seeding demo database with initial users, suppliers, materials, and sample orders...");

        String defaultPass = passwordEncoder.encode("Password123!");

        // 1. Settings
        if (settingsRepository.findById((short) 1).isEmpty()) {
            settingsRepository.save(ScoringSettings.builder()
                    .id((short) 1)
                    .wDeliveryReliability((short) 35)
                    .wCommitmentStability((short) 25)
                    .wDeliveryConsistency((short) 20)
                    .wQualityPerformance((short) 20)
                    .highCdiThreshold((short) 40)
                    .stockAlertHorizonDays((short) 14)
                    .maxNegotiationRounds((short) 3)
                    .defaultCutoffDays((short) 7)
                    .build());
        }

        // 2. Categories
        List<String> categoryNames = Arrays.asList(
                "Castings", "Fasteners", "Electrical", "Packaging", "Forgings", "Machined parts"
        );
        for (String cName : categoryNames) {
            if (!categoryRepository.existsByNameIgnoreCase(cName)) {
                categoryRepository.save(MaterialCategory.builder().name(cName).build());
            }
        }

        MaterialCategory catCastings = categoryRepository.findByNameIgnoreCase("Castings").orElseThrow();
        MaterialCategory catFasteners = categoryRepository.findByNameIgnoreCase("Fasteners").orElseThrow();
        MaterialCategory catElectrical = categoryRepository.findByNameIgnoreCase("Electrical").orElseThrow();
        MaterialCategory catPackaging = categoryRepository.findByNameIgnoreCase("Packaging").orElseThrow();

        // 3. Suppliers
        Supplier sup1 = Supplier.builder()
                .id(UUID.randomUUID())
                .companyName("Precision Castings Ltd")
                .registrationNo("REG-2026-001")
                .contactPerson("Rajesh Kumar")
                .email("supplier.sm@vendorpulse.com")
                .phone("+91 9876543210")
                .address("Plot 42, Hinjewadi Industrial Zone, Pune, Maharashtra 411057")
                .materialType("Metallic Castings")
                .businessUnit("Automotive Components")
                .approvalStatus(ApprovalStatus.APPROVED)
                .isActive(true)
                .build();
        sup1 = supplierRepository.save(sup1);

        Supplier sup2 = Supplier.builder()
                .id(UUID.randomUUID())
                .companyName("Apex Fasteners Inc")
                .registrationNo("REG-2026-002")
                .contactPerson("Sunil Verma")
                .email("apex@fasteners.com")
                .phone("+91 9876543211")
                .address("Sector 18, Udyog Vihar, Gurugram, Haryana 122015")
                .materialType("Hardware & Fasteners")
                .businessUnit("Industrial Fasteners")
                .approvalStatus(ApprovalStatus.APPROVED)
                .isActive(true)
                .build();
        sup2 = supplierRepository.save(sup2);

        Supplier sup3 = Supplier.builder()
                .id(UUID.randomUUID())
                .companyName("Global Packaging Corp")
                .registrationNo("REG-2026-003")
                .contactPerson("Anita Roy")
                .email("anita@globalpack.com")
                .phone("+91 9876543212")
                .address("MIDC Andheri East, Mumbai, Maharashtra 400093")
                .materialType("Industrial Packaging")
                .businessUnit("Packaging Logistics")
                .approvalStatus(ApprovalStatus.PENDING)
                .isActive(true)
                .build();
        sup3 = supplierRepository.save(sup3);

        // Map supplier categories
        supplierCategoryRepository.save(new SupplierCategory(sup1.getId(), catCastings.getId()));
        supplierCategoryRepository.save(new SupplierCategory(sup2.getId(), catFasteners.getId()));
        supplierCategoryRepository.save(new SupplierCategory(sup3.getId(), catPackaging.getId()));

        // 4. Users (3 Approved Roles)
        User pmUser = User.builder()
                .id(UUID.randomUUID())
                .userCode("PM001")
                .fullName("Priya Sharma")
                .email("admin.pm@vendorpulse.com")
                .passwordHash(defaultPass)
                .role(UserRole.PROCUREMENT_MANAGER)
                .status(AccountStatus.ACTIVE)
                .build();
        pmUser = userRepository.save(pmUser);

        User smUser = User.builder()
                .id(UUID.randomUUID())
                .userCode("SM001")
                .fullName("Rajesh Kumar")
                .email("supplier.sm@vendorpulse.com")
                .passwordHash(defaultPass)
                .role(UserRole.SUPPLIER_MANAGER)
                .status(AccountStatus.ACTIVE)
                .supplierId(sup1.getId())
                .build();
        smUser = userRepository.save(smUser);

        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .userCode("ADMIN001")
                .fullName("System Administrator")
                .email("admin@vendorpulse.com")
                .passwordHash(defaultPass)
                .role(UserRole.SYSTEM_ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();
        adminUser = userRepository.save(adminUser);

        // Supplier 3 user (PENDING)
        User pendingUser = User.builder()
                .id(UUID.randomUUID())
                .userCode("SM003")
                .fullName("Anita Roy")
                .email("anita@globalpack.com")
                .passwordHash(defaultPass)
                .role(UserRole.SUPPLIER_MANAGER)
                .status(AccountStatus.PENDING)
                .supplierId(sup3.getId())
                .build();
        userRepository.save(pendingUser);

        // Documents
        documentRepository.save(SupplierDocument.builder()
                .id(UUID.randomUUID())
                .supplierId(sup1.getId())
                .fileName("gst_certificate.pdf")
                .mimeType("application/pdf")
                .sizeBytes(245760L)
                .storageKey("documents/sample-gst.pdf")
                .reviewStatus(ApprovalStatus.APPROVED)
                .reviewedBy(pmUser.getId())
                .reviewedAt(Instant.now())
                .uploadedBy(smUser.getId())
                .build());

        // 5. Materials
        Material mat1 = Material.builder()
                .id(UUID.randomUUID())
                .code("MAT-CST-01")
                .name("Engine Block Casting Grade A")
                .categoryId(catCastings.getId())
                .isCritical(true)
                .isActive(true)
                .stockQty(new BigDecimal("150.00"))
                .dailyUsage(new BigDecimal("15.00"))
                .stockUpdatedAt(Instant.now())
                .build();
        mat1 = materialRepository.save(mat1);

        Material mat2 = Material.builder()
                .id(UUID.randomUUID())
                .code("MAT-FST-01")
                .name("High Tensile Bolt M12x50")
                .categoryId(catFasteners.getId())
                .isCritical(false)
                .isActive(true)
                .stockQty(new BigDecimal("5000.00"))
                .dailyUsage(new BigDecimal("200.00"))
                .stockUpdatedAt(Instant.now())
                .build();
        mat2 = materialRepository.save(mat2);

        Material mat3 = Material.builder()
                .id(UUID.randomUUID())
                .code("MAT-ELC-01")
                .name("Copper Wiring Harness 24V")
                .categoryId(catElectrical.getId())
                .isCritical(true)
                .isActive(true)
                .stockQty(new BigDecimal("80.00"))
                .dailyUsage(new BigDecimal("10.00"))
                .stockUpdatedAt(Instant.now())
                .build();
        materialRepository.save(mat3);

        // 6. Sample Purchase Orders
        LocalDate today = LocalDate.now();

        // PO 1: CONFIRMED with Agreement & Commitment
        PurchaseOrder po1 = PurchaseOrder.builder()
                .id(UUID.randomUUID())
                .poNumber("PO-2026-1001")
                .supplierId(sup1.getId())
                .status(PoStatus.CONFIRMED)
                .requiredDate(today.plusDays(20))
                .agreedPriceTotal(new BigDecimal("25000.00"))
                .agreedDeliveryDate(today.plusDays(20))
                .cancellationCutoffDays((short) 7)
                .createdBy(pmUser.getId())
                .build();
        po1 = poRepository.save(po1);

        poLineRepository.save(PoLine.builder()
                .id(UUID.randomUUID())
                .poId(po1.getId())
                .lineNo((short) 1)
                .materialId(mat1.getId())
                .quantity(new BigDecimal("100.00"))
                .unitPrice(new BigDecimal("250.00"))
                .requiredDate(today.plusDays(20))
                .build());

        commitmentRepository.save(PoCommitment.builder()
                .id(UUID.randomUUID())
                .poId(po1.getId())
                .seqNo((short) 0)
                .type(CommitmentType.ORIGINAL)
                .committedDate(today.plusDays(20))
                .reason("Agreed terms")
                .createdBy(smUser.getId())
                .build());

        agreementRepository.save(Agreement.builder()
                .id(UUID.randomUUID())
                .poId(po1.getId())
                .deliveryDate(today.plusDays(20))
                .priceTotal(new BigDecimal("25000.00"))
                .cutoffDays((short) 7)
                .acceptedByManufacturer(pmUser.getId())
                .acceptedByManufacturerAt(Instant.now())
                .acceptedBySupplier(smUser.getId())
                .acceptedBySupplierAt(Instant.now())
                .build());

        // PO 2, 3, 4 (completed to establish ratings for Precision Castings)
        for (int i = 1; i <= 3; i++) {
            PurchaseOrder compPo = PurchaseOrder.builder()
                    .id(UUID.randomUUID())
                    .poNumber(String.format("PO-2026-090%d", i))
                    .supplierId(sup1.getId())
                    .status(PoStatus.DELIVERED)
                    .requiredDate(today.minusDays(40 - (i * 10)))
                    .agreedPriceTotal(new BigDecimal("12500.00"))
                    .agreedDeliveryDate(today.minusDays(40 - (i * 10)))
                    .cancellationCutoffDays((short) 7)
                    .dispatchedOn(today.minusDays(42 - (i * 10)))
                    .supplierMarkedDeliveredOn(today.minusDays(40 - (i * 10)))
                    .createdBy(pmUser.getId())
                    .build();
            compPo = poRepository.save(compPo);

            poLineRepository.save(PoLine.builder()
                    .id(UUID.randomUUID())
                    .poId(compPo.getId())
                    .lineNo((short) 1)
                    .materialId(mat1.getId())
                    .quantity(new BigDecimal("50.00"))
                    .unitPrice(new BigDecimal("250.00"))
                    .requiredDate(today.minusDays(40 - (i * 10)))
                    .build());

            commitmentRepository.save(PoCommitment.builder()
                    .id(UUID.randomUUID())
                    .poId(compPo.getId())
                    .seqNo((short) 0)
                    .type(CommitmentType.ORIGINAL)
                    .committedDate(today.minusDays(40 - (i * 10)))
                    .reason("Original baseline")
                    .createdBy(smUser.getId())
                    .build());

            deliveryRepository.save(Delivery.builder()
                    .id(UUID.randomUUID())
                    .poId(compPo.getId())
                    .deliveryDate(today.minusDays(40 - (i * 10)))
                    .deliveredQty(new BigDecimal("50.00"))
                    .acceptedQty(new BigDecimal("49.00"))
                    .rejectedQty(new BigDecimal("1.00"))
                    .completion(DeliveryStatus.COMPLETE)
                    .qualityRating((short) 5)
                    .inspection(InspectionResult.PASS)
                    .remarks("Quality passed")
                    .recordedBy(pmUser.getId())
                    .build());
        }

        // PO 5: SUBMITTED (awaiting supplier action)
        PurchaseOrder poSubmitted = PurchaseOrder.builder()
                .id(UUID.randomUUID())
                .poNumber("PO-2026-1002")
                .supplierId(sup1.getId())
                .status(PoStatus.SUBMITTED)
                .requiredDate(today.plusDays(25))
                .agreedPriceTotal(new BigDecimal("50000.00"))
                .cancellationCutoffDays((short) 7)
                .awaitingParty(PartySide.SUPPLIER)
                .createdBy(pmUser.getId())
                .build();
        poSubmitted = poRepository.save(poSubmitted);

        poLineRepository.save(PoLine.builder()
                .id(UUID.randomUUID())
                .poId(poSubmitted.getId())
                .lineNo((short) 1)
                .materialId(mat1.getId())
                .quantity(new BigDecimal("200.00"))
                .unitPrice(new BigDecimal("250.00"))
                .requiredDate(today.plusDays(25))
                .build());

        roundRepository.save(NegotiationRound.builder()
                .id(UUID.randomUUID())
                .poId(poSubmitted.getId())
                .roundNo((short) 1)
                .fromParty(PartySide.MANUFACTURER)
                .action(NegotiationAction.PROPOSAL)
                .proposedDate(today.plusDays(25))
                .proposedPrice(new BigDecimal("50000.00"))
                .reason("Initial order proposal for casting parts")
                .madeBy(pmUser.getId())
                .build());

        // Calculate initial scores for demo
        scoringService.recalculateSupplier(sup1.getId());

        log.info("Demo data seeding completed!");
        log.info("Logins available:");
        log.info("  Procurement Manager: admin.pm@vendorpulse.com / Password123!");
        log.info("  Supplier Manager:    supplier.sm@vendorpulse.com / Password123!");
        log.info("  System Admin:        admin@vendorpulse.com / Password123!");
    }
}

