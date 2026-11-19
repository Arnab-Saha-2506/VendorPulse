package com.tcs.VendorPulse.agreement.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tcs.VendorPulse.agreement.domain.Agreement;
import com.tcs.VendorPulse.agreement.dto.AgreementDto;
import com.tcs.VendorPulse.agreement.repository.AgreementRepository;
import com.tcs.VendorPulse.commitment.domain.PoCommitment;
import com.tcs.VendorPulse.commitment.repository.PoCommitmentRepository;
import com.tcs.VendorPulse.common.enums.CommitmentType;
import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.storage.FileStorageService;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.user.domain.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementService {

    private final AgreementRepository agreementRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoCommitmentRepository commitmentRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public Agreement createAgreementOnAcceptance(
            PurchaseOrder po,
            LocalDate deliveryDate,
            BigDecimal priceTotal,
            UUID acceptedByManufacturer,
            Instant acceptedByManufacturerAt,
            UUID acceptedBySupplier,
            Instant acceptedBySupplierAt) {

        if (agreementRepository.existsByPoId(po.getId())) {
            return agreementRepository.findByPoId(po.getId()).get();
        }

        try {
            Agreement agreement = Agreement.builder()
                    .id(UUID.randomUUID())
                    .poId(po.getId())
                    .deliveryDate(deliveryDate)
                    .priceTotal(priceTotal)
                    .cutoffDays(po.getCancellationCutoffDays())
                    .acceptedByManufacturer(acceptedByManufacturer)
                    .acceptedByManufacturerAt(acceptedByManufacturerAt)
                    .acceptedBySupplier(acceptedBySupplier)
                    .acceptedBySupplierAt(acceptedBySupplierAt)
                    .build();

            agreement = agreementRepository.save(agreement);

            // Create original commitment (seq_no = 0)
            if (commitmentRepository.findByPoIdAndSeqNo(po.getId(), (short) 0).isEmpty()) {
                PoCommitment originalCommitment = PoCommitment.builder()
                        .id(UUID.randomUUID())
                        .poId(po.getId())
                        .seqNo((short) 0)
                        .type(CommitmentType.ORIGINAL)
                        .committedDate(deliveryDate)
                        .reason("Agreed terms on negotiation acceptance")
                        .createdBy(acceptedBySupplier)
                        .build();
                commitmentRepository.save(originalCommitment);
            }

            // Update PO status to CONFIRMED
            po.setStatus(PoStatus.CONFIRMED);
            po.setAgreedDeliveryDate(deliveryDate);
            po.setAgreedPriceTotal(priceTotal);
            po.setAwaitingParty(null);
            purchaseOrderRepository.save(po);

            // Generate agreement PDF
            try {
                byte[] pdfBytes = generateAgreementPdfBytes(agreement, po);
                String storageKey = fileStorageService.storeAgreementPdf(po.getPoNumber(), pdfBytes);
                agreement.setPdfStorageKey(storageKey);
                agreement = agreementRepository.save(agreement);
            } catch (Exception e) {
                log.warn("Failed to generate PDF immediately for PO {}: {}", po.getPoNumber(), e.getMessage());
            }

            return agreement;
        } catch (Exception e) {
            log.error("Failed to create agreement for PO {}: {}", po.getId(), e.getMessage(), e);
            throw new ApiException(ErrorCode.AGREEMENT_GENERATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public AgreementDto getAgreementByPoId(UUID poId) {
        Agreement agreement = agreementRepository.findByPoId(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Agreement not found for this purchase order"));

        PurchaseOrder po = purchaseOrderRepository.findById(poId).orElse(null);
        String poNumber = po != null ? po.getPoNumber() : "";

        String mfgName = userRepository.findById(agreement.getAcceptedByManufacturer())
                .map(User::getFullName).orElse("Manufacturer");
        String supName = userRepository.findById(agreement.getAcceptedBySupplier())
                .map(User::getFullName).orElse("Supplier");

        return AgreementDto.builder()
                .id(agreement.getId())
                .poId(agreement.getPoId())
                .poNumber(poNumber)
                .deliveryDate(agreement.getDeliveryDate())
                .priceTotal(agreement.getPriceTotal())
                .cutoffDays(agreement.getCutoffDays())
                .acceptedByManufacturer(agreement.getAcceptedByManufacturer())
                .manufacturerName(mfgName)
                .acceptedByManufacturerAt(agreement.getAcceptedByManufacturerAt())
                .acceptedBySupplier(agreement.getAcceptedBySupplier())
                .supplierName(supName)
                .acceptedBySupplierAt(agreement.getAcceptedBySupplierAt())
                .pdfStorageKey(agreement.getPdfStorageKey())
                .createdAt(agreement.getCreatedAt())
                .build();
    }

    public byte[] getAgreementPdf(UUID poId) {
        Agreement agreement = agreementRepository.findByPoId(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Agreement not found for this purchase order"));

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        return generateAgreementPdfBytes(agreement, po);
    }

    private byte[] generateAgreementPdfBytes(Agreement agreement, PurchaseOrder po) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

            Paragraph title = new Paragraph("VENDORPULSE PURCHASE AGREEMENT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Paragraph subtitle = new Paragraph("Binding Terms for Order " + po.getPoNumber(), subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(25);
            document.add(subtitle);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(20f);

            Supplier supplier = supplierRepository.findById(po.getSupplierId()).orElse(null);
            String supplierName = supplier != null ? supplier.getCompanyName() : "Supplier";

            addTableRow(table, "Purchase Order Number:", po.getPoNumber(), boldFont, textFont);
            addTableRow(table, "Supplier Organization:", supplierName, boldFont, textFont);
            addTableRow(table, "Agreed Total Price:", "$" + agreement.getPriceTotal().toPlainString(), boldFont, textFont);
            addTableRow(table, "Agreed Delivery Date:", agreement.getDeliveryDate().toString(), boldFont, textFont);
            addTableRow(table, "Cancellation Cutoff Days:", agreement.getCutoffDays() + " days before delivery", boldFont, textFont);
            addTableRow(table, "Agreement Executed Date:", agreement.getCreatedAt().toString(), boldFont, textFont);

            document.add(table);

            Paragraph termsTitle = new Paragraph("Terms & Conditions Summary", subtitleFont);
            termsTitle.setSpacingAfter(10);
            document.add(termsTitle);

            Paragraph terms = new Paragraph(
                    "1. This agreement is final and legally binding on both parties upon digital acceptance.\n" +
                    "2. The agreed delivery date establishes the baseline commitment (Seq 0) in the vendor rating system.\n" +
                    "3. Any revisions requested by the supplier after agreement must be submitted with valid reasons.\n" +
                    "4. Manufacturer may cancel the order only prior to the cutoff threshold and before dispatch.\n" +
                    "5. Quality inspection results will directly determine supplier score history.",
                    textFont
            );
            terms.setSpacingAfter(30);
            document.add(terms);

            Paragraph sigTitle = new Paragraph("Digital Signatures and Acceptance Records", subtitleFont);
            sigTitle.setSpacingAfter(10);
            document.add(sigTitle);

            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);

            String mfgAcceptor = userRepository.findById(agreement.getAcceptedByManufacturer()).map(User::getFullName).orElse("Manufacturer");
            String supAcceptor = userRepository.findById(agreement.getAcceptedBySupplier()).map(User::getFullName).orElse("Supplier");

            addTableRow(sigTable, "Manufacturer Accepted By:", mfgAcceptor + "\nTime: " + agreement.getAcceptedByManufacturerAt(), boldFont, textFont);
            addTableRow(sigTable, "Supplier Accepted By:", supAcceptor + "\nTime: " + agreement.getAcceptedBySupplierAt(), boldFont, textFont);

            document.add(sigTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF: {}", e.getMessage(), e);
            throw new ApiException(ErrorCode.AGREEMENT_GENERATION_FAILED);
        }
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setPadding(8);
        c1.setBackgroundColor(new Color(245, 245, 245));
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valFont));
        c2.setPadding(8);
        table.addCell(c2);
    }
}

