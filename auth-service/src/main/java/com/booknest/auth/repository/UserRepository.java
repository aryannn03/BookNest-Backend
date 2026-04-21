package com.booknest.auth.repository;

import com.booknest.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Find user by email (used during login)
    Optional<User> findByEmail(String email);

    // Check if email already registered (used during registration)
    boolean existsByEmail(String email);

    // Find all users by role (used by admin to list all customers)
    List<User> findAllByRole(String role);

    // Delete user by userId (used by admin)
    void deleteByUserId(int userId);
}