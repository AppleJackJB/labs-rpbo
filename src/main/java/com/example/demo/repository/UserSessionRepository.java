package com.example.demo.repository;

import com.example.demo.entity.UserSession;
import com.example.demo.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    // Найти сессию по refreshTokenId
    Optional<UserSession> findByRefreshTokenId(String refreshTokenId);

    // Найти все активные сессии пользователя
    List<UserSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    // Найти все истёкшие сессии
    List<UserSession> findByStatusAndExpiresAtBefore(SessionStatus status, LocalDateTime dateTime);

    // Обновить статус сессии
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.status = :status WHERE s.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") SessionStatus status);

    // Отозвать все сессии пользователя
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.status = 'REVOKED' WHERE s.user.id = :userId AND s.status = 'ACTIVE'")
    void revokeAllUserSessions(@Param("userId") Long userId);
}