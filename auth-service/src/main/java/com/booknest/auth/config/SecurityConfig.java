package com.booknest.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // Disable CSRF (not needed for REST APIs)
            .csrf(csrf -> csrf.disable())

            // Session management - stateless because we use JWT
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL authorization rules
            .authorizeHttpRequests(auth -> auth

                // Public endpoints - no token needed
                .requestMatchers("/auth/register").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/validate").permitAll()

                // OAuth2 login endpoints - public
                .requestMatchers("/login/oauth2/**").permitAll()
                .requestMatchers("/oauth2/**").permitAll()

                // Admin only endpoints
                .requestMatchers("/auth/users/**").hasRole("ADMIN")

                // All other endpoints need authentication
                .anyRequest().authenticated()
            )

            // Google OAuth2 login
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/auth/oauth2/success", true)
                .failureUrl("/auth/oauth2/failure")
            );

        return http.build();
    }
}