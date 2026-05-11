package com.booknest.notification.service;

import com.booknest.notification.entity.Notification;

import java.util.List;

public interface NotificationService {

    // Send in-app notification
    Notification sendNotification(int userId,
                                  String type, String message);

    // Mark single notification as read
    void markAsRead(int notificationId);

    // Mark all notifications as read for a user
    void markAllRead(int userId);

    // Get all notifications for a user
    List<Notification> getByUser(int userId);

    // Get unread notifications for a user
    List<Notification> getUnreadByUser(int userId);

    // Get unread count for a user
    int getUnreadCount(int userId);

    // Delete a notification
    void deleteNotification(int notificationId);

    // Send email alert
    void sendEmailAlert(String toEmail,
                        String subject, String body);

    // Get all notifications (Admin)
    List<Notification> getAll();
}