package com.capgemini.cartservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int cartId;

    // Links cart to a specific user
    @Column(unique = true)
    private int userId;

    private double totalPrice;

    // One cart has many cart items
    @OneToMany(mappedBy = "cart",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>();
}