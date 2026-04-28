package com.capgemini.cartservice.serviceimpl;

import com.capgemini.cartservice.entity.Cart;
import com.capgemini.cartservice.entity.CartItem;
import com.capgemini.cartservice.repository.CartRepository;
import com.capgemini.cartservice.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${book.service.url}")
    private String bookServiceUrl;

    // ─── Get Cart By User ─────────────────────────────────────────────────────

    @Override
    public Cart getCartByUser(int userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Auto-create cart if not exists
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    newCart.setTotalPrice(0.0);
                    return cartRepository.save(newCart);
                });
    }

    // ─── Add Item ─────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    @Override
    public Cart addItem(int userId, int bookId, int quantity) {
        Cart cart = getCartByUser(userId);

        // Fetch book details from book-service
        Map<String, Object> bookDetails = restTemplate.getForObject(
                bookServiceUrl + "/books/" + bookId, Map.class);

        if (bookDetails == null) {
            throw new RuntimeException("Book not found with id: " + bookId);
        }

        String bookTitle = (String) bookDetails.get("title");
        double price = ((Number) bookDetails.get("price")).doubleValue();
        int stock = ((Number) bookDetails.get("stock")).intValue();

        if (stock < quantity) {
            throw new RuntimeException("Insufficient stock for book: "
                    + bookTitle);
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
        newItem.setBookTitle(bookTitle);
        newItem.setPrice(price);
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

        cart.getItems().removeIf(item -> item.getItemId() == itemId);
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

        for (CartItem item : cart.getItems()) {
            if (item.getItemId() == itemId) {
                item.setQuantity(quantity);
                break;
            }
        }

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