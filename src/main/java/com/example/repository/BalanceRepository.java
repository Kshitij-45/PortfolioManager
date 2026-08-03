package com.example.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.entity.Balance;
import com.example.mapper.BalanceRowMapper;

@Repository
public class BalanceRepository {

    private static final int ACCOUNT_ID = 1;
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("10000.00");

    private final JdbcTemplate jdbcTemplate;
    private final BalanceRowMapper balanceRowMapper;

    public BalanceRepository(JdbcTemplate jdbcTemplate,
                             BalanceRowMapper balanceRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.balanceRowMapper = balanceRowMapper;
        ensureTableAndSeed();
    }

    // Get Balance
    public Balance getBalance() {

        String sql = """
                SELECT id,
                       available_balance
                FROM account_balance
                WHERE id = ?
                """;

        List<Balance> balances = jdbcTemplate.query(
                sql,
                balanceRowMapper,
                ACCOUNT_ID);

        return balances.get(0);
    }

    // Update Balance
    public void updateBalance(Balance balance) {

        String sql = """
                UPDATE account_balance
                SET available_balance = ?
                WHERE id = ?
                """;

        jdbcTemplate.update(
                sql,
                balance.getAvailableBalance(),
                balance.getId());
    }

    // Create table and insert initial balance
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
                    "INSERT INTO account_balance(id, available_balance) VALUES(?, ?)",
                    ACCOUNT_ID,
                    DEFAULT_BALANCE);
        }
    }

    
}