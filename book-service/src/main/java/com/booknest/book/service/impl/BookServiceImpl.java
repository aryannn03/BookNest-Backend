package com.booknest.book.service.impl;

import com.booknest.book.entity.Book;
import com.booknest.book.repository.BookRepository;
import com.booknest.book.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    
    // Add Book
    @Override
    public Book addBook(Book book) {

        // Check if ISBN already exists
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new RuntimeException("Book already exists with ISBN: " + book.getIsbn());
        }

        return bookRepository.save(book);
    }


    // Get All Books
    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }


    // Get Book By ID
    @Override
    public Optional<Book> getBookById(int bookId) {
        return bookRepository.findById(bookId);
    }


    // Get Book By ISBN
    @Override
    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    
    // Search Books by Keyword
    @Override
    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return bookRepository.findAll();
        }
        return bookRepository.searchByKeyword(keyword.trim());
    }

 
    // Get By Genre
    @Override
    public List<Book> getByGenre(String genre) {
        return bookRepository.findByGenreIgnoreCase(genre);
    }


    // Get By Author
    @Override
    public List<Book> getByAuthor(String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }


    // Get By Publisher
    @Override
    public List<Book> getByPublisher(String publisher) {
        return bookRepository.findByPublisherContainingIgnoreCase(publisher);
    }


    // Get By Price Range
    @Override
    public List<Book> getByPriceRange(double minPrice, double maxPrice) {
        if (minPrice < 0 || maxPrice < 0) {
            throw new RuntimeException("Price cannot be negative");
        }
        if (minPrice > maxPrice) {
            throw new RuntimeException("Min price cannot be greater than max price");
        }
        return bookRepository.findByPriceBetween(minPrice, maxPrice);
    }


    // Get Featured Books
    @Override
    public List<Book> getFeaturedBooks() {
        return bookRepository.findByFeaturedTrue();
    }


    // Get Top Rated Books
    @Override
    public List<Book> getTopRatedBooks() {
        return bookRepository.findTop10ByOrderByRatingDesc();
    }


    // Get New Arrivals
    @Override
    public List<Book> getNewArrivals() {
        return bookRepository.findTop10ByOrderByPublishedDateDesc();
    }


    // Get In Stock Books
    @Override
    public List<Book> getInStockBooks() {
        return bookRepository.findByStockGreaterThan(0);
    }


    // Update Book
    @Override
    public Book updateBook(Book book) {

        // Check if book exists
        bookRepository.findById(book.getBookId())
                .orElseThrow(() -> new RuntimeException(
                        "Book not found with id: " + book.getBookId()));

        return bookRepository.save(book);
    }


    // Delete Book
    @Override
    public void deleteBook(int bookId) {

        // Check if book exists
        if (!bookRepository.existsById(bookId)) {
            throw new RuntimeException("Book not found with id: " + bookId);
        }

        bookRepository.deleteById(bookId);
    }


    // Update Stock
    @Override
    public void updateStock(int bookId, int newStock) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException(
                        "Book not found with id: " + bookId));

        if (newStock < 0) {
            throw new RuntimeException("Stock cannot be negative");
        }

        book.setStock(newStock);
        bookRepository.save(book);
    }


    // Update Rating
    @Override
    public void updateRating(int bookId, double newRating) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException(
                        "Book not found with id: " + bookId));

        if (newRating < 0.0 || newRating > 5.0) {
            throw new RuntimeException("Rating must be between 0.0 and 5.0");
        }

        book.setRating(newRating);
        bookRepository.save(book);
    }
}