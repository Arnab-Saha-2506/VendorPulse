package com.tcs.VendorPulse.supplier.repository;

import com.tcs.VendorPulse.supplier.domain.SupplierCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierCategoryRepository extends JpaRepository<SupplierCategory, SupplierCategory.SupplierCategoryId> {
    List<SupplierCategory> findBySupplierId(UUID supplierId);
    List<SupplierCategory> findByCategoryId(Short categoryId);

    long countByCategoryId(Short categoryId);

    @Modifying
    @Query("DELETE FROM SupplierCategory sc WHERE sc.categoryId = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Short categoryId);

    @Modifying
    @Query("DELETE FROM SupplierCategory sc WHERE sc.supplierId = :supplierId")
    void deleteBySupplierId(@Param("supplierId") UUID supplierId);
}

