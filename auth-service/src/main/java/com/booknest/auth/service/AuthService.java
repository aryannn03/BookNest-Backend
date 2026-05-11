package com.booknest.auth.service;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.entity.User;

import java.util.List;

public interface AuthService {

    // Register new user
    AuthResponse register(RegisterRequest request);

    // Login with email and password
    AuthResponse login(LoginRequest request);

    // Validate JWT token
    boolean validateToken(String token);

    // Refresh JWT token
    String refreshToken(String token);

    // Check if an email is already registered
    boolean emailExists(String email);

    // Get user by email
    User getUserByEmail(String email);

    // Get user by ID
    User getUserById(int userId);

    // Get all users by role
    List<User> getUsersByRole(String role);

    // Update user profile
    User updateProfile(int userId, User updatedUser);

    // Change password
    void changePassword(int userId, String oldPassword, String newPassword);

    // ─── Forgot / Reset Password ─────────────────────────────────────────────


    // Step 1 — Initiate forgot-password flow.

    void sendPasswordResetOtp(String email);


    // Step 2 — Verify OTP + reset password atomically.
    void resetPassword(String email, String newPassword);

    // Delete account
    void deleteAccount(int userId);

    // Logout and blacklist token
    void logout(String token);

    // Check if token is blacklisted
    boolean isTokenBlacklisted(String token);
}