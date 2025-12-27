package com.example.demo.service;

import com.example.demo.entity.Transaction;
import com.example.demo.entity.Account;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    public Transaction createTransaction(Transaction transaction) {
        Account fromAccount = null;
        Account toAccount = null;

        // 1. Находим счёт-отправитель
        if (transaction.getFromAccount() != null && transaction.getFromAccount().getId() != null) {
            fromAccount = accountRepository.findById(transaction.getFromAccount().getId())
                    .orElseThrow(() -> new IllegalArgumentException("From account not found"));
            transaction.setFromAccount(fromAccount);
        }

        // 2. Находим счёт-получатель
        if (transaction.getToAccount() != null && transaction.getToAccount().getId() != null) {
            toAccount = accountRepository.findById(transaction.getToAccount().getId())
                    .orElseThrow(() -> new IllegalArgumentException("To account not found"));
            transaction.setToAccount(toAccount);
        }

        // 3. Если это перевод (TRANSFER) — меняем балансы
        if ("TRANSFER".equals(transaction.getTransactionType()) && fromAccount != null && toAccount != null) {
            // Проверяем достаточно ли денег
            if (fromAccount.getBalance().compareTo(transaction.getAmount()) < 0) {
                throw new IllegalArgumentException("Insufficient funds");
            }

            // Меняем балансы
            fromAccount.setBalance(fromAccount.getBalance().subtract(transaction.getAmount()));
            toAccount.setBalance(toAccount.getBalance().add(transaction.getAmount()));

            // Сохраняем счета
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
        }

        // 4. Сохраняем транзакцию
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    public Transaction updateTransaction(Long id, Transaction transactionDetails) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setAmount(transactionDetails.getAmount());
        transaction.setTransactionType(transactionDetails.getTransactionType());
        transaction.setDescription(transactionDetails.getDescription());

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new RuntimeException("Transaction not found");
        }
        transactionRepository.deleteById(id);
    }

    public List<Transaction> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findByFromAccountAccountNumberOrToAccountAccountNumber(accountNumber, accountNumber);
    }
}