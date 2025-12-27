package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserSession;
import com.example.demo.enums.SessionStatus;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSessionRepository;
import com.example.demo.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    // Создать новую пару токенов
    public Map<String, String> createTokenPair(String username, String userAgent, String ipAddress) {
        try {
            logger.info("=== НАЧАЛО createTokenPair для пользователя: {} ===", username);

            // 1. Получаем UserDetails ДО создания сессии
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            logger.info("UserDetails получен: {}", userDetails.getUsername());

            // 2. Находим User entity
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        logger.error("Пользователь не найден: {}", username);
                        return new RuntimeException("User not found: " + username);
                    });
            logger.info("Найден пользователь: ID={}, username={}", user.getId(), user.getUsername());

            // 3. Сначала генерируем refresh токен, чтобы получить tokenId
            String tempSessionId = UUID.randomUUID().toString();
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails, tempSessionId);
            String tokenId = jwtTokenProvider.getTokenIdFromToken(refreshToken);
            logger.info("TokenId из refresh токена: {}", tokenId);

            // 4. Теперь создаём сессию С УЖЕ ИЗВЕСТНЫМ tokenId
            UserSession session = new UserSession();
            session.setUser(user);
            session.setRefreshTokenId(tokenId); // ← УЖЕ ЗНАЕМ!
            session.setStatus(SessionStatus.ACTIVE);
            session.setExpiresAt(LocalDateTime.now().plusDays(7));
            session.setUserAgent(userAgent);
            session.setIpAddress(ipAddress);

            UserSession savedSession = userSessionRepository.save(session);
            logger.info("Сессия создана: ID={}, refreshTokenId={}", savedSession.getId(), tokenId);

            // 5. Генерируем access токен с реальным ID сессии
            String accessToken = jwtTokenProvider.generateAccessToken(userDetails, savedSession.getId().toString());
            logger.info("Access токен сгенерирован");

            // 6. Возвращаем результат
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);
            tokens.put("sessionId", savedSession.getId().toString());
            tokens.put("message", "Login successful");

            logger.info("=== УСПЕХ createTokenPair для пользователя: {} ===", username);
            return tokens;

        } catch (Exception e) {
            logger.error("=== ОШИБКА в createTokenPair ===", e);
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    // Обновить пару токенов
    @Transactional
    public Map<String, String> refreshTokenPair(String oldRefreshToken, String userAgent, String ipAddress) {
        try {
            logger.info("=== НАЧАЛО refreshTokenPair ===");

            // 1. Валидируем refresh токен
            if (!jwtTokenProvider.validateRefreshToken(oldRefreshToken)) {
                logger.error("Refresh токен невалиден");
                throw new RuntimeException("Invalid refresh token");
            }

            // 2. Извлекаем данные из токена
            String tokenId = jwtTokenProvider.getTokenIdFromToken(oldRefreshToken);
            String username = jwtTokenProvider.getUsernameFromToken(oldRefreshToken);

            logger.info("Данные из токена: tokenId={}, username={}", tokenId, username);

            // 3. Находим сессию в БД
            UserSession oldSession = userSessionRepository.findByRefreshTokenId(tokenId)
                    .orElseThrow(() -> {
                        logger.error("Сессия не найдена для tokenId: {}", tokenId);
                        return new RuntimeException("Session not found");
                    });

            logger.info("Найдена старая сессия: ID={}, статус={}", oldSession.getId(), oldSession.getStatus());

            // 4. Проверяем, что сессия активна
            if (!oldSession.isActive()) {
                logger.error("Сессия неактивна. Статус: {}", oldSession.getStatus());
                throw new RuntimeException("Session is not active");
            }

            // 5. Помечаем старую сессию как заменённую
            oldSession.setStatus(SessionStatus.REPLACED);
            userSessionRepository.save(oldSession);
            logger.info("Старая сессия {} помечена как REPLACED", oldSession.getId());

            // 6. Создаём новую пару токенов
            logger.info("Создаю новую пару токенов для пользователя: {}", username);
            return createTokenPair(username, userAgent, ipAddress);

        } catch (Exception e) {
            logger.error("=== ОШИБКА в refreshTokenPair ===", e);
            throw new RuntimeException("Refresh failed: " + e.getMessage(), e);
        }
    }

    // Выйти из системы (отозвать сессию)
    @Transactional
    public void logout(String refreshToken) {
        try {
            logger.info("=== НАЧАЛО logout ===");

            // 1. Валидируем refresh токен
            if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
                logger.warn("Невалидный refresh токен при logout");
                return;
            }

            // 2. Извлекаем tokenId из токена
            String tokenId = jwtTokenProvider.getTokenIdFromToken(refreshToken);
            logger.info("TokenId для logout: {}", tokenId);

            // 3. Находим сессию и помечаем как отозванную
            userSessionRepository.findByRefreshTokenId(tokenId)
                    .ifPresentOrElse(
                            session -> {
                                session.setStatus(SessionStatus.REVOKED);
                                userSessionRepository.save(session);
                                logger.info("Сессия {} отозвана для пользователя {}",
                                        session.getId(), session.getUser().getUsername());
                            },
                            () -> logger.warn("Сессия не найдена для tokenId: {}", tokenId)
                    );

        } catch (Exception e) {
            logger.error("=== ОШИБКА в logout ===", e);
            throw new RuntimeException("Logout failed: " + e.getMessage(), e);
        }
    }
}