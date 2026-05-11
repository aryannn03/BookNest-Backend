package com.booknest.review.serviceimpl;

import com.booknest.review.client.BookClient;
import com.booknest.review.client.OrderClient;
import com.booknest.review.dto.OrderResponse;
import com.booknest.review.entity.Review;
import com.booknest.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private BookClient bookClient;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review mockReview;
    private OrderResponse mockOrder;

    @BeforeEach
    void setUp() {
        mockReview = new Review();
        mockReview.setReviewId(1);
        mockReview.setUserId(1);
        mockReview.setBookId(1);
        mockReview.setFullName("Test User");
        mockReview.setRating(4);
        mockReview.setComment("Great book!");
        mockReview.setVerified(true);

        mockOrder = new OrderResponse();
        mockOrder.setBookId(1);
        mockOrder.setOrderStatus("Delivered");
    }

    // ─── Add Review ───────────────────────────────────────────────────────────

    @Test
    void addReview_Success() {
        when(reviewRepository.findByBookIdAndUserId(1, 1))
                .thenReturn(Optional.empty());
        when(orderClient.getOrdersByUserId(1))
                .thenReturn(new OrderResponse[]{mockOrder});
        when(reviewRepository.save(any(Review.class)))
                .thenReturn(mockReview);
        when(reviewRepository.avgRatingByBookId(1)).thenReturn(4.0);

        Review result = reviewService.addReview(
                1, "Test User", 1, 4, "Great book!");

        assertNotNull(result);
        assertEquals(4, result.getRating());
        assertEquals("Great book!", result.getComment());
        assertTrue(result.isVerified());
        verify(bookClient).updateRating(eq(1), anyDouble());
    }

    @Test
    void addReview_AlreadyReviewed_ThrowsException() {
        when(reviewRepository.findByBookIdAndUserId(1, 1))
                .thenReturn(Optional.of(mockReview));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.addReview(
                        1, "Test User", 1, 4, "Great book!"));
        assertTrue(ex.getMessage().contains("already reviewed"));
    }

    @Test
    void addReview_InvalidRatingTooHigh_ThrowsException() {
        when(reviewRepository.findByBookIdAndUserId(1, 1))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.addReview(
                        1, "Test User", 1, 6, "Great book!"));
        assertTrue(ex.getMessage().contains("Rating must be between 1 and 5"));
    }

    @Test
    void addReview_InvalidRatingTooLow_ThrowsException() {
        when(reviewRepository.findByBookIdAndUserId(1, 1))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.addReview(
                        1, "Test User", 1, 0, "Great book!"));
        assertTrue(ex.getMessage().contains("Rating must be between 1 and 5"));
    }

    @Test
    void addReview_NotVerifiedPurchaser_ThrowsException() {
        OrderResponse undeliveredOrder = new OrderResponse();
        undeliveredOrder.setBookId(1);
        undeliveredOrder.setOrderStatus("Placed");

        when(reviewRepository.findByBookIdAndUserId(1, 1))
                .thenReturn(Optional.empty());
        when(orderClient.getOrdersByUserId(1))
                .thenReturn(new OrderResponse[]{undeliveredOrder});

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.addReview(
                        1, "Test User", 1, 4, "Great book!"));
        assertTrue(ex.getMessage().contains("purchased and received"));
    }

    @Test
    void addReview_OrderServiceUnavailable_ThrowsException() {
        when(reviewRepository.findByBookIdAndUserId(1, 1))
                .thenReturn(Optional.empty());
        when(orderClient.getOrdersByUserId(1))
                .thenThrow(new RuntimeException("Service down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.addReview(
                        1, "Test User", 1, 4, "Great book!"));
        assertTrue(ex.getMessage().contains("Service down"));
    }

    @Test
    void addReview_NoOrdersForBook_ThrowsException() {
        OrderResponse otherBookOrder = new OrderResponse();
        otherBookOrder.setBookId(99);
        otherBookOrder.setOrderStatus("Delivered");

        when(reviewRepository.findByBookIdAndUserId(1, 1))
                .thenReturn(Optional.empty());
        when(orderClient.getOrdersByUserId(1))
                .thenReturn(new OrderResponse[]{otherBookOrder});

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.addReview(
                        1, "Test User", 1, 4, "Great book!"));
        assertTrue(ex.getMessage().contains("purchased and received"));
    }

    // ─── Get By Book ──────────────────────────────────────────────────────────

    @Test
    void getByBook_ReturnsReviews() {
        when(reviewRepository.findByBookId(1))
                .thenReturn(List.of(mockReview));

        List<Review> reviews = reviewService.getByBook(1);

        assertEquals(1, reviews.size());
        assertEquals(1, reviews.get(0).getBookId());
    }

    @Test
    void getByBook_NoReviews_ReturnsEmpty() {
        when(reviewRepository.findByBookId(1))
                .thenReturn(List.of());

        List<Review> reviews = reviewService.getByBook(1);

        assertTrue(reviews.isEmpty());
    }

    // ─── Get By User ──────────────────────────────────────────────────────────

    @Test
    void getByUser_ReturnsReviews() {
        when(reviewRepository.findByUserId(1))
                .thenReturn(List.of(mockReview));

        List<Review> reviews = reviewService.getByUser(1);

        assertEquals(1, reviews.size());
        assertEquals(1, reviews.get(0).getUserId());
    }

    // ─── Update Review ────────────────────────────────────────────────────────

    @Test
    void updateReview_Success() {
        when(reviewRepository.findById(1))
                .thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(any(Review.class)))
                .thenReturn(mockReview);
        when(reviewRepository.avgRatingByBookId(1)).thenReturn(4.5);

        Review result = reviewService.updateReview(1, 1, 5, "Excellent!");

        assertNotNull(result);
        verify(reviewRepository).save(any(Review.class));
        verify(bookClient).updateRating(eq(1), anyDouble());
    }

    @Test
    void updateReview_WrongUser_ThrowsException() {
        when(reviewRepository.findById(1))
                .thenReturn(Optional.of(mockReview));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.updateReview(1, 99, 5, "Excellent!"));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void updateReview_InvalidRating_ThrowsException() {
        when(reviewRepository.findById(1))
                .thenReturn(Optional.of(mockReview));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.updateReview(1, 1, 6, "Excellent!"));
        assertTrue(ex.getMessage().contains("Rating must be between 1 and 5"));
    }

    @Test
    void updateReview_NotFound_ThrowsException() {
        when(reviewRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> reviewService.updateReview(99, 1, 4, "Good!"));
    }

    // ─── Delete Review ────────────────────────────────────────────────────────

    @Test
    void deleteReview_Success() {
        when(reviewRepository.findById(1))
                .thenReturn(Optional.of(mockReview));
        when(reviewRepository.avgRatingByBookId(1)).thenReturn(0.0);

        assertDoesNotThrow(() -> reviewService.deleteReview(1, 1));
        verify(reviewRepository).deleteById(1);
        verify(bookClient).updateRating(eq(1), anyDouble());
    }

    @Test
    void deleteReview_WrongUser_ThrowsException() {
        when(reviewRepository.findById(1))
                .thenReturn(Optional.of(mockReview));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.deleteReview(1, 99));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void deleteReview_NotFound_ThrowsException() {
        when(reviewRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> reviewService.deleteReview(99, 1));
    }

    // ─── Get Average Rating ───────────────────────────────────────────────────

    @Test
    void getAvgRating_ReturnsRoundedAverage() {
        when(reviewRepository.avgRatingByBookId(1)).thenReturn(4.35);

        double avg = reviewService.getAvgRating(1);

        assertEquals(4.4, avg);
    }

    @Test
    void getAvgRating_NoReviews_ReturnsZero() {
        when(reviewRepository.avgRatingByBookId(1)).thenReturn(null);

        double avg = reviewService.getAvgRating(1);

        assertEquals(0.0, avg);
    }

    // ─── Get Review Count ─────────────────────────────────────────────────────

    @Test
    void getReviewCount_ReturnsCount() {
        when(reviewRepository.countByBookId(1)).thenReturn(5);

        int count = reviewService.getReviewCount(1);

        assertEquals(5, count);
    }

    // ─── Get All Reviews ──────────────────────────────────────────────────────

    @Test
    void getAllReviews_ReturnsList() {
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview));

        List<Review> reviews = reviewService.getAllReviews();

        assertEquals(1, reviews.size());
    }

    // ─── Get Review By ID ─────────────────────────────────────────────────────

    @Test
    void getReviewById_ReturnsReview() {
        when(reviewRepository.findById(1))
                .thenReturn(Optional.of(mockReview));

        Optional<Review> result = reviewService.getReviewById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getReviewId());
    }

    @Test
    void getReviewById_NotFound_ReturnsEmpty() {
        when(reviewRepository.findById(99))
                .thenReturn(Optional.empty());

        Optional<Review> result = reviewService.getReviewById(99);

        assertFalse(result.isPresent());
    }
}