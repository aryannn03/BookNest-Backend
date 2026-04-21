package com.booknest.book.repository;

import com.booknest.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    // Find books by title (partial match)
    List<Book> findByTitleContainingIgnoreCase(String title);

    // Find books by author (partial match)
    List<Book> findByAuthorContainingIgnoreCase(String author);

    // Find books by genre
    List<Book> findByGenreIgnoreCase(String genre);

    // Find book by ISBN
    Optional<Book> findByIsbn(String isbn);

    // Find books within a price range
    List<Book> findByPriceBetween(double minPrice, double maxPrice);

    // Find books with stock greater than 0 (in stock)
    List<Book> findByStockGreaterThan(int stock);

    // Find featured books
    List<Book> findByFeaturedTrue();

    // Full text keyword search across title, author, genre
    @Query("SELECT b FROM Book b WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.genre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Book> searchByKeyword(@Param("keyword") String keyword);

    // Find books by publisher
    List<Book> findByPublisherContainingIgnoreCase(String publisher);

    // Find top rated books
    List<Book> findTop10ByOrderByRatingDesc();

    // Find new arrivals (sorted by published date)
    List<Book> findTop10ByOrderByPublishedDateDesc();

    // Check if ISBN already exists
    boolean existsByIsbn(String isbn);
}