package com.booknest.auth.serviceimpl;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.entity.User;
import com.booknest.auth.repository.UserRepository;
import com.booknest.auth.security.JwtUtil;
import com.booknest.auth.service.AuthService;
import com.booknest.auth.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OtpService otpService;

    // ─── Register ────────────────────────────────────────────────────────────

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (!otpService.isEmailVerified(request.getEmail())) {
            throw new RuntimeException(
                "Email not verified. Please verify your email with OTP before registering.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setMobile(request.getMobile());
        user.setRole(request.getRole() != null ? request.getRole().toUpperCase() : "CUSTOMER");
        user.setProvider("LOCAL");

        User saved = userRepository.save(user);
        otpService.consumeVerifiedFlag(request.getEmail());

        String token = jwtUtil.generateToken(
                saved.getEmail(), saved.getRole(), saved.getUserId());

        return new AuthResponse(
                token, saved.getRole(), saved.getUserId(),
                saved.getFullName(), saved.getEmail(), "Registration successful");
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "User not found with email: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(), user.getRole(), user.getUserId());

        return new AuthResponse(
                token, user.getRole(), user.getUserId(),
                user.getFullName(), user.getEmail(), "Login successful");
    }

    // ─── Validate Token ──────────────────────────────────────────────────────

    @Override
    public boolean validateToken(String token) {
        if (isTokenBlacklisted(token)) return false;
        return jwtUtil.validateToken(token);
    }

    // ─── Refresh Token ───────────────────────────────────────────────────────

    @Override
    public String refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        return jwtUtil.refreshToken(token);
    }

    // ─── Email Exists ─────────────────────────────────────────────────────────

    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    // ─── Get User By Email ───────────────────────────────────────────────────

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    // ─── Get User By ID ──────────────────────────────────────────────────────

    @Override
    public User getUserById(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found with id: " + userId));
    }

    // ─── Get Users By Role ───────────────────────────────────────────────────

    @Override
    public List<User> getUsersByRole(String role) {
        return userRepository.findAllByRole(role.toUpperCase());
    }

    // ─── Update Profile ──────────────────────────────────────────────────────

    @Override
    public User updateProfile(int userId, User updatedUser) {
        User existing = getUserById(userId);
        existing.setFullName(updatedUser.getFullName());
        existing.setMobile(updatedUser.getMobile());
        return userRepository.save(existing);
    }

    // ─── Change Password (logged-in user) ────────────────────────────────────

    @Override
    public void changePassword(int userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Old password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ─── Forgot Password — Send OTP ──────────────────────────────────────────

    @Override
    public void sendPasswordResetOtp(String email) {
        // Ensure the account exists before sending an OTP
        if (!userRepository.existsByEmail(email)) {
            throw new RuntimeException("No account found with email: " + email);
        }
        // Delegate to OtpService — same Redis + email infrastructure
        otpService.sendOtp(email);
    }

    // ─── Reset Password — Verify OTP + Update ────────────────────────────────

    @Override
    public void resetPassword(String email, String newPassword) {

        // Check verified flag (set after OTP verification)
        if (!otpService.isEmailVerified(email)) {
            throw new RuntimeException("OTP not verified. Please verify OTP first.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "No account found with email: " + email));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Clean verified flag
        otpService.consumeVerifiedFlag(email);
    }

    // ─── Delete Account ──────────────────────────────────────────────────────

    @Override
    public void deleteAccount(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    @Override
    public void logout(String token) {
        redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "true",
                Duration.ofMillis(jwtExpiration)
        );
    }

    // ─── Token Blacklist Check ───────────────────────────────────────────────

    @Override
    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }
}