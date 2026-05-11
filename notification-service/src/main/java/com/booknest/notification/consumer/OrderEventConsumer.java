package com.booknest.notification.consumer;

import com.booknest.notification.config.RabbitMQConfig;
import com.booknest.notification.dto.OrderEvent;
import com.booknest.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    @Autowired
    private NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleOrderEvent(OrderEvent event) {

        // Save in-app notification
        String message = buildMessage(event);
        notificationService.sendNotification(
            event.getUserId(),
            event.getEventType(),
            message
        );

        // Send email if email is available
        if (event.getUserEmail() != null &&
                !event.getUserEmail().isEmpty()) {
            String subject = buildSubject(event);
            String body = buildEmailBody(event);
            notificationService.sendEmailAlert(
                event.getUserEmail(), subject, body);
        }
    }

    private String buildMessage(OrderEvent event) {
        return switch (event.getEventType()) {
            case "ORDER_PLACED" -> "Your order #" + event.getOrderId()
                + " for " + event.getBookTitle()
                + " has been placed successfully.";
            case "ORDER_CANCELLED" -> "Your order #" + event.getOrderId()
                + " has been cancelled.";
            case "STATUS_CHANGED" -> "Your order #" + event.getOrderId()
                + " status has been updated to: "
                + event.getOrderStatus();
            default -> "Update on your order #" + event.getOrderId();
        };
    }

    private String buildSubject(OrderEvent event) {
        return switch (event.getEventType()) {
            case "ORDER_PLACED" -> "Order Confirmed - #" + event.getOrderId();
            case "ORDER_CANCELLED" -> "Order Cancelled - #" + event.getOrderId();
            case "STATUS_CHANGED" -> "Order " + event.getOrderStatus()
                + " - #" + event.getOrderId();
            default -> "Order Update - #" + event.getOrderId();
        };
    }

    private String buildEmailBody(OrderEvent event) {
        return switch (event.getEventType()) {
            case "ORDER_PLACED" -> "Hello,\n\n"
                + "Your order has been placed successfully.\n\n"
                + "Order ID: #" + event.getOrderId() + "\n"
                + "Book: " + event.getBookTitle() + "\n"
                + "Quantity: " + event.getQuantity() + "\n"
                + "Amount: ₹" + event.getAmountPaid() + "\n"
                + "Payment Mode: " + event.getPaymentMode() + "\n\n"
                + "Thank you for shopping with BookNest.";
            case "ORDER_CANCELLED" -> "Hello,\n\n"
                + "Your order #" + event.getOrderId()
                + " has been cancelled.\n\n"
                + "If payment was made via wallet, "
                + "a refund has been processed.\n\n"
                + "BookNest Team";
            case "STATUS_CHANGED" -> "Hello,\n\n"
                + "Your order #" + event.getOrderId()
                + " status has been updated to: "
                + event.getOrderStatus() + "\n\n"
                + "Book: " + event.getBookTitle() + "\n\n"
                + "Thank you for shopping with BookNest.";
            default -> "Update on your order #" + event.getOrderId();
        };
    }
}