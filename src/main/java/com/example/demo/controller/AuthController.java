package com.example.demo.controller;

import com.example.demo.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    // Логин пользователя
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        try {
            // 1. Аутентификация пользователя
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 2. Получение информации о клиенте
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(request);

            // 3. Генерация токенов
            Map<String, String> tokens = tokenService.createTokenPair(
                    loginRequest.getUsername(),
                    userAgent,
                    ipAddress
            );

            // 4. Возврат токенов
            return ResponseEntity.ok()
                    .header("Authorization", "Bearer " + tokens.get("accessToken"))
                    .body(new LoginResponse(
                            tokens.get("accessToken"),
                            tokens.get("refreshToken"),
                            "Login successful"
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    // Обновление токенов
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestBody RefreshTokenRequest refreshRequest,
            HttpServletRequest request) {

        try {
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(request);

            Map<String, String> tokens = tokenService.refreshTokenPair(
                    refreshRequest.getRefreshToken(),
                    userAgent,
                    ipAddress
            );

            return ResponseEntity.ok()
                    .header("Authorization", "Bearer " + tokens.get("accessToken"))
                    .body(new LoginResponse(
                            tokens.get("accessToken"),
                            tokens.get("refreshToken"),
                            "Token refreshed successfully"
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        }
    }

    // Логаут
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest logoutRequest) {
        try {
            tokenService.logout(logoutRequest.getRefreshToken());
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Logout failed"));
        }
    }

    // Получение IP адреса клиента
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // DTO классы для запросов/ответов
    public static class LoginRequest {
        private String username;
        private String password;

        // геттеры и сеттеры
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class LogoutRequest {
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String message;

        public LoginResponse(String accessToken, String refreshToken, String message) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.message = message;
        }

        // геттеры
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public String getMessage() { return message; }
    }
}