package com.chatapp.chatservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {
    // In production, move this to environment variables
    private final SecretKey key = Keys.hmacShaKeyFor("your-very-secure-secret-key-that-is-at-least-32-chars".getBytes());

    public String generateToken(Authentication auth) {
        
        long ONE_YEAR = 1000L * 60 * 60 * 24 * 365; 

        return Jwts.builder()
                .setSubject(auth.getName())
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + ONE_YEAR)) // 1 Saal ki validity
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}