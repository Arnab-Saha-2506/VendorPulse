package com.tcs.VendorPulse.notification.service;

import com.tcs.VendorPulse.common.enums.NotificationType;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.notification.domain.Notification;
import com.tcs.VendorPulse.notification.dto.NotificationDtos.*;
import com.tcs.VendorPulse.notification.repository.NotificationRepository;
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
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(UUID supplierId, UUID userId, NotificationType type, String message, UUID poId) {
        try {
            Notification notification = Notification.builder()
                    .supplierId(supplierId)
                    .userId(userId)
                    .type(type)
                    .message(message)
                    .poId(poId)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getNotifications(UUID supplierId, UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = notificationRepository.findUserNotifications(supplierId, userId, unreadOnly, pageable);
        return PagedResponse.from(page.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID supplierId, UUID userId) {
        return notificationRepository.countUnreadNotifications(supplierId, userId);
    }

    @Transactional
    public void markAsRead(UUID id, UUID supplierId, UUID userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Notification not found"));

        boolean allowed = false;
        if (supplierId != null && supplierId.equals(notification.getSupplierId())) {
            allowed = true;
        }
        if (userId != null && userId.equals(notification.getUserId())) {
            allowed = true;
        }

        if (!allowed) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID supplierId, UUID userId) {
        notificationRepository.markAllAsRead(supplierId, userId, Instant.now());
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .supplierId(n.getSupplierId())
                .userId(n.getUserId())
                .type(n.getType())
                .message(n.getMessage())
                .poId(n.getPoId())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

