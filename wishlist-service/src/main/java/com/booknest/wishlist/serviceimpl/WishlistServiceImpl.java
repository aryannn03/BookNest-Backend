package com.booknest.wishlist.serviceimpl;

import com.booknest.wishlist.client.BookClient;
import com.booknest.wishlist.client.CartClient;
import com.booknest.wishlist.dto.BookResponse;
import com.booknest.wishlist.entity.Wishlist;
import com.booknest.wishlist.entity.WishlistItem;
import com.booknest.wishlist.repository.WishlistRepository;
import com.booknest.wishlist.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishlistServiceImpl implements WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private BookClient bookClient;

    @Autowired
    private CartClient cartClient;

    // ─── Get Wishlist By User ─────────────────────────────────────────────────

    @Override
    public Wishlist getWishlistByUser(int userId) {

        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setUserId(userId);
                    return wishlistRepository.save(newWishlist);
                });

        // Refresh latest prices from Book Service
        for (WishlistItem item : wishlist.getBooks()) {
            try {
                BookResponse book = bookClient.getBookById(item.getBookId());
                if (book != null) {
                    item.setBookPrice(book.getPrice());
                }
            } catch (Exception e) {
                // Keep old price if Book Service unavailable
            }
        }

        wishlistRepository.save(wishlist);

        return wishlist;
    }

    // ─── Add Book ─────────────────────────────────────────────────────────────

    @Override
    public Wishlist addBook(int userId, int bookId) {
        Wishlist wishlist = getWishlistByUser(userId);

        // Check if book already in wishlist
        for (WishlistItem item : wishlist.getBooks()) {
            if (item.getBookId() == bookId) {
                throw new RuntimeException("Book already in wishlist");
            }
        }

        // Fetch book details from book-service
        BookResponse book;
        try {
        	book = bookClient.getBookById(bookId);
        } catch (Exception e) {
            throw new RuntimeException("Book service unavailable");
        }

        if (book == null) {
            throw new RuntimeException(
                    "Book not found with id: " + bookId);
        }

        // Add new wishlist item
        WishlistItem item = new WishlistItem();
        item.setBookId(bookId);
        item.setBookTitle(book.getTitle());
        item.setBookPrice(book.getPrice());
        item.setWishlist(wishlist);

        wishlist.getBooks().add(item);
        return wishlistRepository.save(wishlist);
    }

    // ─── Remove Book ──────────────────────────────────────────────────────────

    @Override
    public Wishlist removeBook(int userId, int itemId) {
        Wishlist wishlist = getWishlistByUser(userId);

        boolean removed = wishlist.getBooks().removeIf(
                item -> item.getItemId() == itemId);

        if (!removed) {
            throw new RuntimeException(
                    "Item not found in wishlist: " + itemId);
        }

        return wishlistRepository.save(wishlist);
    }

    // ─── Clear Wishlist ───────────────────────────────────────────────────────

    @Override
    public void clearWishlist(int userId) {
        Wishlist wishlist = getWishlistByUser(userId);
        wishlist.getBooks().clear();
        wishlistRepository.save(wishlist);
    }

    // ─── Move To Cart ─────────────────────────────────────────────────────────

    @Override
    public void moveToCart(int userId, int itemId, String authHeader) {
        Wishlist wishlist = getWishlistByUser(userId);

        // Find the item
        WishlistItem itemToMove = wishlist.getBooks().stream()
                .filter(item -> item.getItemId() == itemId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Item not found in wishlist: " + itemId));

        // Add to cart via cart-service
        try {
            cartClient.addToCart(authHeader,
                    itemToMove.getBookId(), 1);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cart service unavailable, please try again later");
        }

        // Remove from wishlist only after successful cart add
        wishlist.getBooks().remove(itemToMove);
        wishlistRepository.save(wishlist);
    }

    // ─── Get All Wishlists ────────────────────────────────────────────────────

    @Override
    public List<Wishlist> getAllWishlists() {
        return wishlistRepository.findAll();
    }
}