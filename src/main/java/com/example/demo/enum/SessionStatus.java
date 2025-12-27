package com.example.demo.enums;

public enum SessionStatus {
    ACTIVE,      // Активная сессия
    EXPIRED,     // Истекла по времени
    REVOKED,     // Отозвана (логаут)
    REPLACED     // Заменена новой (при refresh)
}