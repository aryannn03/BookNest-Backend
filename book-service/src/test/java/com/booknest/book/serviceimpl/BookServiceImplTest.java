package com.booknest.book.serviceimpl;

import com.booknest.book.entity.Book;
import com.booknest.book.repository.BookRepository;
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
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book mockBook;

    @BeforeEach
    void setUp() {
        mockBook = new Book();
        mockBook.setBookId(1);
        mockBook.setTitle("Clean Code");
        mockBook.setAuthor("Robert C. Martin");
        mockBook.setIsbn("9780132350884");
        mockBook.setPrice(499.0);
        mockBook.setStock(10);
        mockBook.setGenre("Programming");
        mockBook.setFeatured(true);
        mockBook.setRating(4.5);
    }

    // ─── Add Book ─────────────────────────────────────────────────────────────

    @Test
    void addBook_Success() {
        when(bookRepository.findByIsbn(mockBook.getIsbn()))
                .thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenReturn(mockBook);

        Book saved = bookService.addBook(mockBook);

        assertNotNull(saved);
        assertEquals("Clean Code", saved.getTitle());
        verify(bookRepository).save(mockBook);
    }

    @Test
    void addBook_DuplicateIsbn_ThrowsException() {
        when(bookRepository.findByIsbn(mockBook.getIsbn()))
                .thenReturn(Optional.of(mockBook));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookService.addBook(mockBook));
        assertTrue(ex.getMessage().contains("ISBN already exists"));
    }

    // ─── Get All Books ────────────────────────────────────────────────────────

    @Test
    void getAllBooks_ReturnsBookList() {
        when(bookRepository.findAll()).thenReturn(List.of(mockBook));

        List<Book> books = bookService.getAllBooks();

        assertEquals(1, books.size());
        assertEquals("Clean Code", books.get(0).getTitle());
    }

    @Test
    void getAllBooks_EmptyList_ReturnsEmpty() {
        when(bookRepository.findAll()).thenReturn(List.of());

        List<Book> books = bookService.getAllBooks();

        assertTrue(books.isEmpty());
    }

    // ─── Get Book By ID ───────────────────────────────────────────────────────

    @Test
    void getBookById_BookExists_ReturnsBook() {
        when(bookRepository.findById(1)).thenReturn(Optional.of(mockBook));

        Optional<Book> result = bookService.getBookById(1);

        assertTrue(result.isPresent());
        assertEquals("Clean Code", result.get().getTitle());
    }

    @Test
    void getBookById_BookNotFound_ReturnsEmpty() {
        when(bookRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Book> result = bookService.getBookById(99);

        assertFalse(result.isPresent());
    }

    // ─── Get Book By ISBN ─────────────────────────────────────────────────────

    @Test
    void getBookByIsbn_BookExists_ReturnsBook() {
        when(bookRepository.findByIsbn("9780132350884"))
                .thenReturn(Optional.of(mockBook));

        Optional<Book> result = bookService.getBookByIsbn("9780132350884");

        assertTrue(result.isPresent());
        assertEquals("Clean Code", result.get().getTitle());
    }

    @Test
    void getBookByIsbn_NotFound_ReturnsEmpty() {
        when(bookRepository.findByIsbn("0000000000"))
                .thenReturn(Optional.empty());

        Optional<Book> result = bookService.getBookByIsbn("0000000000");

        assertFalse(result.isPresent());
    }

    // ─── Search Books ─────────────────────────────────────────────────────────

    @Test
    void searchBooks_ValidKeyword_ReturnsResults() {
        when(bookRepository.searchByKeyword("clean"))
                .thenReturn(List.of(mockBook));

        List<Book> results = bookService.searchBooks("clean");

        assertEquals(1, results.size());
    }

    @Test
    void searchBooks_EmptyKeyword_ReturnsEmptyList() {
        List<Book> results = bookService.searchBooks("");

        assertTrue(results.isEmpty());
        verify(bookRepository, never()).searchByKeyword(any());
    }

    @Test
    void searchBooks_NullKeyword_ReturnsEmptyList() {
        List<Book> results = bookService.searchBooks(null);

        assertTrue(results.isEmpty());
        verify(bookRepository, never()).searchByKeyword(any());
    }

    // ─── Get By Genre ─────────────────────────────────────────────────────────

    @Test
    void getByGenre_ReturnsBooks() {
        when(bookRepository.findByGenreIgnoreCase("Programming"))
                .thenReturn(List.of(mockBook));

        List<Book> books = bookService.getByGenre("Programming");

        assertEquals(1, books.size());
        assertEquals("Programming", books.get(0).getGenre());
    }

    // ─── Get By Author ────────────────────────────────────────────────────────

    @Test
    void getByAuthor_ReturnsBooks() {
        when(bookRepository.findByAuthorContainingIgnoreCase("Martin"))
                .thenReturn(List.of(mockBook));

        List<Book> books = bookService.getByAuthor("Martin");

        assertEquals(1, books.size());
        assertEquals("Robert C. Martin", books.get(0).getAuthor());
    }

    // ─── Update Book ──────────────────────────────────────────────────────────

    @Test
    void updateBook_Success() {
        Book updated = new Book();
        updated.setBookId(1);
        updated.setTitle("Clean Code Updated");
        updated.setAuthor("Robert C. Martin");
        updated.setIsbn("9780132350884");
        updated.setPrice(599.0);
        updated.setStock(5);

        when(bookRepository.findById(1)).thenReturn(Optional.of(mockBook));
        when(bookRepository.save(any(Book.class))).thenReturn(updated);

        Book result = bookService.updateBook(updated);

        assertNotNull(result);
        assertEquals("Clean Code Updated", result.getTitle());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void updateBook_BookNotFound_ThrowsException() {
        Book updated = new Book();
        updated.setBookId(99);

        when(bookRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> bookService.updateBook(updated));
    }

    // ─── Delete Book ──────────────────────────────────────────────────────────

    @Test
    void deleteBook_Success() {
        when(bookRepository.existsById(1)).thenReturn(true);

        assertDoesNotThrow(() -> bookService.deleteBook(1));
        verify(bookRepository).deleteById(1);
    }

    @Test
    void deleteBook_BookNotFound_ThrowsException() {
        when(bookRepository.existsById(99)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> bookService.deleteBook(99));
    }

    // ─── Update Stock ─────────────────────────────────────────────────────────

    @Test
    void updateStock_Success() {
        when(bookRepository.findById(1)).thenReturn(Optional.of(mockBook));
        when(bookRepository.save(any(Book.class))).thenReturn(mockBook);

        assertDoesNotThrow(() -> bookService.updateStock(1, 20));
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void updateStock_NegativeQuantity_ThrowsException() {
        when(bookRepository.findById(1)).thenReturn(Optional.of(mockBook));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookService.updateStock(1, -5));
        assertTrue(ex.getMessage().contains("Stock cannot be negative"));
    }

    @Test
    void updateStock_BookNotFound_ThrowsException() {
        when(bookRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> bookService.updateStock(99, 10));
    }

    // ─── Update Rating ────────────────────────────────────────────────────────

    @Test
    void updateRating_Success() {
        when(bookRepository.findById(1)).thenReturn(Optional.of(mockBook));
        when(bookRepository.save(any(Book.class))).thenReturn(mockBook);

        assertDoesNotThrow(() -> bookService.updateRating(1, 4.8));
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void updateRating_BookNotFound_ThrowsException() {
        when(bookRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> bookService.updateRating(99, 4.8));
    }

    // ─── Get Featured Books ───────────────────────────────────────────────────

    @Test
    void getFeaturedBooks_ReturnsFeaturedBooks() {
        when(bookRepository.findByFeaturedTrue()).thenReturn(List.of(mockBook));

        List<Book> books = bookService.getFeaturedBooks();

        assertEquals(1, books.size());
        assertTrue(books.get(0).isFeatured());
    }

    // ─── Get By Price Range ───────────────────────────────────────────────────

    @Test
    void getByPriceRange_ReturnsBooks() {
        when(bookRepository.findByPriceBetween(100.0, 600.0))
                .thenReturn(List.of(mockBook));

        List<Book> books = bookService.getByPriceRange(100.0, 600.0);

        assertEquals(1, books.size());
        assertEquals(499.0, books.get(0).getPrice());
    }

    // ─── Get In Stock Books ───────────────────────────────────────────────────

    @Test
    void getInStockBooks_ReturnsInStockBooks() {
        when(bookRepository.findByStockGreaterThan(0))
                .thenReturn(List.of(mockBook));

        List<Book> books = bookService.getInStockBooks();

        assertEquals(1, books.size());
        assertTrue(books.get(0).getStock() > 0);
    }
}