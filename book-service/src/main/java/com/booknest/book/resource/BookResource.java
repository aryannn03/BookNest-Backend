package com.booknest.book.resource;

import com.booknest.book.entity.Book;
import com.booknest.book.service.BookService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/books")
public class BookResource {

    @Autowired
    private BookService bookService;

    // ─── Add Book (Admin only) ────────────────────────────────────────────────

    @PostMapping("/add")
    public ResponseEntity<Book> addBook(@Valid @RequestBody Book book) {
        Book saved = bookService.addBook(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ─── Get All Books ────────────────────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    // ─── Get Book By ID ───────────────────────────────────────────────────────

    @GetMapping("/{bookId}")
    public ResponseEntity<?> getBookById(@PathVariable int bookId) {
        return bookService.getBookById(bookId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Get Book By ISBN ─────────────────────────────────────────────────────

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<?> getBookByIsbn(@PathVariable String isbn) {
        return bookService.getBookByIsbn(isbn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Search Books By Keyword ──────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(
            @RequestParam String keyword) {
        return ResponseEntity.ok(bookService.searchBooks(keyword));
    }

    // ─── Get Books By Genre ───────────────────────────────────────────────────

    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<Book>> getByGenre(
            @PathVariable String genre) {
        return ResponseEntity.ok(bookService.getByGenre(genre));
    }

    // ─── Get Books By Author ──────────────────────────────────────────────────

    @GetMapping("/author/{author}")
    public ResponseEntity<List<Book>> getByAuthor(
            @PathVariable String author) {
        return ResponseEntity.ok(bookService.getByAuthor(author));
    }

    // ─── Get Featured Books ───────────────────────────────────────────────────

    @GetMapping("/featured")
    public ResponseEntity<List<Book>> getFeaturedBooks() {
        return ResponseEntity.ok(bookService.getFeaturedBooks());
    }

    // ─── Get In Stock Books ───────────────────────────────────────────────────

    @GetMapping("/in-stock")
    public ResponseEntity<List<Book>> getInStockBooks() {
        return ResponseEntity.ok(bookService.getInStockBooks());
    }

    // ─── Get Books By Price Range ─────────────────────────────────────────────

    @GetMapping("/price-range")
    public ResponseEntity<List<Book>> getByPriceRange(
            @RequestParam double min,
            @RequestParam double max) {
        return ResponseEntity.ok(bookService.getByPriceRange(min, max));
    }

    // ─── Update Book (Admin only) ─────────────────────────────────────────────

    @PutMapping("/update")
    public ResponseEntity<Book> updateBook(@Valid @RequestBody Book book) {
        return ResponseEntity.ok(bookService.updateBook(book));
    }

    // ─── Update Stock (Admin only) ────────────────────────────────────────────

    @PutMapping("/update-stock/{bookId}")
    public ResponseEntity<Map<String, String>> updateStock(
            @PathVariable int bookId,
            @RequestParam int quantity) {
        bookService.updateStock(bookId, quantity);
        return ResponseEntity.ok(Map.of(
                "message", "Stock updated successfully",
                "bookId", String.valueOf(bookId),
                "newStock", String.valueOf(quantity)
        ));
    }

    // ─── Delete Book (Admin only) ─────────────────────────────────────────────

    @DeleteMapping("/delete/{bookId}")
    public ResponseEntity<Map<String, String>> deleteBook(
            @PathVariable int bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.ok(Map.of("message",
                "Book deleted successfully"));
    }
    
    // ─── Update Book Rating (internal call from review-service) ───────────────

    @PutMapping("/update-rating/{bookId}")
    public ResponseEntity<Map<String, String>> updateRating(
            @PathVariable int bookId,
            @RequestParam double rating) {
        bookService.updateRating(bookId, rating);
        return ResponseEntity.ok(Map.of(
            "message", "Rating updated successfully",
            "newRating", String.valueOf(rating)
        ));
    }
}