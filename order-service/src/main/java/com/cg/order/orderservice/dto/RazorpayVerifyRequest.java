package com.cg.order.orderservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayVerifyRequest {

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    // bookId and quantity removed — cart items are fetched from the cart-service
    private int addressId;
}