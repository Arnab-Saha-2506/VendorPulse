package com.tcs.VendorPulse.material.repository;

import com.tcs.VendorPulse.material.entity.Material;
import com.tcs.VendorPulse.material.entity.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {}
public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, Long> {}
