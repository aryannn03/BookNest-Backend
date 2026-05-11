package com.cg.order.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookResponse {
    private int bookId;
    private String title;
    private double price;
    private int stock;
}