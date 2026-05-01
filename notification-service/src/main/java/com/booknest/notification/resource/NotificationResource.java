package com.booknest.notification.resource;

import com.booknest.notification.entity.Notification;
import com.booknest.notification.security.JwtUtil;
import com.booknest.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationResource {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtUtil jwtUtil;

    // ─── Helper — Extract userId from token ───────────────────────────────────

    private int extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException(
                    "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        return jwtUtil.extractUserId(token);
    }

    // ─── Send Notification (internal service call) ────────────────────────────

    @PostMapping("/send")
    public ResponseEntity<Notification> sendNotification(
            @RequestParam int userId,
            @RequestParam String type,
            @RequestParam String message) {
        Notification notification = notificationService
                .sendNotification(userId, type, message);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notification);
    }

    // ─── Send Email Alert (internal service call) ─────────────────────────────

    @PostMapping("/send-email")
    public ResponseEntity<Map<String, String>> sendEmail(
            @RequestParam String toEmail,
            @RequestParam String subject,
            @RequestParam String body) {
        notificationService.sendEmailAlert(toEmail, subject, body);
        return ResponseEntity.ok(Map.of(
                "message", "Email sent successfully"));
    }

    // ─── Get My Notifications ─────────────────────────────────────────────────

    @GetMapping("/my-notifications")
    public ResponseEntity<List<Notification>> getMyNotifications(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        return ResponseEntity.ok(
                notificationService.getByUser(userId));
    }

    // ─── Get My Unread Notifications ──────────────────────────────────────────

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        return ResponseEntity.ok(
                notificationService.getUnreadByUser(userId));
    }

    // ─── Get Unread Count (badge) ─────────────────────────────────────────────

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        int count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ─── Mark Single As Read ──────────────────────────────────────────────────

    @PutMapping("/mark-read/{notificationId}")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable int notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of(
                "message", "Notification marked as read"));
    }

    // ─── Mark All As Read ─────────────────────────────────────────────────────

    @PutMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllRead(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read"));
    }

    // ─── Delete Notification ──────────────────────────────────────────────────

    @DeleteMapping("/delete/{notificationId}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable int notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of(
                "message", "Notification deleted successfully"));
    }

    // ─── Get All Notifications (Admin) ────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    // ─── Global Exception Handler ─────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(
            RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}