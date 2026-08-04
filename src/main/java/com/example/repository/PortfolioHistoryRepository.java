package com.example.repository;

import com.example.entity.PortfolioHistory;
import com.example.mapper.PortfolioHistoryRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class PortfolioHistoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PortfolioHistoryRowMapper rowMapper;

    public PortfolioHistoryRepository(JdbcTemplate jdbcTemplate,
                                      PortfolioHistoryRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
        ensureTableExists();
    }

    private void ensureTableExists() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS portfolio_history (
                    id             INT PRIMARY KEY AUTO_INCREMENT,
                    portfolio_id   INT NOT NULL,
                    symbol         VARCHAR(20) NOT NULL,
                    recorded_date  DATE NOT NULL,
                    buy_price      DECIMAL(19,4) NOT NULL,
                    current_price  DECIMAL(19,4) NOT NULL,
                    quantity       INT NOT NULL,
                    profit         DECIMAL(19,4) NOT NULL
                )
                """);
    }

    /** Insert one history snapshot row. */
    public void save(PortfolioHistory history) {
        String sql = """
                INSERT INTO portfolio_history
                    (portfolio_id, symbol, recorded_date, buy_price, current_price, quantity, profit)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql,
                history.getPortfolioId(),
                history.getSymbol(),
                history.getRecordedDate(),
                history.getBuyPrice(),
                history.getCurrentPrice(),
                history.getQuantity(),
                history.getProfit());
    }

    /** All snapshots for a specific portfolio entry (single holding's full history). */
    public List<PortfolioHistory> findByPortfolioId(Integer portfolioId) {
        String sql = """
                SELECT * FROM portfolio_history
                WHERE portfolio_id = ?
                ORDER BY recorded_date ASC
                """;
        return jdbcTemplate.query(sql, rowMapper, portfolioId);
    }

    /** Total profit per day across ALL holdings — used for the line graph. */
    public List<Object[]> findDailyProfitSummary() {
        String sql = """
                SELECT recorded_date, SUM(profit) AS total_profit
                FROM portfolio_history
                GROUP BY recorded_date
                ORDER BY recorded_date ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getDate("recorded_date").toLocalDate(),
                rs.getBigDecimal("total_profit")
        });
    }

    /** Delete all history rows for a given portfolio (on portfolio delete). */
    public void deleteByPortfolioId(Integer portfolioId) {
        jdbcTemplate.update("DELETE FROM portfolio_history WHERE portfolio_id = ?", portfolioId);
    }
}
