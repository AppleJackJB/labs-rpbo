package com.example.demo.entity;

import com.example.demo.enums.SessionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String refreshTokenId; // UUID для поиска в БД

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address")
    private String ipAddress;

    // Конструкторы
    public UserSession() {}

    public UserSession(User user, String refreshTokenId, LocalDateTime expiresAt) {
        this.user = user;
        this.refreshTokenId = refreshTokenId;
        this.expiresAt = expiresAt;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getRefreshTokenId() { return refreshTokenId; }
    public void setRefreshTokenId(String refreshTokenId) { this.refreshTokenId = refreshTokenId; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    // Вспомогательный метод
    public boolean isActive() {
        return status == SessionStatus.ACTIVE &&
                LocalDateTime.now().isBefore(expiresAt);
    }

    // Метод для пометки как заменённой
    public void markAsReplaced() {
        this.status = SessionStatus.REPLACED;
    }
}