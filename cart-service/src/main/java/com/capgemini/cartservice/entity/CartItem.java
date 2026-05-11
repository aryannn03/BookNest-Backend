package com.capgemini.cartservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int itemId;

    private int bookId;

    private String bookTitle;

    private double price;

    private int quantity;

    // Many items belong to one cart
    @ManyToOne
    @JoinColumn(name = "cart_id")
    @JsonIgnore
    @ToString.Exclude 
    private Cart cart;
}