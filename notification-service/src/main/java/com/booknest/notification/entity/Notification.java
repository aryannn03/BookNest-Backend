package com.booknest.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int notificationId;

    private int userId;

    // ORDER_PLACED, ORDER_DISPATCHED, ORDER_DELIVERED,
    // PAYMENT_SUCCESS, PAYMENT_FAILED, LOW_STOCK, NEW_ORDER
    private String type;

    @Column(columnDefinition = "TEXT")
    private String message;

    private boolean isRead;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }
}