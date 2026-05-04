package com.cg.order.orderservice.dto;

import lombok.Data;

@Data
public class BookResponse {
    private int bookId;
    private String title;
    private double price;
    private int stock;
}