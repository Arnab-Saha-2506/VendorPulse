package com.tcs.VendorPulse.notification.repository;

import com.tcs.VendorPulse.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE " +
           "((:supplierId IS NOT NULL AND n.supplierId = :supplierId) OR (:userId IS NOT NULL AND n.userId = :userId)) AND " +
           "(:unreadOnly = FALSE OR n.readAt IS NULL) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findUserNotifications(
            @Param("supplierId") UUID supplierId,
            @Param("userId") UUID userId,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE " +
           "((:supplierId IS NOT NULL AND n.supplierId = :supplierId) OR (:userId IS NOT NULL AND n.userId = :userId)) AND " +
           "n.readAt IS NULL")
    long countUnreadNotifications(@Param("supplierId") UUID supplierId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE " +
           "((:supplierId IS NOT NULL AND n.supplierId = :supplierId) OR (:userId IS NOT NULL AND n.userId = :userId)) AND " +
           "n.readAt IS NULL")
    void markAllAsRead(@Param("supplierId") UUID supplierId, @Param("userId") UUID userId, @Param("readAt") Instant readAt);
}

