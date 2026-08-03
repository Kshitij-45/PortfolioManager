package com.example.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.Balance;
import com.example.exception.InvalidBalanceAmountException;
import com.example.exception.InsufficientBalanceException;
import com.example.repository.BalanceRepository;

@Service
@Transactional
public class BalanceService {

    private final BalanceRepository balanceRepository;

    public BalanceService(BalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
    }

    // Get Current Balance
    public Balance getBalance() {
        return balanceRepository.getBalance();
    }

    // Add Money
    public Balance addBalance(BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBalanceAmountException("Amount must be greater than 0");
        }

        Balance balance = balanceRepository.getBalance();

        BigDecimal updatedBalance =
                balance.getAvailableBalance().add(amount);

        balance.setAvailableBalance(updatedBalance);

        balanceRepository.updateBalance(balance);

        return balance;
    }

    // Deduct Money
    public void deductBalance(BigDecimal amount) {

        Balance balance = balanceRepository.getBalance();

        BigDecimal currentBalance =
                balance.getAvailableBalance();

        if (currentBalance.compareTo(amount) < 0) {

            throw new InsufficientBalanceException(
                    "Insufficient Balance. Available Balance : ₹"
                            + currentBalance
                            + ", Required Amount : ₹"
                            + amount);
        }

        BigDecimal updatedBalance =
                currentBalance.subtract(amount);

        balance.setAvailableBalance(updatedBalance);

        balanceRepository.updateBalance(balance);
    }
}