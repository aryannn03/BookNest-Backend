package com.booknest.wishlist.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlist_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int itemId;

    private int bookId;

    private String bookTitle;

    private double bookPrice;

    // Many items belong to one wishlist
    @ManyToOne
    @JoinColumn(name = "wishlist_id")
    @JsonIgnore
    @ToString.Exclude
    private Wishlist wishlist;
}