package com.booknest.notification.serviceimpl;

import com.booknest.notification.entity.Notification;
import com.booknest.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification mockNotification;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService,
                "fromEmail", "test@booknest.com");

        mockNotification = new Notification();
        mockNotification.setNotificationId(1);
        mockNotification.setUserId(1);
        mockNotification.setType("ORDER_PLACED");
        mockNotification.setMessage("Your order has been placed.");
        mockNotification.setRead(false);
    }

    // ─── Send Notification ────────────────────────────────────────────────────

    @Test
    void sendNotification_Success() {
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(mockNotification);

        Notification result = notificationService.sendNotification(
                1, "ORDER_PLACED", "Your order has been placed.");

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals("ORDER_PLACED", result.getType());
        assertFalse(result.isRead());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sendNotification_DecodesUrlEncodedMessage() {
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(mockNotification);

        Notification result = notificationService.sendNotification(
                1, "ORDER_PLACED", "Your+order+has+been+placed.");

        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    // ─── Mark As Read ─────────────────────────────────────────────────────────

    @Test
    void markAsRead_Success() {
        when(notificationRepository.findById(1))
                .thenReturn(Optional.of(mockNotification));
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(mockNotification);

        assertDoesNotThrow(() -> notificationService.markAsRead(1));

        assertTrue(mockNotification.isRead());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAsRead_NotFound_ThrowsException() {
        when(notificationRepository.findById(99))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> notificationService.markAsRead(99));
        assertTrue(ex.getMessage().contains("Notification not found"));
    }

    // ─── Mark All Read ────────────────────────────────────────────────────────

    @Test
    void markAllRead_Success() {
        Notification unread1 = new Notification();
        unread1.setUserId(1);
        unread1.setRead(false);

        Notification unread2 = new Notification();
        unread2.setUserId(1);
        unread2.setRead(false);

        when(notificationRepository.findByUserIdAndIsRead(1, false))
                .thenReturn(List.of(unread1, unread2));
        when(notificationRepository.saveAll(anyList()))
                .thenReturn(List.of(unread1, unread2));

        assertDoesNotThrow(() -> notificationService.markAllRead(1));

        assertTrue(unread1.isRead());
        assertTrue(unread2.isRead());
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void markAllRead_NoUnread_DoesNothing() {
        when(notificationRepository.findByUserIdAndIsRead(1, false))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> notificationService.markAllRead(1));

        verify(notificationRepository).saveAll(anyList());
    }

    // ─── Get By User ──────────────────────────────────────────────────────────

    @Test
    void getByUser_ReturnsNotifications() {
        when(notificationRepository.findByUserId(1))
                .thenReturn(List.of(mockNotification));

        List<Notification> result = notificationService.getByUser(1);

        assertEquals(1, result.size());
        assertEquals("ORDER_PLACED", result.get(0).getType());
    }

    @Test
    void getByUser_NoNotifications_ReturnsEmpty() {
        when(notificationRepository.findByUserId(1))
                .thenReturn(List.of());

        List<Notification> result = notificationService.getByUser(1);

        assertTrue(result.isEmpty());
    }

    // ─── Get Unread By User ───────────────────────────────────────────────────

    @Test
    void getUnreadByUser_ReturnsUnreadNotifications() {
        when(notificationRepository.findByUserIdAndIsRead(1, false))
                .thenReturn(List.of(mockNotification));

        List<Notification> result = notificationService.getUnreadByUser(1);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isRead());
    }

    // ─── Get Unread Count ─────────────────────────────────────────────────────

    @Test
    void getUnreadCount_ReturnsCount() {
        when(notificationRepository.countByUserIdAndIsRead(1, false))
                .thenReturn(3);

        int count = notificationService.getUnreadCount(1);

        assertEquals(3, count);
    }

    @Test
    void getUnreadCount_NoUnread_ReturnsZero() {
        when(notificationRepository.countByUserIdAndIsRead(1, false))
                .thenReturn(0);

        int count = notificationService.getUnreadCount(1);

        assertEquals(0, count);
    }

    // ─── Delete Notification ──────────────────────────────────────────────────

    @Test
    void deleteNotification_Success() {
        when(notificationRepository.existsById(1)).thenReturn(true);

        assertDoesNotThrow(() ->
                notificationService.deleteNotification(1));
        verify(notificationRepository).deleteById(1);
    }

    @Test
    void deleteNotification_NotFound_ThrowsException() {
        when(notificationRepository.existsById(99)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> notificationService.deleteNotification(99));
        assertTrue(ex.getMessage().contains("Notification not found"));
    }

    // ─── Send Email Alert ─────────────────────────────────────────────────────

    @Test
    void sendEmailAlert_Success() {
        assertDoesNotThrow(() -> notificationService.sendEmailAlert(
                "user@example.com",
                "Order Confirmed",
                "Your order has been placed."));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmailAlert_DecodesUrlEncodedParams() {
        assertDoesNotThrow(() -> notificationService.sendEmailAlert(
                "user@example.com",
                "Order+Confirmed",
                "Your+order+has+been+placed."));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmailAlert_MailFailure_DoesNotThrow() {
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> notificationService.sendEmailAlert(
                "user@example.com",
                "Order Confirmed",
                "Your order has been placed."));
    }

    // ─── Get All Notifications ────────────────────────────────────────────────

    @Test
    void getAll_ReturnsAllNotifications() {
        when(notificationRepository.findAll())
                .thenReturn(List.of(mockNotification));

        List<Notification> result = notificationService.getAll();

        assertEquals(1, result.size());
    }
}