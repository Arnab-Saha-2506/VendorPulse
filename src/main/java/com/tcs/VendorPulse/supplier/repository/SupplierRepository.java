package com.tcs.VendorPulse.supplier.repository;

import com.tcs.VendorPulse.common.enums.ApprovalStatus;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    Optional<Supplier> findByEmailIgnoreCase(String email);
    Optional<Supplier> findByRegistrationNo(String registrationNo);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRegistrationNo(String registrationNo);

    Page<Supplier> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE s.approvalStatus = 'APPROVED' AND " +
           "(:status IS NULL OR s.isActive = :status) AND " +
           "(:q IS NULL OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Supplier> searchApprovedSuppliers(@Param("q") String q, @Param("status") Boolean status, Pageable pageable);

    @Query("SELECT s FROM Supplier s JOIN SupplierCategory sc ON s.id = sc.supplierId WHERE " +
           "s.approvalStatus = 'APPROVED' AND sc.categoryId = :categoryId AND " +
           "(:status IS NULL OR s.isActive = :status) AND " +
           "(:q IS NULL OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Supplier> searchApprovedSuppliersWithCategory(
            @Param("q") String q,
            @Param("categoryId") Short categoryId,
            @Param("status") Boolean status,
            Pageable pageable);

    long countByApprovalStatus(ApprovalStatus approvalStatus);
}

