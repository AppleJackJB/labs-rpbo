package com.example.demo.controller;

import com.example.demo.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/loans")

public class LoanController {

    @Autowired
    private LoanService loanService;

    @PostMapping("/take")
    public ResponseEntity<?> takeLoan(@RequestBody Map<String, Object> request) {

        System.out.println("=== DEBUG: Получен запрос на получение кредита ===");
        System.out.println("Входящий JSON: " + request);

        try {
            // Извлекаем данные из JSON
            Long accountId = Long.valueOf(request.get("accountId").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            Integer termMonths = Integer.valueOf(request.get("termMonths").toString());
            Double interestRate = Double.valueOf(request.get("interestRate").toString());

            System.out.println("Преобразовано: accountId=" + accountId +
                    ", amount=" + amount +
                    ", termMonths=" + termMonths +
                    ", interestRate=" + interestRate);

            // Вызываем сервис
            return ResponseEntity.ok(loanService.takeLoan(accountId, amount, termMonths, interestRate));

        } catch (NullPointerException e) {
            System.err.println("ОШИБКА: Отсутствует обязательное поле в JSON");
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required field in JSON",
                    "details", "Check if all fields are present: accountId, amount, termMonths, interestRate"
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
                    "error", "Failed to process loan request",
                    "details", e.getMessage()
            ));
        }
    }

    @PostMapping("/{loanId}/repay")
    public ResponseEntity<?> repayLoan(
            @PathVariable Long loanId,
            @RequestBody Map<String, Object> request) {

        System.out.println("=== DEBUG: Получен запрос на погашение кредита ===");
        System.out.println("loanId: " + loanId);
        System.out.println("Входящий JSON: " + request);

        try {
            Long accountId = Long.valueOf(request.get("accountId").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            System.out.println("Преобразовано: accountId=" + accountId + ", amount=" + amount);

            return ResponseEntity.ok(loanService.repayLoan(loanId, accountId, amount));

        } catch (NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required field in JSON",
                    "details", "Check if all fields are present: accountId, amount"
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid number format",
                    "details", "Make sure all numeric fields contain valid numbers"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process repayment",
                    "details", e.getMessage()
            ));
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getCustomerLoans(@PathVariable Long customerId) {
        return ResponseEntity.ok(loanService.getCustomerLoans(customerId));
    }
}