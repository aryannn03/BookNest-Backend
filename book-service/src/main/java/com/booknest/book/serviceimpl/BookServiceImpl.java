package com.booknest.book.serviceimpl;

import com.booknest.book.entity.Book;
import com.booknest.book.repository.BookRepository;
import com.booknest.book.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;
import java.util.Optional;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    // ─── Add Book ────────────────────────────────────────────────────────────
    @CacheEvict(value = {"books", "book", "featured-books"}, allEntries = true)
    @Override
    public Book addBook(Book book) {
        if (book.getIsbn() != null &&
            bookRepository.findByIsbn(book.getIsbn()).isPresent()) {
            throw new RuntimeException("Book with ISBN already exists: "
                    + book.getIsbn());
        }
        return bookRepository.save(book);
    }

    // ─── Get All Books ───────────────────────────────────────────────────────
    @Cacheable(value = "books")
    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // ─── Get Book By ID ──────────────────────────────────────────────────────

    @Override
    public Optional<Book> getBookById(int bookId) {
        return bookRepository.findById(bookId);
    }

    // ─── Get Book By ISBN ────────────────────────────────────────────────────

    @Override
    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    // ─── Search Books ────────────────────────────────────────────────────────

    @Override
    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return bookRepository.searchByKeyword(keyword.trim());
    }

    // ─── Get By Genre ────────────────────────────────────────────────────────

    @Override
    public List<Book> getByGenre(String genre) {
        return bookRepository.findByGenreIgnoreCase(genre);
    }

    // ─── Get By Author ───────────────────────────────────────────────────────

    @Override
    public List<Book> getByAuthor(String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }

    // ─── Update Book ─────────────────────────────────────────────────────────
    @CacheEvict(value = {"books", "book", "featured-books"}, allEntries = true)
    @Override
    public Book updateBook(Book book) {
        Book existing = bookRepository.findById(book.getBookId())
                .orElseThrow(() -> new RuntimeException(
                        "Book not found with id: " + book.getBookId()));

        existing.setTitle(book.getTitle());
        existing.setAuthor(book.getAuthor());
        existing.setIsbn(book.getIsbn());
        existing.setGenre(book.getGenre());
        existing.setPublisher(book.getPublisher());
        existing.setPrice(book.getPrice());
        existing.setStock(book.getStock());
        existing.setDescription(book.getDescription());
        existing.setCoverImageUrl(book.getCoverImageUrl());
        existing.setPublishedDate(book.getPublishedDate());
        existing.setFeatured(book.isFeatured());
        existing.setRating(book.getRating());

        return bookRepository.save(existing);
    }

    // ─── Delete Book ─────────────────────────────────────────────────────────
    @CacheEvict(value = {"books", "book", "featured-books"}, allEntries = true)
    @Override
    public void deleteBook(int bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new RuntimeException("Book not found with id: " + bookId);
        }
        bookRepository.deleteById(bookId);
    }

    // ─── Update Stock ────────────────────────────────────────────────────────

    @CacheEvict(value = {"books", "book", "featured-books"}, allEntries = true)
    @Override
    public void updateStock(int bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException(
                        "Book not found with id: " + bookId));

        if (quantity < 0) {
            throw new RuntimeException("Stock cannot be negative");
        }

        book.setStock(quantity);
        bookRepository.save(book);
    }
    
    @CacheEvict(value = {"books", "book", "featured-books"}, allEntries = true)
    @Override
    public void updateRating(int bookId, double rating) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
        book.setRating(rating);
        bookRepository.save(book);
    }

    // ─── Get Featured Books ──────────────────────────────────────────────────
    @Cacheable(value = "featured-books")
    @Override
    public List<Book> getFeaturedBooks() {
        return bookRepository.findByFeaturedTrue();
    }

    // ─── Get By Price Range ──────────────────────────────────────────────────

    @Override
    public List<Book> getByPriceRange(double min, double max) {
        return bookRepository.findByPriceBetween(min, max);
    }

    // ─── Get In Stock Books ──────────────────────────────────────────────────

    @Override
    public List<Book> getInStockBooks() {
        return bookRepository.findByStockGreaterThan(0);
    }
}