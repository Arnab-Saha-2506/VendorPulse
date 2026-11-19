package com.tcs.VendorPulse.notification.api;

import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.notification.dto.NotificationDtos.*;
import com.tcs.VendorPulse.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get notifications", description = "Get notifications scoped to current user or supplier")
    public ResponseEntity<PagedResponse<NotificationDto>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        CurrentUser current = CurrentUser.get();
        UUID supplierId = current.isSupplier() ? current.getSupplierId() : null;
        UUID userId = current.isProcurement() ? current.getId() : null;

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(notificationService.getNotifications(supplierId, userId, unread, pageable));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        CurrentUser current = CurrentUser.get();
        notificationService.markAsRead(id, current.getSupplierId(), current.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead() {
        CurrentUser current = CurrentUser.get();
        UUID supplierId = current.isSupplier() ? current.getSupplierId() : null;
        UUID userId = current.isProcurement() ? current.getId() : null;
        notificationService.markAllAsRead(supplierId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count badge")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        CurrentUser current = CurrentUser.get();
        UUID supplierId = current.isSupplier() ? current.getSupplierId() : null;
        UUID userId = current.isProcurement() ? current.getId() : null;
        long count = notificationService.getUnreadCount(supplierId, userId);
        return ResponseEntity.ok(UnreadCountResponse.builder().unreadCount(count).build());
    }
}

