package com.tcs.VendorPulse.material.service;

import com.tcs.VendorPulse.common.enums.ApprovalStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.material.domain.MaterialCategory;
import com.tcs.VendorPulse.material.dto.MaterialDtos.*;
import com.tcs.VendorPulse.material.repository.MaterialCategoryRepository;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.domain.SupplierCategory;
import com.tcs.VendorPulse.supplier.repository.SupplierCategoryRepository;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final MaterialCategoryRepository categoryRepository;
    private final SupplierCategoryRepository supplierCategoryRepository;
    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(cat -> {
                    long activeCount = supplierCategoryRepository.findByCategoryId(cat.getId()).stream()
                            .filter(sc -> {
                                return supplierRepository.findById(sc.getSupplierId())
                                        .map(s -> s.getApprovalStatus() == ApprovalStatus.APPROVED && Boolean.TRUE.equals(s.getIsActive()))
                                        .orElse(false);
                            })
                            .count();
                    return CategoryDto.builder()
                            .id(cat.getId())
                            .name(cat.getName())
                            .supplierCount(activeCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto createCategory(CategoryCreateRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ApiException(ErrorCode.CATEGORY_EXISTS);
        }
        MaterialCategory category = MaterialCategory.builder()
                .name(name)
                .build();
        category = categoryRepository.save(category);
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .supplierCount(0)
                .build();
    }

    @Transactional
    public CategoryDto renameCategory(Short id, CategoryCreateRequest request) {
        MaterialCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Category not found"));

        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name) && !category.getName().equalsIgnoreCase(name)) {
            throw new ApiException(ErrorCode.CATEGORY_EXISTS);
        }

        category.setName(name);
        category = categoryRepository.save(category);

        long count = supplierCategoryRepository.countByCategoryId(id);
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .supplierCount(count)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Supplier> getMappedSuppliers(Short categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Category not found");
        }
        List<SupplierCategory> mappings = supplierCategoryRepository.findByCategoryId(categoryId);
        List<UUID> supplierIds = mappings.stream().map(SupplierCategory::getSupplierId).collect(Collectors.toList());
        return supplierRepository.findAllById(supplierIds);
    }

    @Transactional
    public void mapSuppliers(Short categoryId, List<UUID> supplierIds) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Category not found");
        }

        // Validate that all suppliers are APPROVED and active (BR-06)
        for (UUID sId : supplierIds) {
            Supplier s = supplierRepository.findById(sId)
                    .orElseThrow(() -> new ApiException(ErrorCode.INVALID_MAPPING, "Supplier not found: " + sId));
            if (s.getApprovalStatus() != ApprovalStatus.APPROVED || !Boolean.TRUE.equals(s.getIsActive())) {
                throw new ApiException(ErrorCode.INVALID_MAPPING, "Supplier is not approved and active: " + s.getCompanyName());
            }
        }

        supplierCategoryRepository.deleteByCategoryId(categoryId);

        List<SupplierCategory> newMappings = new ArrayList<>();
        for (UUID sId : supplierIds) {
            newMappings.add(SupplierCategory.builder()
                    .categoryId(categoryId)
                    .supplierId(sId)
                    .build());
        }
        supplierCategoryRepository.saveAll(newMappings);
    }
}

