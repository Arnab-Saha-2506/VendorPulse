package com.tcs.VendorPulse.compare.service;

import com.tcs.VendorPulse.common.enums.ApprovalStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.compare.dto.CompareDtos.*;
import com.tcs.VendorPulse.material.domain.MaterialCategory;
import com.tcs.VendorPulse.material.repository.MaterialCategoryRepository;
import com.tcs.VendorPulse.scoring.domain.SupplierScoreHistory;
import com.tcs.VendorPulse.scoring.repository.SupplierScoreHistoryRepository;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.domain.SupplierCategory;
import com.tcs.VendorPulse.supplier.repository.SupplierCategoryRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final SupplierRepository supplierRepository;
    private final SupplierCategoryRepository supplierCategoryRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final SupplierScoreHistoryRepository scoreHistoryRepository;

    @Transactional(readOnly = true)
    public CompareResponse compareCategorySuppliers(Short categoryId) {
        MaterialCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Category not found"));

        List<SupplierCategory> mappings = supplierCategoryRepository.findByCategoryId(categoryId);
        if (mappings.isEmpty()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "No eligible suppliers mapped to this category");
        }

        List<UUID> supplierIds = mappings.stream().map(SupplierCategory::getSupplierId).collect(Collectors.toList());
        List<Supplier> activeSuppliers = supplierRepository.findAllById(supplierIds).stream()
                .filter(s -> s.getApprovalStatus() == ApprovalStatus.APPROVED && Boolean.TRUE.equals(s.getIsActive()))
                .collect(Collectors.toList());

        if (activeSuppliers.isEmpty()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "No active approved suppliers found for this category");
        }

        List<SupplierRankItem> items = new ArrayList<>();

        for (Supplier s : activeSuppliers) {
            Optional<SupplierScoreHistory> shOpt = scoreHistoryRepository.findFirstBySupplierIdOrderByPeriodDesc(s.getId());

            var builder = SupplierRankItem.builder()
                    .supplierId(s.getId())
                    .supplierName(s.getCompanyName());

            if (shOpt.isPresent() && shOpt.get().getOrdersCounted() >= 3) {
                SupplierScoreHistory sh = shOpt.get();
                builder.overallScore(sh.getOverallScore())
                        .deliveryReliability(sh.getDeliveryReliability())
                        .commitmentStability(sh.getCommitmentStability())
                        .deliveryConsistency(sh.getDeliveryConsistency())
                        .qualityPerformance(sh.getQualityPerformance())
                        .cdi(sh.getCdi())
                        .isRated(true)
                        .ratingLabel(String.format("%.1f", sh.getOverallScore()));
            } else {
                builder.isRated(false)
                        .ratingLabel("Not rated");
            }
            items.add(builder.build());
        }

        // Rank by overall score desc, CDI asc, quality desc (Not rated last) - BR-20, BR-23
        items.sort((a, b) -> {
            if (!a.isRated() && !b.isRated()) return 0;
            if (!a.isRated()) return 1;
            if (!b.isRated()) return -1;

            int comp = b.getOverallScore().compareTo(a.getOverallScore());
            if (comp != 0) return comp;

            comp = a.getCdi().compareTo(b.getCdi()); // lower CDI is better
            if (comp != 0) return comp;

            return b.getQualityPerformance().compareTo(a.getQualityPerformance());
        });

        int rank = 1;
        for (SupplierRankItem item : items) {
            item.setRank(rank++);
        }

        RecommendedSupplierDto recommended = null;
        SupplierRankItem top = items.stream().filter(SupplierRankItem::isRated).findFirst().orElse(null);

        if (top != null) {
            List<String> reasons = new ArrayList<>();
            if (top.getDeliveryReliability() != null && top.getDeliveryReliability().compareTo(BigDecimal.valueOf(90)) >= 0) {
                reasons.add(String.format("Highest delivery reliability (DRS: %.1f%%)", top.getDeliveryReliability().doubleValue()));
            }
            if (top.getCdi() != null && top.getCdi().compareTo(BigDecimal.valueOf(10)) <= 0) {
                reasons.add(String.format("Lowest commitment drift (CDI: %.1f%%)", top.getCdi().doubleValue()));
            }
            if (top.getQualityPerformance() != null && top.getQualityPerformance().compareTo(BigDecimal.valueOf(85)) >= 0) {
                reasons.add(String.format("Superior quality performance (QPS: %.1f%%)", top.getQualityPerformance().doubleValue()));
            }
            if (reasons.isEmpty()) {
                reasons.add("Highest overall composite rating in category (" + top.getOverallScore() + ")");
            }

            recommended = RecommendedSupplierDto.builder()
                    .supplierId(top.getSupplierId())
                    .supplierName(top.getSupplierName())
                    .overallScore(top.getOverallScore())
                    .cdi(top.getCdi())
                    .reasons(reasons)
                    .insufficientHistory(false)
                    .build();
        } else {
            // No rated supplier in category
            SupplierRankItem firstUnrated = items.get(0);
            recommended = RecommendedSupplierDto.builder()
                    .supplierId(firstUnrated.getSupplierId())
                    .supplierName(firstUnrated.getSupplierName())
                    .reasons(Collections.singletonList("Eligible supplier (insufficient historical data for full score)"))
                    .insufficientHistory(true)
                    .build();
        }

        return CompareResponse.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .ranking(items)
                .recommended(recommended)
                .build();
    }

    @Transactional(readOnly = true)
    public RecommendedSupplierDto getRecommendedSupplierForCategory(Short categoryId) {
        CompareResponse response = compareCategorySuppliers(categoryId);
        return response.getRecommended();
    }
}

