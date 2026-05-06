package com.booknest.review.dto;

import lombok.Data;

@Data
public class OrderResponse {
    private int bookId;
    private String orderStatus;
}