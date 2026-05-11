package com.booknest.wishlist.repository;

import com.booknest.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository
        extends JpaRepository<Wishlist, Integer> {

    // Find wishlist by userId
    Optional<Wishlist> findByUserId(int userId);

    // Check if wishlist exists for user
    boolean existsByUserId(int userId);

    // Delete wishlist by userId
    void deleteByUserId(int userId);
}