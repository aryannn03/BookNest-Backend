package com.cg.order.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RazorpayOrderResponse {
    private String razorpayOrderId;
    private double amount;          // in rupees (for display)
    private String currency;
    private String keyId;           // sent to frontend to open modal
}