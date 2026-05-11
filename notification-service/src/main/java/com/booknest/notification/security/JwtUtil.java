package com.booknest.notification.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
	
	private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    // Generate key from secret
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Extract email from token
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    // Extract role from token
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // Extract userId from token
    public int extractUserId(String token) {
        return getClaims(token).get("userId", Integer.class);
    }

    // Validate token
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
        	log.warn("Token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
        	log.warn("Unsupported token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
        	log.warn("Malformed token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
        	log.warn("Illegal argument: {}", e.getMessage());
        }
        return false;
    }

    // Check if token is expired
    public boolean isTokenExpired(String token) {
        try {
            return getClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }


    // Internal — parse and return all claims
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
