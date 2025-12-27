package com.example.demo.controller;

import com.example.demo.service.ExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.example.demo.entity.ExchangeRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/exchange")
@Transactional
public class ExchangeController {

    @Autowired
    private ExchangeService exchangeService;

    @PostMapping("/rate")
    public ResponseEntity<?> setExchangeRate(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency,
            @RequestParam BigDecimal rate) {
        return ResponseEntity.ok(exchangeService.setExchangeRate(fromCurrency, toCurrency, rate));
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertCurrency(
            @RequestParam Long fromAccountId,
            @RequestParam Long toAccountId,
            @RequestParam BigDecimal amount,
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency) {
        try {
            return ResponseEntity.ok(exchangeService.convertCurrency(
                    fromAccountId, toAccountId, amount, fromCurrency, toCurrency));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rate/{fromCurrency}/{toCurrency}")
    public ResponseEntity<?> getExchangeRate(
            @PathVariable String fromCurrency,
            @PathVariable String toCurrency) {
        Optional<ExchangeRate> exchangeRate = exchangeService.getExchangeRate(fromCurrency, toCurrency);
        return exchangeRate.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rates")
    public ResponseEntity<?> getAllExchangeRates() {
        return ResponseEntity.ok(exchangeService.getAllExchangeRates());
    }

    // ========== ДОБАВЬ ЭТОТ МЕТОД ДЛЯ TLS ДЕМОНСТРАЦИИ ==========
    @GetMapping("/tls-info")
    public Map<String, Object> tlsInfo() {
        return Map.of(
                "status", "TLS_ACTIVE",
                "studentId", "1БИБ23253",
                "certificateChain", "StudentRootCA1 → StudentIntermediateCA2 → StudentServerCert3",
                "timestamp", LocalDateTime.now().toString(),
                "message", "HTTPS/TLS is working on port 8443",
                "endpoints", Map.of(
                        "tlsTest", "GET /exchange/tls-info",
                        "login", "POST /api/auth/login",
                        "accounts", "GET /accounts (requires JWT)"
                )
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "tls", "ENABLED",
                "student", "1БИБ23253",
                "service", "Exchange Service",
                "timestamp", LocalDateTime.now().toString()
        );
    }
}