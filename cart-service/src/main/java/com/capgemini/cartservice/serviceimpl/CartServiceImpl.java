package com.capgemini.cartservice.serviceimpl;

import com.capgemini.cartservice.client.BookClient;
import com.capgemini.cartservice.dto.BookResponse;
import com.capgemini.cartservice.entity.Cart;
import com.capgemini.cartservice.entity.CartItem;
import com.capgemini.cartservice.repository.CartRepository;
import com.capgemini.cartservice.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private BookClient bookClient;

    // ─── Get Cart By User ─────────────────────────────────────────────────────

    @Override
    public Cart getCartByUser(int userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    newCart.setTotalPrice(0.0);
                    return cartRepository.save(newCart);
                });

        // Refresh prices from Book Service
        for (CartItem item : cart.getItems()) {
            try {
                BookResponse book = bookClient.getBookById(item.getBookId());
                if (book != null) {
                    item.setPrice(book.getPrice());
                }
            } catch (Exception e) {
                // Keep old price if Book Service is unavailable
            }
        }

        recalculateTotal(cart);
        cartRepository.save(cart);

        return cart;
    }

    // ─── Add Item ─────────────────────────────────────────────────────────────

    @Override
    public Cart addItem(int userId, int bookId, int quantity) {
        Cart cart = getCartByUser(userId);

        // Fetch book details from book-service
        BookResponse book;
        try {
            book = bookClient.getBookById(bookId);
        } catch (Exception e) {
            throw new RuntimeException("Book service unavailable, please try again later");
        }

        if (book == null) {
            throw new RuntimeException("Book not found with id: " + bookId);
        }

        if (book.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock for book: " + book.getTitle());
        }

        // Check if book already in cart — update quantity instead
        for (CartItem item : cart.getItems()) {
            if (item.getBookId() == bookId) {
                item.setQuantity(item.getQuantity() + quantity);
                recalculateTotal(cart);
                return cartRepository.save(cart);
            }
        }

        // Add new cart item
        CartItem newItem = new CartItem();
        newItem.setBookId(bookId);
        newItem.setBookTitle(book.getTitle());
        newItem.setPrice(book.getPrice());
        newItem.setQuantity(quantity);
        newItem.setCart(cart);

        cart.getItems().add(newItem);
        recalculateTotal(cart);

        return cartRepository.save(cart);
    }

    // ─── Remove Item ──────────────────────────────────────────────────────────

    @Override
    public Cart removeItem(int userId, int itemId) {
        Cart cart = getCartByUser(userId);

        boolean removed = cart.getItems().removeIf(
                item -> item.getItemId() == itemId);

        if (!removed) {
            throw new RuntimeException("Item not found in cart: " + itemId);
        }

        recalculateTotal(cart);
        return cartRepository.save(cart);
    }

    // ─── Update Quantity ──────────────────────────────────────────────────────

    @Override
    public Cart updateQuantity(int userId, int itemId, int quantity) {
        Cart cart = getCartByUser(userId);

        if (quantity <= 0) {
            return removeItem(userId, itemId);
        }

        CartItem target = cart.getItems().stream()
                .filter(item -> item.getItemId() == itemId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Item not found in cart: " + itemId));

        target.setQuantity(quantity);
        recalculateTotal(cart);
        return cartRepository.save(cart);
    }

    // ─── Clear Cart ───────────────────────────────────────────────────────────

    @Override
    public void clearCart(int userId) {
        Cart cart = getCartByUser(userId);
        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);
    }

    // ─── Cart Total ───────────────────────────────────────────────────────────

    @Override
    public double cartTotal(int userId) {
        Cart cart = getCartByUser(userId);
        return cart.getTotalPrice();
    }

    // ─── Get All Carts ────────────────────────────────────────────────────────

    @Override
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    // ─── Helper — Recalculate Total ───────────────────────────────────────────

    private void recalculateTotal(Cart cart) {
        double total = cart.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        cart.setTotalPrice(total);
    }
}