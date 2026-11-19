package com.tcs.VendorPulse.audit.service;

import com.tcs.VendorPulse.audit.domain.AuditLog;
import com.tcs.VendorPulse.audit.dto.AuditLogDto;
import com.tcs.VendorPulse.audit.repository.AuditLogRepository;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID userId, String userName, String action, String recordType, String recordId, String recordLabel) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userName(userName != null ? userName : "System")
                    .action(action)
                    .recordType(recordType)
                    .recordId(recordId)
                    .recordLabel(recordLabel)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> searchAuditLogs(Instant from, Instant to, UUID userId, String action, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.searchAuditLogs(from, to, userId, action, pageable);
        Page<AuditLogDto> dtoPage = page.map(a -> AuditLogDto.builder()
                .id(a.getId())
                .occurredAt(a.getOccurredAt())
                .userId(a.getUserId())
                .userName(a.getUserName())
                .action(a.getAction())
                .recordType(a.getRecordType())
                .recordId(a.getRecordId())
                .recordLabel(a.getRecordLabel())
                .build());
        return PagedResponse.from(dtoPage);
    }
}

