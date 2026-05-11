package com.booknest.wishlist.resource;

import com.booknest.wishlist.entity.Wishlist;
import com.booknest.wishlist.security.JwtUtil;
import com.booknest.wishlist.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wishlist")
public class WishlistResource {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private JwtUtil jwtUtil;

    // ─── Helper — Extract userId from token ───────────────────────────────────

    private int extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException(
                    "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        return jwtUtil.extractUserId(token);
    }

    // ─── Get My Wishlist ──────────────────────────────────────────────────────

    @GetMapping("/my-wishlist")
    public ResponseEntity<Wishlist> getMyWishlist(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        return ResponseEntity.ok(
                wishlistService.getWishlistByUser(userId));
    }

    // ─── Add Book To Wishlist ─────────────────────────────────────────────────

    @PostMapping("/add/{bookId}")
    public ResponseEntity<Wishlist> addBook(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int bookId) {
        int userId = extractUserId(authHeader);
        Wishlist wishlist = wishlistService.addBook(userId, bookId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wishlist);
    }

    // ─── Remove Book From Wishlist ────────────────────────────────────────────

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<Wishlist> removeBook(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int itemId) {
        int userId = extractUserId(authHeader);
        Wishlist wishlist = wishlistService.removeBook(
                userId, itemId);
        return ResponseEntity.ok(wishlist);
    }

    // ─── Clear Wishlist ───────────────────────────────────────────────────────

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearWishlist(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        wishlistService.clearWishlist(userId);
        return ResponseEntity.ok(Map.of(
                "message", "Wishlist cleared successfully"));
    }

    // ─── Move Item To Cart ────────────────────────────────────────────────────

    @PostMapping("/move-to-cart/{itemId}")
    public ResponseEntity<Map<String, String>> moveToCart(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int itemId) {
        int userId = extractUserId(authHeader);
        wishlistService.moveToCart(userId, itemId, authHeader);
        return ResponseEntity.ok(Map.of(
                "message", "Item moved to cart successfully"));
    }

    // ─── Get All Wishlists (Admin) ────────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<Wishlist>> getAllWishlists() {
        return ResponseEntity.ok(
                wishlistService.getAllWishlists());
    }

}