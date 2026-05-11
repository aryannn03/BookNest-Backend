package com.booknest.book.service;

import com.booknest.book.entity.Book;

import java.util.List;
import java.util.Optional;

public interface BookService {

    // Add new book
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

    // Update book details
    Book updateBook(Book book);

    // Delete book by ID
    void deleteBook(int bookId);

    // Update stock level
    void updateStock(int bookId, int quantity);
    
    void updateRating(int bookId, double rating);

    // Get featured books
    List<Book> getFeaturedBooks();

    // Get books by price range
    List<Book> getByPriceRange(double min, double max);

    // Get in-stock books only
    List<Book> getInStockBooks();
}