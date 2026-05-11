package com.cg.order.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderEvent {
    private int orderId;
    private int userId;
    private String userEmail;
    private String bookTitle;
    private int quantity;
    private double amountPaid;
    private String paymentMode;
    private String orderStatus;
    private String eventType; // ORDER_PLACED, ORDER_CANCELLED, STATUS_CHANGED
}