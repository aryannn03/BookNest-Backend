package com.capgemini.cartservice.resource;

import com.capgemini.cartservice.entity.Cart;
import com.capgemini.cartservice.security.JwtUtil;
import com.capgemini.cartservice.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartResource {

    @Autowired
    private CartService cartService;

    @Autowired
    private JwtUtil jwtUtil;

    // ─── Helper — Extract userId from token ───────────────────────────────────

    private int extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        return jwtUtil.extractUserId(token);
    }

    // ─── Get My Cart ──────────────────────────────────────────────────────────

    @GetMapping("/my-cart")
    public ResponseEntity<Cart> getMyCart(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        Cart cart = cartService.getCartByUser(userId);
        return ResponseEntity.ok(cart);
    }

    // ─── Add Item To Cart ─────────────────────────────────────────────────────

    @PostMapping("/add")
    public ResponseEntity<Cart> addItem(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam int bookId,
            @RequestParam int quantity) {
        int userId = extractUserId(authHeader);
        Cart cart = cartService.addItem(userId, bookId, quantity);
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    // ─── Remove Item From Cart ────────────────────────────────────────────────

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<Cart> removeItem(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int itemId) {
        int userId = extractUserId(authHeader);
        Cart cart = cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(cart);
    }

    // ─── Update Item Quantity ─────────────────────────────────────────────────

    @PutMapping("/update/{itemId}")
    public ResponseEntity<Cart> updateQuantity(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int itemId,
            @RequestParam int quantity) {
        int userId = extractUserId(authHeader);
        Cart cart = cartService.updateQuantity(userId, itemId, quantity);
        return ResponseEntity.ok(cart);
    }

    // ─── Clear Cart ───────────────────────────────────────────────────────────

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCart(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
    }

    // ─── Get Cart Total ───────────────────────────────────────────────────────

    @GetMapping("/total")
    public ResponseEntity<Map<String, Double>> getCartTotal(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        double total = cartService.cartTotal(userId);
        return ResponseEntity.ok(Map.of("total", total));
    }

    // ─── Get Cart By UserId (internal service call) ───────────────────────────

    @GetMapping("/user/{userId}")
    public ResponseEntity<Cart> getCartByUserId(
            @PathVariable int userId) {
        Cart cart = cartService.getCartByUser(userId);
        return ResponseEntity.ok(cart);
    }

    // ─── Get All Carts (Admin) ────────────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<Cart>> getAllCarts() {
        return ResponseEntity.ok(cartService.getAllCarts());
    }
    
    // ─── Clear Cart By UserId (internal service call) ─────────────────────────

    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<Map<String, String>> clearCartByUserId(
            @PathVariable int userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message",
                "Cart cleared successfully"));
    }

}