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

    // Find by title (partial match)
    List<Book> findByTitleContainingIgnoreCase(String title);

    // Find by author (partial match)
    List<Book> findByAuthorContainingIgnoreCase(String author);

    // Find by genre
    List<Book> findByGenreIgnoreCase(String genre);

    // Find by ISBN
    Optional<Book> findByIsbn(String isbn);

    // Find by price range
    List<Book> findByPriceBetween(double minPrice, double maxPrice);

    // Find books with stock greater than zero
    List<Book> findByStockGreaterThan(int stock);

    // Find featured books
    List<Book> findByFeaturedTrue();

    // Full-text keyword search across title, author, genre, description
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.genre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Book> searchByKeyword(@Param("keyword") String keyword);

    // Find by publisher
    List<Book> findByPublisherIgnoreCase(String publisher);
}