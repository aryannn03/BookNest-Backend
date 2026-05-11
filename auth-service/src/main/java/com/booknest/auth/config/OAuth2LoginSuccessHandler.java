package com.booknest.auth.config;

import com.booknest.auth.entity.User;
import com.booknest.auth.repository.UserRepository;
import com.booknest.auth.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // ─── Detect Provider ──────────────────────────────────────────────────
        String provider = "UNKNOWN";
        if (authentication instanceof OAuth2AuthenticationToken) {
            provider = ((OAuth2AuthenticationToken) authentication)
                    .getAuthorizedClientRegistrationId()
                    .toUpperCase(); // "GITHUB" or "GOOGLE"
        }

        String email = null;
        String fullName = null;

        // ─── GitHub ───────────────────────────────────────────────────────────
        if ("GITHUB".equals(provider)) {
            email = oAuth2User.getAttribute("email");
            String login = oAuth2User.getAttribute("login");

            if (email == null || email.isEmpty()) {
                email = login + "@github.com";
            }

            fullName = oAuth2User.getAttribute("name") != null
                    ? oAuth2User.getAttribute("name")
                    : login;
        }

        // ─── Google ───────────────────────────────────────────────────────────
        else if ("GOOGLE".equals(provider)) {
            email = oAuth2User.getAttribute("email");
            fullName = oAuth2User.getAttribute("name");
        }

        if (email == null || email.isEmpty()) {
            response.sendRedirect(frontendUrl + "/login?error=email_not_found");
            return;
        }

        final String finalEmail = email;
        final String finalName = fullName;
        final String finalProvider = provider;

        // ─── Find or Create User ──────────────────────────────────────────────
        User user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(finalEmail);
            newUser.setFullName(finalName);
            newUser.setProvider(finalProvider);
            newUser.setRole("CUSTOMER");
            return userRepository.save(newUser);
        });

        // ─── Generate Token ───────────────────────────────────────────────────
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole(),
                user.getUserId()
        );

        response.sendRedirect(frontendUrl + "/oauth-callback?token=" + token);
    }
}