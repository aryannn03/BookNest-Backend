package com.booknest.review.repository;

import com.booknest.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Find all reviews for a book
    List<Review> findByBookId(int bookId);

    // Find all reviews by a user
    List<Review> findByUserId(int userId);

    // Find review by bookId and userId
    Optional<Review> findByBookIdAndUserId(int bookId, int userId);

    // Average rating for a book
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.bookId = :bookId")
    Double avgRatingByBookId(@Param("bookId") int bookId);

    // Count reviews for a book
    int countByBookId(int bookId);

    // Delete review by reviewId
    void deleteByReviewId(int reviewId);

    // Find verified reviews for a book
    List<Review> findByBookIdAndVerifiedTrue(int bookId);
}