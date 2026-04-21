package com.booknest.book.service;

import com.booknest.book.entity.Book;

import java.util.List;
import java.util.Optional;

public interface BookService {

    // Add a new book (Admin)
    Book addBook(Book book);

    // Get all books
    List<Book> getAllBooks();

    // Get book by ID
    Optional<Book> getBookById(int bookId);

    // Get book by ISBN
    Optional<Book> getBookByIsbn(String isbn);

    // Search books by keyword
    List<Book> searchBooks(String keyword);

    // Get books by genre
    List<Book> getByGenre(String genre);

    // Get books by author
    List<Book> getByAuthor(String author);

    // Get books by publisher
    List<Book> getByPublisher(String publisher);

    // Get books by price range
    List<Book> getByPriceRange(double minPrice, double maxPrice);

    // Get featured books
    List<Book> getFeaturedBooks();

    // Get top rated books
    List<Book> getTopRatedBooks();

    // Get new arrivals
    List<Book> getNewArrivals();

    // Get in stock books
    List<Book> getInStockBooks();

    // Update book details (Admin)
    Book updateBook(Book book);

    // Delete book (Admin)
    void deleteBook(int bookId);

    // Update stock level (Admin)
    void updateStock(int bookId, int newStock);

    // Update book rating (called by review-service)
    void updateRating(int bookId, double newRating);
}