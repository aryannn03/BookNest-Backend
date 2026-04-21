package com.booknest.book.resource;

import com.booknest.book.entity.Book;
import com.booknest.book.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/books")
public class BookResource {

    @Autowired
    private BookService bookService;


    // Add Book (Admin)
    // POST /books
    @PostMapping
    public ResponseEntity<?> addBook(@RequestBody Book book) {
        try {
            Book savedBook = bookService.addBook(book);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    // Get All Books
    // GET /books
    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }


    // Get Book By ID
    // GET /books/{bookId}
    @GetMapping("/{bookId}")
    public ResponseEntity<?> getBookById(@PathVariable int bookId) {
        Optional<Book> book = bookService.getBookById(bookId);
        return book.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }


    // Get Book By ISBN
    // GET /books/isbn/{isbn}
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<?> getBookByIsbn(@PathVariable String isbn) {
        Optional<Book> book = bookService.getBookByIsbn(isbn);
        return book.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }


    // Search Books by Keyword
    // GET /books/search?keyword=java
    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(
            @RequestParam(required = false) String keyword) {
        List<Book> books = bookService.searchBooks(keyword);
        return ResponseEntity.ok(books);
    }


    // Get Books By Genre
    // GET /books/genre/{genre}
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<Book>> getByGenre(@PathVariable String genre) {
        List<Book> books = bookService.getByGenre(genre);
        return ResponseEntity.ok(books);
    }


    // Get Books By Author
    // GET /books/author/{author}
    @GetMapping("/author/{author}")
    public ResponseEntity<List<Book>> getByAuthor(@PathVariable String author) {
        List<Book> books = bookService.getByAuthor(author);
        return ResponseEntity.ok(books);
    }


    // Get Books By Publisher
    // GET /books/publisher/{publisher}
    @GetMapping("/publisher/{publisher}")
    public ResponseEntity<List<Book>> getByPublisher(@PathVariable String publisher) {
        List<Book> books = bookService.getByPublisher(publisher);
        return ResponseEntity.ok(books);
    }


    // Get Books By Price Range
    // GET /books/price?min=100&max=500
    @GetMapping("/price")
    public ResponseEntity<?> getByPriceRange(
            @RequestParam double min,
            @RequestParam double max) {
        try {
            List<Book> books = bookService.getByPriceRange(min, max);
            return ResponseEntity.ok(books);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    // Get Featured Books
    // GET /books/featured
    @GetMapping("/featured")
    public ResponseEntity<List<Book>> getFeaturedBooks() {
        List<Book> books = bookService.getFeaturedBooks();
        return ResponseEntity.ok(books);
    }


    // Get Top Rated Books
    // GET /books/top-rated
    @GetMapping("/top-rated")
    public ResponseEntity<List<Book>> getTopRatedBooks() {
        List<Book> books = bookService.getTopRatedBooks();
        return ResponseEntity.ok(books);
    }


    // Get New Arrivals
    // GET /books/new-arrivals
    @GetMapping("/new-arrivals")
    public ResponseEntity<List<Book>> getNewArrivals() {
        List<Book> books = bookService.getNewArrivals();
        return ResponseEntity.ok(books);
    }


    // Get In Stock Books
    // GET /books/in-stock
    @GetMapping("/in-stock")
    public ResponseEntity<List<Book>> getInStockBooks() {
        List<Book> books = bookService.getInStockBooks();
        return ResponseEntity.ok(books);
    }


    // Update Book (Admin)
    // PUT /books
    @PutMapping
    public ResponseEntity<?> updateBook(@RequestBody Book book) {
        try {
            Book updatedBook = bookService.updateBook(book);
            return ResponseEntity.ok(updatedBook);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    // Delete Book (Admin)
    // DELETE /books/{bookId}
    @DeleteMapping("/{bookId}")
    public ResponseEntity<?> deleteBook(@PathVariable int bookId) {
        try {
            bookService.deleteBook(bookId);
            return ResponseEntity.ok("Book deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    // Update Stock (Admin)
    // PUT /books/{bookId}/stock?newStock=50\
    @PutMapping("/{bookId}/stock")
    public ResponseEntity<?> updateStock(
            @PathVariable int bookId,
            @RequestParam int newStock) {
        try {
            bookService.updateStock(bookId, newStock);
            return ResponseEntity.ok("Stock updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    // Update Rating (called by review-service)
    // PUT /books/{bookId}/rating?newRating=4.5
    @PutMapping("/{bookId}/rating")
    public ResponseEntity<?> updateRating(
            @PathVariable int bookId,
            @RequestParam double newRating) {
        try {
            bookService.updateRating(bookId, newRating);
            return ResponseEntity.ok("Rating updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}