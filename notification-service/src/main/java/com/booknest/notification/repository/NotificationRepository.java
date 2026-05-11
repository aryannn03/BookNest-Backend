package com.booknest.notification.repository;

import com.booknest.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Integer> {

    // Find all notifications for a user
    List<Notification> findByUserId(int userId);

    // Find unread notifications for a user
    List<Notification> findByUserIdAndIsRead(int userId, boolean isRead);

    // Count unread notifications for a user
    int countByUserIdAndIsRead(int userId, boolean isRead);

    // Find notifications by type
    List<Notification> findByType(String type);

    // Delete notification by id
    void deleteByNotificationId(int notificationId);
}