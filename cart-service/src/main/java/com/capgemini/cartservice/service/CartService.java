package com.capgemini.cartservice.service;

import com.capgemini.cartservice.entity.Cart;

import java.util.List;

public interface CartService {

    // Get cart by userId
    Cart getCartByUser(int userId);

    // Add item to cart
    Cart addItem(int userId, int bookId, int quantity);

    // Remove item from cart
    Cart removeItem(int userId, int itemId);

    // Update item quantity
    Cart updateQuantity(int userId, int itemId, int quantity);

    // Clear entire cart
    void clearCart(int userId);

    // Calculate cart total
    double cartTotal(int userId);

    // Get all carts (Admin)
    List<Cart> getAllCarts();
}