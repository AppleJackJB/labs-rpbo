package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ГЕНЕРАЦИЯ ТОКЕНОВ

    public String generateAccessToken(UserDetails userDetails, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sessionId", sessionId);
        claims.put("tokenType", "access");
        return buildToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sessionId", sessionId);
        claims.put("tokenType", "refresh");
        claims.put("tokenId", UUID.randomUUID().toString());
        return buildToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ВАЛИДАЦИЯ ТОКЕНОВ

    public boolean validateAccessToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return "access".equals(claims.getBody().get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return "refresh".equals(claims.getBody().get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    //ИЗВЛЕЧЕНИЕ ДАННЫХ

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public String getTokenIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("tokenId", String.class));
    }

    public String getSessionIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("sessionId", String.class));
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }
}