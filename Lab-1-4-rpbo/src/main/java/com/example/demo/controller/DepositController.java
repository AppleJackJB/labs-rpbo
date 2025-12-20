package com.example.demo.controller;

import com.example.demo.service.DepositService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/deposits")
public class DepositController {

    @Autowired
    private DepositService depositService;

    @PostMapping("/open")
    public ResponseEntity<?> openDeposit(@RequestBody Map<String, Object> request) {

        System.out.println("=== DEBUG: Получен запрос на открытие депозита ===");
        System.out.println("Входящий JSON: " + request);

        try {
            // 1. Извлекаем и преобразуем данные из JSON
            Long accountId = Long.valueOf(request.get("accountId").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            Double interestRate = Double.valueOf(request.get("interestRate").toString());
            Integer termMonths = Integer.valueOf(request.get("termMonths").toString());

            // 2. Проверяем, что все поля есть
            System.out.println("Преобразовано: accountId=" + accountId +
                    ", amount=" + amount +
                    ", interestRate=" + interestRate +
                    ", termMonths=" + termMonths);

            // 3. Вызываем сервис
            return ResponseEntity.ok(
                    depositService.openDeposit(accountId, amount, interestRate, termMonths)
            );

        } catch (NullPointerException e) {
            System.err.println("ОШИБКА: Отсутствует обязательное поле в JSON");
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required field in JSON",
                    "details", "Check if all fields are present: accountId, amount, interestRate, termMonths"
            ));
        } catch (NumberFormatException e) {
            System.err.println("ОШИБКА: Неверный формат числа");
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid number format",
                    "details", "Make sure all numeric fields contain valid numbers"
            ));
        } catch (Exception e) {
            System.err.println("ОШИБКА: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process request",
                    "details", e.getMessage()
            ));
        }
    }

    @PostMapping("/{depositId}/close")
    public ResponseEntity<?> closeDeposit(@PathVariable Long depositId) {
        try {
            return ResponseEntity.ok(depositService.closeDeposit(depositId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getCustomerDeposits(@PathVariable Long customerId) {
        return ResponseEntity.ok(depositService.getCustomerDeposits(customerId));
    }
}