package com.booknest.auth.resource;

import com.booknest.auth.entity.User;
import com.booknest.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthResource {

    @Autowired
    private AuthService authService;


    // Register
    // POST /auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User savedUser = authService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    // Login
    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");
            String token = authService.login(email, password);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Login successful");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }


    // Logout
    // POST /auth/logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            authService.logout(token);
            return ResponseEntity.ok("Logged out successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    
    // Validate Token
    // GET /auth/validate
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        boolean isValid = authService.validateToken(token);
        if (isValid) {
            return ResponseEntity.ok("Token is valid");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
        }
    }


    // Refresh Token
    // POST /auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token) {
        try {
            String newToken = authService.refreshToken(token);
            Map<String, String> response = new HashMap<>();
            response.put("token", newToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }


    // Get Profile
    // GET /auth/profile/{userId}
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable int userId) {
        try {
            Optional<User> user = authService.getUserById(userId);
            return user.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    // Update Profile
    // PUT /auth/profile
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User user) {
        try {
            User updatedUser = authService.register(user);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    // Change Password
    // PUT /auth/change-password/{userId}
    @PutMapping("/change-password/{userId}")
    public ResponseEntity<?> changePassword(
            @PathVariable int userId,
            @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("newPassword");
            authService.changePassword(userId, newPassword);
            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    // Get All Users by Role (Admin)
    // GET /auth/users/{role}
    @GetMapping("/users/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String role) {
        List<User> users = authService.getAllUsersByRole(role);
        return ResponseEntity.ok(users);
    }


    // Delete User (Admin)
    // DELETE /auth/users/{userId}
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable int userId) {
        try {
            authService.deleteUser(userId);
            return ResponseEntity.ok("User deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    
    // OAuth2 Success
    // GET /auth/oauth2/success
    @GetMapping("/oauth2/success")
    public ResponseEntity<?> oauth2Success(@AuthenticationPrincipal OAuth2User oauthUser) {
        try {
            String email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("name");

            // Check if user already exists, if not auto register
            User user;
            try {
                user = authService.getUserByEmail(email);
            } catch (RuntimeException e) {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFullName(name);
                newUser.setProvider("google");
                newUser.setRole("CUSTOMER");
                user = authService.register(newUser);
            }

            // Generate JWT token
            String token = authService.login(email, null);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Google login successful");
            response.put("email", email);
            response.put("name", name);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("OAuth2 login failed: " + e.getMessage());
        }
    }

    // OAuth2 Failure
    // GET /auth/oauth2/failure
    @GetMapping("/oauth2/failure")
    public ResponseEntity<?> oauth2Failure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Google login failed. Please try again.");
    }
}