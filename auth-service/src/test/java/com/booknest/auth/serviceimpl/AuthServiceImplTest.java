package com.booknest.auth.serviceimpl;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.entity.User;
import com.booknest.auth.repository.UserRepository;
import com.booknest.auth.security.JwtUtil;
import com.booknest.auth.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private OtpService otpService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService,
                "jwtExpiration", 86400000L);

        mockUser = new User();
        mockUser.setUserId(1);
        mockUser.setFullName("Test User");
        mockUser.setEmail("test@example.com");
        mockUser.setPasswordHash("hashedPassword");
        mockUser.setRole("CUSTOMER");
        mockUser.setProvider("LOCAL");
    }

    // ─── Register ─────────────────────────────────────────────────────────────

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setRole("CUSTOMER");

        when(otpService.isEmailVerified("test@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyInt()))
                .thenReturn("mockToken");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());
        assertEquals("CUSTOMER", response.getRole());
        assertEquals("Registration successful", response.getMessage());
        verify(otpService).consumeVerifiedFlag("test@example.com");
    }

    @Test
    void register_EmailNotVerified_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        when(otpService.isEmailVerified("test@example.com")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Email not verified"));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        when(otpService.isEmailVerified("test@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Email already registered"));
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "hashedPassword"))
                .thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), anyInt()))
                .thenReturn("mockToken");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword"))
                .thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Invalid password"));
    }

    // ─── Validate Token ───────────────────────────────────────────────────────

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        when(redisTemplate.hasKey("blacklist:validToken")).thenReturn(false);
        when(jwtUtil.validateToken("validToken")).thenReturn(true);

        assertTrue(authService.validateToken("validToken"));
    }

    @Test
    void validateToken_BlacklistedToken_ReturnsFalse() {
        when(redisTemplate.hasKey("blacklist:blacklistedToken")).thenReturn(true);

        assertFalse(authService.validateToken("blacklistedToken"));
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        when(redisTemplate.hasKey("blacklist:invalidToken")).thenReturn(false);
        when(jwtUtil.validateToken("invalidToken")).thenReturn(false);

        assertFalse(authService.validateToken("invalidToken"));
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Test
    void logout_BlacklistsToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout("someToken");

        verify(valueOperations).set(eq("blacklist:someToken"),
                eq("true"), any());
    }

    // ─── Get User By ID ───────────────────────────────────────────────────────

    @Test
    void getUserById_UserExists_ReturnsUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        User user = authService.getUserById(1);

        assertNotNull(user);
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    void getUserById_UserNotFound_ThrowsException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.getUserById(99));
    }

    // ─── Get Users By Role ────────────────────────────────────────────────────

    @Test
    void getUsersByRole_ReturnsUserList() {
        when(userRepository.findAllByRole("CUSTOMER"))
                .thenReturn(List.of(mockUser));

        List<User> users = authService.getUsersByRole("CUSTOMER");

        assertEquals(1, users.size());
        assertEquals("CUSTOMER", users.get(0).getRole());
    }

    // ─── Change Password ──────────────────────────────────────────────────────

    @Test
    void changePassword_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("oldPass", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        assertDoesNotThrow(() -> authService.changePassword(1, "oldPass", "newPass"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_WrongOldPassword_ThrowsException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongOld", "hashedPassword")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.changePassword(1, "wrongOld", "newPass"));
        assertTrue(ex.getMessage().contains("Old password is incorrect"));
    }

    // ─── Reset Password ───────────────────────────────────────────────────────

    @Test
    void resetPassword_Success() {
        when(otpService.isEmailVerified("test@example.com")).thenReturn(true);
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("newPass")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        assertDoesNotThrow(() ->
                authService.resetPassword("test@example.com", "newPass"));
        verify(otpService).consumeVerifiedFlag("test@example.com");
    }

    @Test
    void resetPassword_OtpNotVerified_ThrowsException() {
        when(otpService.isEmailVerified("test@example.com")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.resetPassword("test@example.com", "newPass"));
        assertTrue(ex.getMessage().contains("OTP not verified"));
    }

    // ─── Delete Account ───────────────────────────────────────────────────────

    @Test
    void deleteAccount_Success() {
        when(userRepository.existsById(1)).thenReturn(true);

        assertDoesNotThrow(() -> authService.deleteAccount(1));
        verify(userRepository).deleteById(1);
    }

    @Test
    void deleteAccount_UserNotFound_ThrowsException() {
        when(userRepository.existsById(99)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> authService.deleteAccount(99));
    }
}