package com.booknest.review.resource;

import com.booknest.review.entity.Review;
import com.booknest.review.security.JwtUtil;
import com.booknest.review.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
public class ReviewResource {

    @Autowired
    private ReviewService reviewService;

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

    // ─── Add Review ───────────────────────────────────────────────────────────

    @PostMapping("/add")
    public ResponseEntity<Review> addReview(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam int bookId,
            @RequestParam int rating,
            @RequestParam String comment,
            @RequestParam String fullName) {
        int userId = extractUserId(authHeader);
        Review review = reviewService.addReview(userId, fullName, bookId, rating, comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    // ─── Get Reviews By Book ──────────────────────────────────────────────────

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<Review>> getByBook(
            @PathVariable int bookId) {
        return ResponseEntity.ok(reviewService.getByBook(bookId));
    }

    // ─── Get Reviews By User ──────────────────────────────────────────────────

    @GetMapping("/my-reviews")
    public ResponseEntity<List<Review>> getMyReviews(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        return ResponseEntity.ok(reviewService.getByUser(userId));
    }

    // ─── Get Review By ID ─────────────────────────────────────────────────────

    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReviewById(
            @PathVariable int reviewId) {
        return reviewService.getReviewById(reviewId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Update Review ────────────────────────────────────────────────────────

    @PutMapping("/update/{reviewId}")
    public ResponseEntity<Review> updateReview(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int reviewId,
            @RequestParam int rating,
            @RequestParam String comment) {
        int userId = extractUserId(authHeader);
        Review review = reviewService.updateReview(
                reviewId, userId, rating, comment);
        return ResponseEntity.ok(review);
    }

    // ─── Delete Review ────────────────────────────────────────────────────────

    @DeleteMapping("/delete/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int reviewId) {
        int userId = extractUserId(authHeader);
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(Map.of(
                "message", "Review deleted successfully"));
    }

    // ─── Get Average Rating ───────────────────────────────────────────────────

    @GetMapping("/avg-rating/{bookId}")
    public ResponseEntity<Map<String, Double>> getAvgRating(
            @PathVariable int bookId) {
        double avg = reviewService.getAvgRating(bookId);
        return ResponseEntity.ok(Map.of("averageRating", avg));
    }

    // ─── Get Review Count ─────────────────────────────────────────────────────

    @GetMapping("/count/{bookId}")
    public ResponseEntity<Map<String, Integer>> getReviewCount(
            @PathVariable int bookId) {
        int count = reviewService.getReviewCount(bookId);
        return ResponseEntity.ok(Map.of("reviewCount", count));
    }

    // ─── Get All Reviews (Admin) ──────────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<Review>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

}