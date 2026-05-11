package com.booknest.auth.serviceimpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private OtpServiceImpl otpService;

    // ─── Send OTP ─────────────────────────────────────────────────────────────

    @Test
    void sendOtp_StoresOtpInRedisAndSendsEmail() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        otpService.sendOtp("test@example.com");

        verify(valueOperations).set(
                eq("otp:test@example.com"),
                anyString(),
                eq(Duration.ofMinutes(5))
        );
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // ─── Verify OTP ───────────────────────────────────────────────────────────

    @Test
    void verifyOtp_CorrectOtp_ReturnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:test@example.com")).thenReturn("123456");

        boolean result = otpService.verifyOtp("test@example.com", "123456");

        assertTrue(result);
        verify(redisTemplate).delete("otp:test@example.com");
        verify(valueOperations).set(
                eq("otp_verified:test@example.com"),
                eq("true"),
                eq(Duration.ofMinutes(15))
        );
    }

    @Test
    void verifyOtp_WrongOtp_ReturnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:test@example.com")).thenReturn("123456");

        boolean result = otpService.verifyOtp("test@example.com", "999999");

        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verifyOtp_ExpiredOtp_ReturnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:test@example.com")).thenReturn(null);

        boolean result = otpService.verifyOtp("test@example.com", "123456");

        assertFalse(result);
    }

    // ─── Is Email Verified ────────────────────────────────────────────────────

    @Test
    void isEmailVerified_KeyExists_ReturnsTrue() {
        when(redisTemplate.hasKey("otp_verified:test@example.com"))
                .thenReturn(true);

        assertTrue(otpService.isEmailVerified("test@example.com"));
    }

    @Test
    void isEmailVerified_KeyNotExists_ReturnsFalse() {
        when(redisTemplate.hasKey("otp_verified:test@example.com"))
                .thenReturn(false);

        assertFalse(otpService.isEmailVerified("test@example.com"));
    }

    // ─── Consume Verified Flag ────────────────────────────────────────────────

    @Test
    void consumeVerifiedFlag_DeletesKey() {
        otpService.consumeVerifiedFlag("test@example.com");

        verify(redisTemplate).delete("otp_verified:test@example.com");
    }
}