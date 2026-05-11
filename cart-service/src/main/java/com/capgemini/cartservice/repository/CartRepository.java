package com.capgemini.cartservice.repository;

import com.capgemini.cartservice.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    // Find cart by userId
    Optional<Cart> findByUserId(int userId);

    // Check if cart exists for user
    boolean existsByUserId(int userId);

    // Delete cart by userId
    void deleteByUserId(int userId);
}