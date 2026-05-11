package com.capgemini.cartservice.serviceimpl;

import com.capgemini.cartservice.client.BookClient;
import com.capgemini.cartservice.dto.BookResponse;
import com.capgemini.cartservice.entity.Cart;
import com.capgemini.cartservice.entity.CartItem;
import com.capgemini.cartservice.repository.CartRepository;
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
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private BookClient bookClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart mockCart;
    private CartItem mockCartItem;
    private BookResponse mockBook;

    @BeforeEach
    void setUp() {
        mockBook = new BookResponse();
        mockBook.setBookId(1);
        mockBook.setTitle("Clean Code");
        mockBook.setPrice(499.0);
        mockBook.setStock(10);

        mockCartItem = new CartItem();
        mockCartItem.setItemId(1);
        mockCartItem.setBookId(1);
        mockCartItem.setBookTitle("Clean Code");
        mockCartItem.setPrice(499.0);
        mockCartItem.setQuantity(2);

        mockCart = new Cart();
        mockCart.setCartId(1);
        mockCart.setUserId(1);
        mockCart.setTotalPrice(998.0);
        mockCart.setItems(new ArrayList<>(List.of(mockCartItem)));
        mockCartItem.setCart(mockCart);
    }

    // ─── Get Cart By User ─────────────────────────────────────────────────────

    @Test
    void getCartByUser_ExistingCart_ReturnsCart() {
        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(mockCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        Cart result = cartService.getCartByUser(1);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
    }

    @Test
    void getCartByUser_NoCart_CreatesNewCart() {
        Cart newCart = new Cart();
        newCart.setUserId(1);
        newCart.setTotalPrice(0.0);
        newCart.setItems(new ArrayList<>());

        when(cartRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        Cart result = cartService.getCartByUser(1);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals(0.0, result.getTotalPrice());
    }

    // ─── Add Item ─────────────────────────────────────────────────────────────

    @Test
    void addItem_NewBook_AddsToCart() {
        Cart emptyCart = new Cart();
        emptyCart.setUserId(1);
        emptyCart.setItems(new ArrayList<>());
        emptyCart.setTotalPrice(0.0);

        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(emptyCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        Cart result = cartService.addItem(1, 1, 2);

        assertNotNull(result);
        verify(cartRepository, atLeastOnce()).save(any(Cart.class));
    }

    @Test
    void addItem_ExistingBook_UpdatesQuantity() {
        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(mockCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        Cart result = cartService.addItem(1, 1, 1);

        assertNotNull(result);
        assertEquals(3, mockCart.getItems().get(0).getQuantity());
    }

    @Test
    void addItem_InsufficientStock_ThrowsException() {
        Cart emptyCart = new Cart();
        emptyCart.setUserId(1);
        emptyCart.setItems(new ArrayList<>());

        mockBook.setStock(1);

        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(emptyCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.addItem(1, 1, 5));
        assertTrue(ex.getMessage().contains("Insufficient stock"));
    }

    @Test
    void addItem_BookServiceUnavailable_ThrowsException() {
        Cart emptyCart = new Cart();
        emptyCart.setUserId(1);
        emptyCart.setItems(new ArrayList<>());

        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(emptyCart));
        when(bookClient.getBookById(1)).thenThrow(new RuntimeException("Service down"));
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.addItem(1, 1, 1));
        assertTrue(ex.getMessage().contains("Book service unavailable"));
    }

    // ─── Remove Item ──────────────────────────────────────────────────────────

    @Test
    void removeItem_Success() {
        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(mockCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        Cart result = cartService.removeItem(1, 1);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void removeItem_ItemNotFound_ThrowsException() {
        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(mockCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.removeItem(1, 99));
        assertTrue(ex.getMessage().contains("Item not found"));
    }

    // ─── Update Quantity ──────────────────────────────────────────────────────

    @Test
    void updateQuantity_Success() {
        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(mockCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        Cart result = cartService.updateQuantity(1, 1, 5);

        assertNotNull(result);
        assertEquals(5, mockCart.getItems().get(0).getQuantity());
    }

    @Test
    void updateQuantity_ZeroQuantity_RemovesItem() {
        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(mockCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        Cart result = cartService.updateQuantity(1, 1, 0);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void updateQuantity_ItemNotFound_ThrowsException() {
        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(mockCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.updateQuantity(1, 99, 3));
        assertTrue(ex.getMessage().contains("Item not found"));
    }

    // ─── Clear Cart ───────────────────────────────────────────────────────────

    @Test
    void clearCart_Success() {
        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(mockCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        assertDoesNotThrow(() -> cartService.clearCart(1));

        assertTrue(mockCart.getItems().isEmpty());
        assertEquals(0.0, mockCart.getTotalPrice());
        verify(cartRepository, atLeastOnce()).save(any(Cart.class));
    }

    // ─── Cart Total ───────────────────────────────────────────────────────────

    @Test
    void cartTotal_ReturnsCorrectTotal() {
        when(cartRepository.findByUserId(1)).thenReturn(Optional.of(mockCart));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        double total = cartService.cartTotal(1);

        assertEquals(998.0, total);
    }

    // ─── Get All Carts ────────────────────────────────────────────────────────

    @Test
    void getAllCarts_ReturnsList() {
        when(cartRepository.findAll()).thenReturn(List.of(mockCart));

        List<Cart> carts = cartService.getAllCarts();

        assertEquals(1, carts.size());
    }
}