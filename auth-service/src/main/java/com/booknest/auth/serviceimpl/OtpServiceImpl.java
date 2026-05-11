package com.booknest.auth.serviceimpl;

import com.booknest.auth.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpServiceImpl implements OtpService {

    private static final String OTP_PREFIX        = "otp:";
    private static final String VERIFIED_PREFIX   = "otp_verified:";

    // OTP expires in 5 minutes
    private static final long OTP_TTL_MINUTES      = 5;
    // Verified flag expires in 15 minutes (window to complete the registration form)
    private static final long VERIFIED_TTL_MINUTES = 15;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JavaMailSender mailSender;

    // ─── Send OTP ─────────────────────────────────────────────────────────────

    @Override
    public void sendOtp(String email) {
        String otp = generateOtp();

        redisTemplate.opsForValue().set(
                OTP_PREFIX + email,
                otp,
                Duration.ofMinutes(OTP_TTL_MINUTES)
        );

        sendOtpEmail(email, otp);
    }

    // ─── Verify OTP ───────────────────────────────────────────────────────────

    @Override
    public boolean verifyOtp(String email, String otp) {
        String otpKey      = OTP_PREFIX + email;
        String verifiedKey = VERIFIED_PREFIX + email;

        Object storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            return false; // expired or never sent
        }

        if (storedOtp.toString().equals(otp)) {
            // Consume the OTP so it cannot be used again
            redisTemplate.delete(otpKey);

            // Plant a verified flag so register() can trust this email
            redisTemplate.opsForValue().set(
                    verifiedKey,
                    "true",
                    Duration.ofMinutes(VERIFIED_TTL_MINUTES)
            );
            return true;
        }

        return false; // wrong OTP
    }

    // ─── Check Verified Flag ──────────────────────────────────────────────────

    @Override
    public boolean isEmailVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(VERIFIED_PREFIX + email));
    }

    // ─── Consume Verified Flag ────────────────────────────────────────────────

    @Override
    public void consumeVerifiedFlag(String email) {
        redisTemplate.delete(VERIFIED_PREFIX + email);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100_000 + random.nextInt(900_000); // always 6 digits
        return String.valueOf(otp);
    }

    private void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("BookNest — Your Email Verification OTP");
        message.setText(
                "Hello,\n\n" +
                "Your One-Time Password (OTP) for BookNest email verification is:\n\n" +
                "  " + otp + "\n\n" +
                "This OTP is valid for " + OTP_TTL_MINUTES + " minutes.\n" +
                "Do not share this code with anyone.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "— The BookNest Team"
        );
        mailSender.send(message);
    }
}