package com.booknest.auth.resource;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.ForgotPasswordRequest;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.OtpRequest;
import com.booknest.auth.dto.OtpVerifyRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.dto.ResetPasswordRequest;
import com.booknest.auth.dto.UserProfileResponse;
import com.booknest.auth.entity.User;
import com.booknest.auth.service.AuthService;
import com.booknest.auth.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthResource {

    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    // ─── Register ────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ─── Validate Token ──────────────────────────────────────────────────────

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(
            @RequestParam String token) {
        boolean isValid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    // ─── Refresh Token ───────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(
            @RequestParam String token) {
        String newToken = authService.refreshToken(token);
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    // ─── Send OTP (registration email verification) ──────────────────────────

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtp(
            @Valid @RequestBody OtpRequest request) {

        boolean emailExists = authService.emailExists(request.getEmail());
        otpService.sendOtp(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent successfully to " + request.getEmail(),
                "emailRegistered", String.valueOf(emailExists)
        ));
    }

    // ─── Verify OTP (registration) ───────────────────────────────────────────

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {

        boolean verified = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if (!verified) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "verified", false,
                            "message", "Invalid or expired OTP"
                    ));
        }

        boolean emailRegistered = authService.emailExists(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "verified", true,
                "emailRegistered", emailRegistered,
                "message", emailRegistered
                        ? "Email verified. You can proceed to login."
                        : "Email verified. You can proceed to register."
        ));
    }

    // ─── Forgot Password — Step 1: Send OTP ──────────────────────────────────
    
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<Map<String, String>> forgotPasswordSendOtp(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.sendPasswordResetOtp(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "Password reset OTP sent to " + request.getEmail()
        ));
    }

    // ─── Forgot Password — Step 2: Verify OTP + Reset Password ───────────────
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(
                request.getEmail(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully. You can now log in."
        ));
    }

    // ─── Get Profile ─────────────────────────────────────────────────────────

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfileResponse> getProfile(
            @PathVariable int userId,
            Authentication authentication) {

        User user = authService.getUserById(userId);
        String currentEmail = authentication.getName();

        if (!user.getEmail().equals(currentEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(new UserProfileResponse(
                user.getUserId(), user.getFullName(), user.getEmail(),
                user.getRole(), user.getProvider(), user.getMobile()
        ));
    }

    // ─── Update Profile ──────────────────────────────────────────────────────

    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable int userId,
            @RequestBody User updatedUser,
            Authentication authentication) {

        User existing = authService.getUserById(userId);
        String currentEmail = authentication.getName();

        if (!existing.getEmail().equals(currentEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User saved = authService.updateProfile(userId, updatedUser);
        return ResponseEntity.ok(new UserProfileResponse(
                saved.getUserId(), saved.getFullName(), saved.getEmail(),
                saved.getRole(), saved.getProvider(), saved.getMobile()
        ));
    }

    // ─── Change Password ─────────────────────────────────────────────────────

    @PutMapping("/change-password/{userId}")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable int userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            Authentication authentication) {

        User user = authService.getUserById(userId);
        if (!user.getEmail().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        authService.changePassword(userId, oldPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // ─── OAuth2 Success Redirect ─────────────────────────────────────────────

    @GetMapping("/oauth2/success")
    public ResponseEntity<Map<String, String>> oauth2Success(
            @RequestParam String token) {
        return ResponseEntity.ok(Map.of(
                "token", token,
                "message", "GitHub login successful"
        ));
    }

    // ─── Admin — Get All Users ───────────────────────────────────────────────

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = authService.getUsersByRole("CUSTOMER");
        return ResponseEntity.ok(users);
    }

    // ─── Admin — Delete User ─────────────────────────────────────────────────

    @DeleteMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable int userId) {
        authService.deleteAccount(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<Map<String, String>> verifyForgotPasswordOtp(
            @RequestBody OtpVerifyRequest request) {

        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if (!valid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid or expired OTP"));
        }

        return ResponseEntity.ok(Map.of("message", "OTP verified successfully"));
    }
    
}