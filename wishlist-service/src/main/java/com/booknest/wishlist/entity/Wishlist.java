package com.booknest.wishlist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wishlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int wishlistId;

    @Column(unique = true)
    private int userId;

    private LocalDate createdAt;

    // One wishlist has many items
    @OneToMany(mappedBy = "wishlist",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<WishlistItem> books = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDate.now();
    }
}