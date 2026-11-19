package com.tcs.VendorPulse.settings.repository;

import com.tcs.VendorPulse.settings.domain.ScoringSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoringSettingsRepository extends JpaRepository<ScoringSettings, Short> {
    Optional<ScoringSettings> findById(Short id);
}

