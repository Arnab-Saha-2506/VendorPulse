package com.tcs.VendorPulse.material.repository;

import com.tcs.VendorPulse.material.domain.Material;
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
public interface MaterialRepository extends JpaRepository<Material, UUID> {
    Optional<Material> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT m FROM Material m WHERE " +
           "(:categoryId IS NULL OR m.categoryId = :categoryId) AND " +
           "(:active IS NULL OR m.isActive = :active) AND " +
           "(:q IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(m.code) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Material> searchMaterials(
            @Param("categoryId") Short categoryId,
            @Param("q") String q,
            @Param("active") Boolean active,
            Pageable pageable);

    List<Material> findByIsActiveTrue();
}

