package com.booknest.review.serviceimpl;

import com.booknest.review.client.BookClient;
import com.booknest.review.client.OrderClient;
import com.booknest.review.dto.OrderResponse;
import com.booknest.review.entity.Review;
import com.booknest.review.repository.ReviewRepository;
import com.booknest.review.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderClient orderClient;

    @Autowired
    private BookClient bookClient;

    // ─── Add Review ───────────────────────────────────────────────────────────

    @Override
    public Review addReview(int userId, String fullName, int bookId,
                            int rating, String comment) {

        if (reviewRepository.findByBookIdAndUserId(bookId, userId).isPresent()) {
            throw new RuntimeException("You have already reviewed this book");
        }

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        boolean verified = false;
        try {
            OrderResponse[] orders = orderClient.getOrdersByUserId(userId);

            if (orders != null) {
                for (OrderResponse order : orders) {
                    if (order.getBookId() == bookId
                            && "Delivered".equals(order.getOrderStatus())) {
                        verified = true;
                        break;
                    }
                }
            }

            if (!verified) {
                throw new RuntimeException(
                    "You can only review books you have purchased and received");
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                "Unable to verify purchase. Please try again later.");
        }

        Review review = new Review();
        review.setFullName(fullName);
        review.setUserId(userId);
        review.setBookId(bookId);
        review.setRating(rating);
        review.setComment(comment);
        review.setVerified(verified);

        Review saved = reviewRepository.save(review);
        updateBookRating(bookId);
        return saved;
    }

    // ─── Get By Book ──────────────────────────────────────────────────────────

    @Override
    public List<Review> getByBook(int bookId) {
        return reviewRepository.findByBookId(bookId);
    }

    // ─── Get By User ──────────────────────────────────────────────────────────

    @Override
    public List<Review> getByUser(int userId) {
        return reviewRepository.findByUserId(userId);
    }

    // ─── Update Review ────────────────────────────────────────────────────────

    @Override
    public Review updateReview(int reviewId, int userId,
                               int rating, String comment) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException(
                        "Review not found: " + reviewId));

        if (review.getUserId() != userId) {
            throw new RuntimeException("Unauthorized to update this review");
        }

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        review.setRating(rating);
        review.setComment(comment);

        Review updated = reviewRepository.save(review);
        updateBookRating(review.getBookId());
        return updated;
    }

    // ─── Delete Review ────────────────────────────────────────────────────────

    @Override
    public void deleteReview(int reviewId, int userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException(
                        "Review not found: " + reviewId));

        if (review.getUserId() != userId) {
            throw new RuntimeException("Unauthorized to delete this review");
        }

        int bookId = review.getBookId();
        reviewRepository.deleteById(reviewId);
        updateBookRating(bookId);
    }

    // ─── Get Average Rating ───────────────────────────────────────────────────

    @Override
    public double getAvgRating(int bookId) {
        Double avg = reviewRepository.avgRatingByBookId(bookId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    // ─── Get All Reviews ──────────────────────────────────────────────────────

    @Override
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    // ─── Get Review By ID ─────────────────────────────────────────────────────

    @Override
    public Optional<Review> getReviewById(int reviewId) {
        return reviewRepository.findById(reviewId);
    }

    // ─── Get Review Count ─────────────────────────────────────────────────────

    @Override
    public int getReviewCount(int bookId) {
        return reviewRepository.countByBookId(bookId);
    }

    // ─── Helper — Update Book Rating ──────────────────────────────────────────

    private void updateBookRating(int bookId) {
        try {
            double avgRating = getAvgRating(bookId);
            bookClient.updateRating(bookId, avgRating);
        } catch (Exception e) {
            log.warn("Could not update book rating for bookId {}: {}",
                    bookId, e.getMessage());
        }
    }
}