package com.booknest.review.service;

import com.booknest.review.entity.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewService {

    // Add new review
	Review addReview(int userId, String fullName, int bookId, int rating, String comment);

    // Get all reviews for a book
    List<Review> getByBook(int bookId);

    // Get all reviews by a user
    List<Review> getByUser(int userId);

    // Update review
    Review updateReview(int reviewId, int userId,
                        int rating, String comment);

    // Delete review
    void deleteReview(int reviewId, int userId);

    // Get average rating for a book
    double getAvgRating(int bookId);

    // Get all reviews (Admin)
    List<Review> getAllReviews();

    // Get review by ID
    Optional<Review> getReviewById(int reviewId);

    // Get review count for a book
    int getReviewCount(int bookId);
}