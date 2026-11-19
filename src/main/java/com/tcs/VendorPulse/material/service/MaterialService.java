package com.tcs.VendorPulse.material.service;

import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.material.domain.Material;
import com.tcs.VendorPulse.material.domain.MaterialCategory;
import com.tcs.VendorPulse.material.dto.MaterialDtos.*;
import com.tcs.VendorPulse.material.repository.MaterialCategoryRepository;
import com.tcs.VendorPulse.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public PagedResponse<MaterialDto> searchMaterials(Short categoryId, String q, Boolean active, Pageable pageable) {
        Page<Material> page = materialRepository.searchMaterials(categoryId, q, active, pageable);
        Map<Short, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(MaterialCategory::getId, MaterialCategory::getName));

        return PagedResponse.from(page.map(m -> toDto(m, categoryNames.get(m.getCategoryId()))));
    }

    @Transactional
    public MaterialDto createMaterial(CreateMaterialRequest request) {
        String code = request.getCode().trim();
        if (materialRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(ErrorCode.MATERIAL_EXISTS);
        }

        MaterialCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Category not found"));

        Material material = Material.builder()
                .code(code)
                .name(request.getName().trim())
                .categoryId(category.getId())
                .isCritical(Boolean.TRUE.equals(request.getIsCritical()))
                .isActive(true)
                .stockQty(BigDecimal.ZERO)
                .dailyUsage(BigDecimal.ZERO)
                .build();

        material = materialRepository.save(material);
        return toDto(material, category.getName());
    }

    @Transactional
    public MaterialDto updateMaterial(UUID id, UpdateMaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Material not found"));

        MaterialCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Category not found"));

        material.setName(request.getName().trim());
        material.setCategoryId(category.getId());
        material.setIsCritical(Boolean.TRUE.equals(request.getIsCritical()));

        material = materialRepository.save(material);
        return toDto(material, category.getName());
    }

    @Transactional
    public MaterialDto updateMaterialStatus(UUID id, Boolean isActive) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Material not found"));

        material.setIsActive(Boolean.TRUE.equals(isActive));
        material = materialRepository.save(material);

        String categoryName = categoryRepository.findById(material.getCategoryId())
                .map(MaterialCategory::getName).orElse("");

        return toDto(material, categoryName);
    }

    @Transactional(readOnly = true)
    public MaterialDto getMaterialById(UUID id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Material not found"));
        String categoryName = categoryRepository.findById(material.getCategoryId())
                .map(MaterialCategory::getName).orElse("");
        return toDto(material, categoryName);
    }

    private MaterialDto toDto(Material m, String categoryName) {
        return MaterialDto.builder()
                .id(m.getId())
                .code(m.getCode())
                .name(m.getName())
                .categoryId(m.getCategoryId())
                .categoryName(categoryName)
                .isCritical(m.getIsCritical())
                .isActive(m.getIsActive())
                .stockQty(m.getStockQty())
                .dailyUsage(m.getDailyUsage())
                .stockUpdatedAt(m.getStockUpdatedAt())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}

