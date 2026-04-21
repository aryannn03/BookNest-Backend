package com.booknest.auth.service.impl;

import com.booknest.auth.entity.User;
import com.booknest.auth.repository.UserRepository;
import com.booknest.auth.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    
    // Register
    @Override
    public User register(User user) {

        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered: " + user.getEmail());
        }

        // Hash the password before saving
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        // Set default role if not provided
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("CUSTOMER");
        }

        // Set provider as local
        user.setProvider("local");

        return userRepository.save(user);
    }

    // Login
    @Override
    public String login(String email, String password) {

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Skip password check for OAuth users
        if ("google".equals(user.getProvider())) {
            return generateToken(user);
        }
        
        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        // Generate and return JWT token
        return generateToken(user);
    }


    // Logout
    @Override
    public void logout(String token) {
        // Token invalidation handled on frontend by deleting the token
        // For server-side blacklisting, Redis can be added later
    }


    // Validate Token
    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    // Refresh Token
    @Override
    public String refreshToken(String token) {

        // Validate existing token first
        if (!validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }

        // Extract email from token
        String email = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        // Fetch user and generate new token
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return generateToken(user);
    }


    // Get User By Email
    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    // Change Password
    @Override
    public void changePassword(int userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    
    // Get All Users By Role
    @Override
    public List<User> getAllUsersByRole(String role) {
        return userRepository.findAllByRole(role);
    }


    // Delete User
    @Override
    public void deleteUser(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        userRepository.deleteByUserId(userId);
    }


    // Get User By ID
    @Override
    public Optional<User> getUserById(int userId) {
        return userRepository.findById(userId);
    }


    // Private Helper - Generate JWT Token
    private String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .claim("userId", user.getUserId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    // Private Helper - Get Signing Key
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}