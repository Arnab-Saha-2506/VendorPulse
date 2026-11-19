package com.tcs.VendorPulse.settings.service;

import com.tcs.VendorPulse.audit.service.AuditService;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.settings.domain.ScoringSettings;
import com.tcs.VendorPulse.settings.dto.ScoringSettingsDto;
import com.tcs.VendorPulse.settings.repository.ScoringSettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringSettingsService {

    private final ScoringSettingsRepository scoringSettingsRepository;
    private final AuditService auditService;

    private volatile ScoringSettings cachedSettings;

    @PostConstruct
    public void init() {
        getSettings();
    }

    @Transactional(readOnly = true)
    public ScoringSettingsDto getSettings() {
        if (cachedSettings != null) {
            return toDto(cachedSettings);
        }
        ScoringSettings settings = scoringSettingsRepository.findById((short) 1)
                .orElseGet(this::createDefaultSettings);
        cachedSettings = settings;
        return toDto(settings);
    }

    public ScoringSettings getEntitySettings() {
        if (cachedSettings != null) {
            return cachedSettings;
        }
        getSettings();
        return cachedSettings;
    }

    @Transactional
    public ScoringSettingsDto updateSettings(ScoringSettingsDto request) {
        int totalWeight = request.getWDeliveryReliability() +
                          request.getWCommitmentStability() +
                          request.getWDeliveryConsistency() +
                          request.getWQualityPerformance();

        if (totalWeight != 100 ||
                request.getWDeliveryReliability() < 0 ||
                request.getWCommitmentStability() < 0 ||
                request.getWDeliveryConsistency() < 0 ||
                request.getWQualityPerformance() < 0 ||
                request.getHighCdiThreshold() < 0 ||
                request.getStockAlertHorizonDays() < 0 ||
                request.getMaxNegotiationRounds() <= 0 ||
                request.getDefaultCutoffDays() < 0) {
            throw new ApiException(ErrorCode.INVALID_CONFIGURATION);
        }

        ScoringSettings settings = scoringSettingsRepository.findById((short) 1)
                .orElseGet(() -> ScoringSettings.builder().id((short) 1).build());

        String oldValues = String.format("DR=%d, CS=%d, DC=%d, QP=%d, CDI_TH=%d",
                settings.getWDeliveryReliability(), settings.getWCommitmentStability(),
                settings.getWDeliveryConsistency(), settings.getWQualityPerformance(),
                settings.getHighCdiThreshold());

        settings.setWDeliveryReliability(request.getWDeliveryReliability());
        settings.setWCommitmentStability(request.getWCommitmentStability());
        settings.setWDeliveryConsistency(request.getWDeliveryConsistency());
        settings.setWQualityPerformance(request.getWQualityPerformance());
        settings.setHighCdiThreshold(request.getHighCdiThreshold());
        settings.setStockAlertHorizonDays(request.getStockAlertHorizonDays());
        settings.setMaxNegotiationRounds(request.getMaxNegotiationRounds());
        settings.setDefaultCutoffDays(request.getDefaultCutoffDays());

        CurrentUser current = CurrentUser.get();
        if (current != null) {
            settings.setUpdatedBy(current.getId());
        }

        settings = scoringSettingsRepository.save(settings);
        cachedSettings = settings;

        String newValues = String.format("DR=%d, CS=%d, DC=%d, QP=%d, CDI_TH=%d",
                settings.getWDeliveryReliability(), settings.getWCommitmentStability(),
                settings.getWDeliveryConsistency(), settings.getWQualityPerformance(),
                settings.getHighCdiThreshold());

        auditService.log(
                current != null ? current.getId() : null,
                current != null ? current.getFullName() : "System",
                "UPDATE_SCORING_SETTINGS",
                "SCORING_SETTINGS",
                "1",
                oldValues + " -> " + newValues
        );

        return toDto(settings);
    }

    private ScoringSettings createDefaultSettings() {
        ScoringSettings settings = ScoringSettings.builder()
                .id((short) 1)
                .wDeliveryReliability((short) 35)
                .wCommitmentStability((short) 25)
                .wDeliveryConsistency((short) 20)
                .wQualityPerformance((short) 20)
                .highCdiThreshold((short) 40)
                .stockAlertHorizonDays((short) 14)
                .maxNegotiationRounds((short) 3)
                .defaultCutoffDays((short) 7)
                .build();
        return scoringSettingsRepository.save(settings);
    }

    private ScoringSettingsDto toDto(ScoringSettings s) {
        return ScoringSettingsDto.builder()
                .id(s.getId())
                .wDeliveryReliability(s.getWDeliveryReliability())
                .wCommitmentStability(s.getWCommitmentStability())
                .wDeliveryConsistency(s.getWDeliveryConsistency())
                .wQualityPerformance(s.getWQualityPerformance())
                .highCdiThreshold(s.getHighCdiThreshold())
                .stockAlertHorizonDays(s.getStockAlertHorizonDays())
                .maxNegotiationRounds(s.getMaxNegotiationRounds())
                .defaultCutoffDays(s.getDefaultCutoffDays())
                .updatedBy(s.getUpdatedBy())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}

