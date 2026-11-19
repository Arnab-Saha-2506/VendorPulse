package com.tcs.VendorPulse.material.repository;

import com.tcs.VendorPulse.material.domain.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, Short> {
    Optional<MaterialCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}

