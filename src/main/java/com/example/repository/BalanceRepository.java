package com.example.repository;

import java.math.BigDecimal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BalanceRepository {

    private static final int ACCOUNT_ID = 1;
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("10000.00");

    private final JdbcTemplate jdbcTemplate;

    public BalanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureTableAndSeed();
    }

    public BigDecimal getAvailableBalance() {
        String sql = "SELECT available_balance FROM account_balance WHERE id = ?";
        BigDecimal balance = jdbcTemplate.queryForObject(sql, BigDecimal.class, ACCOUNT_ID);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    public void setAvailableBalance(BigDecimal balance) {
        String sql = "UPDATE account_balance SET available_balance = ? WHERE id = ?";
        jdbcTemplate.update(sql, balance, ACCOUNT_ID);
    }

    private void ensureTableAndSeed() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS account_balance (
                    id INT PRIMARY KEY,
                    available_balance DECIMAL(19,2) NOT NULL
                )
                """);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_balance WHERE id = ?",
                Integer.class,
                ACCOUNT_ID);

        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO account_balance (id, available_balance) VALUES (?, ?)",
                    ACCOUNT_ID,
                    DEFAULT_BALANCE);
        }
    }
}
