package com.booknest.notification.serviceimpl;

import com.booknest.notification.entity.Notification;
import com.booknest.notification.repository.NotificationRepository;
import com.booknest.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JavaMailSender mailSender;

    // ─── Send Notification ────────────────────────────────────────────────────

    @Override
    public Notification sendNotification(int userId,
                                         String type,
                                         String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    // ─── Mark As Read ─────────────────────────────────────────────────────────

    @Override
    public void markAsRead(int notificationId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new RuntimeException(
                        "Notification not found: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // ─── Mark All Read ────────────────────────────────────────────────────────

    @Override
    public void markAllRead(int userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndIsRead(userId, false);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    // ─── Get By User ──────────────────────────────────────────────────────────

    @Override
    public List<Notification> getByUser(int userId) {
        return notificationRepository.findByUserId(userId);
    }

    // ─── Get Unread By User ───────────────────────────────────────────────────

    @Override
    public List<Notification> getUnreadByUser(int userId) {
        return notificationRepository
                .findByUserIdAndIsRead(userId, false);
    }

    // ─── Get Unread Count ─────────────────────────────────────────────────────

    @Override
    public int getUnreadCount(int userId) {
        return notificationRepository
                .countByUserIdAndIsRead(userId, false);
    }

    // ─── Delete Notification ──────────────────────────────────────────────────

    @Override
    public void deleteNotification(int notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new RuntimeException(
                    "Notification not found: " + notificationId);
        }
        notificationRepository.deleteById(notificationId);
    }

    // ─── Send Email Alert ─────────────────────────────────────────────────────

    @Override
    public void sendEmailAlert(String toEmail,
                               String subject,
                               String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("BookNest");
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Email sending failed: "
                    + e.getMessage());
        }
    }

    // ─── Get All Notifications ────────────────────────────────────────────────

    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }
}