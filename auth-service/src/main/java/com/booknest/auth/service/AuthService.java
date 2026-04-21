package com.booknest.auth.service;

import com.booknest.auth.entity.User;

import java.util.List;
import java.util.Optional;

public interface AuthService {

    // Register new user with email and password
    User register(User user);

    // Login with email and password, returns JWT token
    String login(String email, String password);

    // Logout - invalidate token
    void logout(String token);

    // Validate JWT token
    boolean validateToken(String token);

    // Refresh expired JWT token
    String refreshToken(String token);

    // Get user by email
    User getUserByEmail(String email);

    // Change password
    void changePassword(int userId, String newPassword);

    // Get all users by role (admin use)
    List<User> getAllUsersByRole(String role);

    // Delete user account
    void deleteUser(int userId);

    // Get user by ID
    Optional<User> getUserById(int userId);
}