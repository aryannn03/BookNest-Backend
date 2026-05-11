package com.booknest.auth.service;

public interface OtpService {


    void sendOtp(String email);

    boolean verifyOtp(String email, String otp);

    boolean isEmailVerified(String email);

    void consumeVerifiedFlag(String email);
}