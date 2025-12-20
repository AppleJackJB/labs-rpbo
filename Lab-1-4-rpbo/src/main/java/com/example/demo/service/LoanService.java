package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Map<String, Object> takeLoan(Long accountId, BigDecimal amount,
                                        Integer termMonths, Double interestRate) {

        // 1. ПРОВЕРКА ВХОДНЫХ ДАННЫХ
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan amount must be positive");
        }
        if (termMonths <= 0) {
            throw new IllegalArgumentException("Loan term must be positive");
        }
        if (interestRate <= 0) {
            throw new IllegalArgumentException("Interest rate must be positive");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // 2. ЗАЧИСЛЯЕМ ДЕНЬГИ НА СЧЕТ
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // 3. СОЗДАЕМ КРЕДИТ
        Loan loan = new Loan(account, amount, termMonths, interestRate);
        loan.setStatus("ACTIVE");
        loanRepository.save(loan);

        // 4. СОЗДАЕМ ТРАНЗАКЦИЮ
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType("LOAN_DISBURSEMENT");
        transaction.setDescription("Loan #" + loan.getId() + " for " + termMonths + " months at " + interestRate + "%");
        transaction.setToAccount(account);
        transactionRepository.save(transaction);

        // 5. ВОЗВРАЩАЕМ ОТВЕТ
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Loan issued successfully");
        response.put("loanId", loan.getId());
        response.put("monthlyPayment", calculateMonthlyPayment(amount, termMonths, interestRate));
        response.put("totalAmount", amount);
        response.put("termMonths", termMonths);

        return response;
    }

    public Map<String, Object> repayLoan(Long loanId, Long accountId, BigDecimal amount) {
        // 1. ПОЛУЧАЕМ КРЕДИТ И СЧЕТ
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // 2. ПРОВЕРКИ
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        if ("PAID".equals(loan.getStatus())) {
            throw new IllegalArgumentException("Loan is already paid");
        }

        BigDecimal remaining = loan.getRemainingAmount();

        // 3. ЕСЛИ ПЛАТЕЖ БОЛЬШЕ ОСТАТКА - ОШИБКА
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount (" + amount + ") exceeds remaining debt (" + remaining + "). " +
                            "Maximum payment allowed: " + remaining);
        }

        // 4. СПИСЫВАЕМ ДЕНЬГИ
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        // 5. ОБНОВЛЯЕМ ОСТАТОК КРЕДИТА
        BigDecimal newRemaining = remaining.subtract(amount);
        loan.setRemainingAmount(newRemaining);

        // 6. ЕСЛИ ПОЛНОСТЬЮ ПОГАШЕН
        if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus("PAID");
            System.out.println("INFO: Loan #" + loanId + " fully paid!");
        }

        loanRepository.save(loan);

        // 7. СОЗДАЕМ ТРАНЗАКЦИЮ
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType("LOAN_REPAYMENT");
        transaction.setDescription("Loan #" + loanId + " repayment");
        transaction.setFromAccount(account);
        transactionRepository.save(transaction);

        // 8. ВОЗВРАЩАЕМ ОТВЕТ
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Loan repayment successful");
        response.put("loanId", loanId);
        response.put("amountPaid", amount);
        response.put("remainingAmount", newRemaining);
        response.put("status", loan.getStatus());

        return response;
    }

    public Object getCustomerLoans(Long customerId) {
        return loanRepository.findByAccount_Customer_Id(customerId);
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal amount, Integer termMonths, Double interestRate) {
        double monthlyRate = interestRate / 100 / 12;
        double payment = amount.doubleValue() * (monthlyRate * Math.pow(1 + monthlyRate, termMonths))
                / (Math.pow(1 + monthlyRate, termMonths) - 1);
        return BigDecimal.valueOf(payment);
    }
}