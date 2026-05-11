package com.booknest.wishlist.service;

import com.booknest.wishlist.entity.Wishlist;

import java.util.List;

public interface WishlistService {

    // Get wishlist by userId
    Wishlist getWishlistByUser(int userId);

    // Add book to wishlist
    Wishlist addBook(int userId, int bookId);

    // Remove book from wishlist
    Wishlist removeBook(int userId, int itemId);

    // Clear entire wishlist
    void clearWishlist(int userId);

    // Move item from wishlist to cart
    void moveToCart(int userId, int itemId, String authHeader);

    // Get all wishlists (Admin)
    List<Wishlist> getAllWishlists();
}