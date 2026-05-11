package com.booknest.wishlist.serviceimpl;

import com.booknest.wishlist.client.BookClient;
import com.booknest.wishlist.client.CartClient;
import com.booknest.wishlist.dto.BookResponse;
import com.booknest.wishlist.entity.Wishlist;
import com.booknest.wishlist.entity.WishlistItem;
import com.booknest.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private BookClient bookClient;

    @Mock
    private CartClient cartClient;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private Wishlist mockWishlist;
    private WishlistItem mockItem;
    private BookResponse mockBook;

    @BeforeEach
    void setUp() {
        mockBook = new BookResponse();
        mockBook.setBookId(1);
        mockBook.setTitle("Clean Code");
        mockBook.setPrice(499.0);
        mockBook.setStock(10);

        mockItem = new WishlistItem();
        mockItem.setItemId(1);
        mockItem.setBookId(1);
        mockItem.setBookTitle("Clean Code");
        mockItem.setBookPrice(499.0);

        mockWishlist = new Wishlist();
        mockWishlist.setWishlistId(1);
        mockWishlist.setUserId(1);
        mockWishlist.setBooks(new ArrayList<>(List.of(mockItem)));
        mockItem.setWishlist(mockWishlist);
    }

    // ─── Get Wishlist By User ─────────────────────────────────────────────────

    @Test
    void getWishlistByUser_ExistingWishlist_ReturnsWishlist() {
        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWishlist));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(mockWishlist);

        Wishlist result = wishlistService.getWishlistByUser(1);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
    }

    @Test
    void getWishlistByUser_NoWishlist_CreatesNew() {
        Wishlist newWishlist = new Wishlist();
        newWishlist.setUserId(1);
        newWishlist.setBooks(new ArrayList<>());

        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.empty());
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(newWishlist);

        Wishlist result = wishlistService.getWishlistByUser(1);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertTrue(result.getBooks().isEmpty());
    }

    @Test
    void getWishlistByUser_BookServiceUnavailable_KeepsOldPrice() {
        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWishlist));
        when(bookClient.getBookById(1))
                .thenThrow(new RuntimeException("Service down"));
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(mockWishlist);

        Wishlist result = wishlistService.getWishlistByUser(1);

        assertNotNull(result);
        assertEquals(499.0, result.getBooks().get(0).getBookPrice());
    }

    // ─── Add Book ─────────────────────────────────────────────────────────────

    @Test
    void addBook_Success() {
        Wishlist emptyWishlist = new Wishlist();
        emptyWishlist.setUserId(1);
        emptyWishlist.setBooks(new ArrayList<>());

        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(emptyWishlist));

        when(bookClient.getBookById(1))
                .thenReturn(mockBook);

        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(emptyWishlist);

        Wishlist result = wishlistService.addBook(1, 1);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());

        verify(wishlistRepository, times(2))
                .save(any(Wishlist.class));
    }

    @Test
    void addBook_AlreadyInWishlist_ThrowsException() {
        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWishlist));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(mockWishlist);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> wishlistService.addBook(1, 1));
        assertTrue(ex.getMessage().contains("already in wishlist"));
    }

    @Test
    void addBook_BookNotFound_ThrowsException() {
        Wishlist emptyWishlist = new Wishlist();
        emptyWishlist.setUserId(1);
        emptyWishlist.setBooks(new ArrayList<>());

        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(emptyWishlist));
        when(bookClient.getBookById(99)).thenReturn(null);
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(emptyWishlist);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> wishlistService.addBook(1, 99));
        assertTrue(ex.getMessage().contains("Book not found"));
    }

    @Test
    void addBook_BookServiceUnavailable_ThrowsException() {
        Wishlist emptyWishlist = new Wishlist();
        emptyWishlist.setUserId(1);
        emptyWishlist.setBooks(new ArrayList<>());

        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(emptyWishlist));
        when(bookClient.getBookById(1))
                .thenThrow(new RuntimeException("Service down"));
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(emptyWishlist);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> wishlistService.addBook(1, 1));
        assertTrue(ex.getMessage().contains("Book service unavailable"));
    }

    // ─── Remove Book ──────────────────────────────────────────────────────────

    @Test
    void removeBook_Success() {
        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWishlist));

        when(bookClient.getBookById(1))
                .thenReturn(mockBook);

        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(mockWishlist);

        Wishlist result = wishlistService.removeBook(1, 1);

        assertNotNull(result);
        assertTrue(result.getBooks().isEmpty());

        verify(wishlistRepository, times(2))
                .save(any(Wishlist.class));
    }

    @Test
    void removeBook_ItemNotFound_ThrowsException() {
        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWishlist));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(mockWishlist);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> wishlistService.removeBook(1, 99));
        assertTrue(ex.getMessage().contains("Item not found"));
    }

    // ─── Clear Wishlist ───────────────────────────────────────────────────────

    @Test
    void clearWishlist_Success() {
        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWishlist));

        when(bookClient.getBookById(1))
                .thenReturn(mockBook);

        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(mockWishlist);

        assertDoesNotThrow(() -> wishlistService.clearWishlist(1));

        assertTrue(mockWishlist.getBooks().isEmpty());

        verify(wishlistRepository, times(2))
                .save(any(Wishlist.class));
    }

    // ─── Move To Cart ─────────────────────────────────────────────────────────

    @Test
    void moveToCart_Success() {
        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWishlist));

        when(bookClient.getBookById(1))
                .thenReturn(mockBook);

        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(mockWishlist);

        assertDoesNotThrow(() ->
                wishlistService.moveToCart(1, 1, "Bearer token"));

        verify(cartClient).addToCart(eq("Bearer token"), eq(1), eq(1));

        assertTrue(mockWishlist.getBooks().isEmpty());

        verify(wishlistRepository, times(2))
                .save(any(Wishlist.class));
    }

    @Test
    void moveToCart_ItemNotFound_ThrowsException() {
        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWishlist));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(mockWishlist);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> wishlistService.moveToCart(1, 99, "Bearer token"));
        assertTrue(ex.getMessage().contains("Item not found"));
    }

    @Test
    void moveToCart_CartServiceUnavailable_ThrowsException() {
        when(wishlistRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWishlist));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(mockWishlist);
        when(cartClient.addToCart(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Service down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> wishlistService.moveToCart(1, 1, "Bearer token"));
        assertTrue(ex.getMessage().contains("Cart service unavailable"));
    }

    // ─── Get All Wishlists ────────────────────────────────────────────────────

    @Test
    void getAllWishlists_ReturnsList() {
        when(wishlistRepository.findAll())
                .thenReturn(List.of(mockWishlist));

        List<Wishlist> wishlists = wishlistService.getAllWishlists();

        assertEquals(1, wishlists.size());
        assertEquals(1, wishlists.get(0).getUserId());
    }
}