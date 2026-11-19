package com.tcs.VendorPulse.report.api;

import com.tcs.VendorPulse.report.dto.ReportDtos.*;
import com.tcs.VendorPulse.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Performance, commitment, and comparison reports with CSV export")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/supplier-performance")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Supplier performance report", description = "Performance metrics table or CSV download")
    public ResponseEntity<?> getSupplierPerformanceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Short categoryId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false, defaultValue = "json") String format) {

        List<SupplierPerformanceReportRow> rows = reportService.getSupplierPerformanceReport(from, to, categoryId, supplierId);

        if ("csv".equalsIgnoreCase(format)) {
            String csv = reportService.generateSupplierPerformanceCsv(rows);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"supplier-performance.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        }

        return ResponseEntity.ok(rows);
    }

    @GetMapping("/commitment-history")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Commitment history report", description = "Original vs revised dates and change reasons table or CSV")
    public ResponseEntity<?> getCommitmentHistoryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Short categoryId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false, defaultValue = "json") String format) {

        List<CommitmentHistoryReportRow> rows = reportService.getCommitmentHistoryReport(from, to, categoryId, supplierId);

        if ("csv".equalsIgnoreCase(format)) {
            String csv = reportService.generateCommitmentHistoryCsv(rows);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"commitment-history.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        }

        return ResponseEntity.ok(rows);
    }

    @GetMapping("/supplier-comparison")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_EXECUTIVE', 'SYSTEM_ADMIN')")
    @Operation(summary = "Supplier comparison report", description = "Rankings and metrics by category in JSON or CSV")
    public ResponseEntity<?> getSupplierComparisonReport(
            @RequestParam(required = false) Short categoryId,
            @RequestParam(required = false, defaultValue = "json") String format) {

        List<SupplierComparisonReportRow> rows = reportService.getSupplierComparisonReport(categoryId);

        if ("csv".equalsIgnoreCase(format)) {
            String csv = reportService.generateSupplierComparisonCsv(rows);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"supplier-comparison.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        }

        return ResponseEntity.ok(rows);
    }
}

