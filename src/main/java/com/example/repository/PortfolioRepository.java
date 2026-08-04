package com.example.repository;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import com.example.mapper.PortfolioRowMapper;

import org.springframework.stereotype.Repository;

import com.example.entity.Portfolio;
import com.example.entity.PortfolioHistory;


@Repository
public class PortfolioRepository {

    // private final JdbcTemplate jdbcTemplate;

    // public PortfolioRepository(JdbcTemplate jdbcTemplate) {
    //     this.jdbcTemplate = jdbcTemplate;
    // }

    private final JdbcTemplate jdbcTemplate;
    private final PortfolioRowMapper portfolioRowMapper;
    private final PortfolioHistoryRepository portfolioHistoryRepository;

    public PortfolioRepository(JdbcTemplate jdbcTemplate,
                           PortfolioRowMapper portfolioRowMapper,
                           PortfolioHistoryRepository portfolioHistoryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.portfolioRowMapper = portfolioRowMapper;
        this.portfolioHistoryRepository = portfolioHistoryRepository;
        ensureTableExists();
        ensurePurchaseDateColumn();
    }

    private void ensureTableExists() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS portfolio (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    symbol VARCHAR(20) NOT NULL,
                    company_name VARCHAR(120) NOT NULL,
                    asset_type VARCHAR(40) NOT NULL,
                    quantity INT NOT NULL,
                    buy_price DECIMAL(19,4) NOT NULL,
                    current_price DECIMAL(19,4) NOT NULL,
                    purchase_date DATE NOT NULL
                )
                """);
    }

    private void ensurePurchaseDateColumn() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                                WHERE LOWER(table_name) = 'portfolio'
                                    AND LOWER(column_name) = 'purchase_date'
                """,
                Integer.class);

        if (count == null || count == 0) {
                        jdbcTemplate.execute("ALTER TABLE portfolio ADD COLUMN purchase_date DATE");
                        jdbcTemplate.execute("UPDATE portfolio SET purchase_date = CURRENT_DATE WHERE purchase_date IS NULL");
        }
    }

    // Get All Assets
    public List<Portfolio> findAll() {

        String sql = """
                SELECT id,
                       symbol,
                       company_name,
                       asset_type,
                       quantity,
                       buy_price,
                       current_price,
                       purchase_date
                FROM portfolio
                """;

        return jdbcTemplate.query(sql, portfolioRowMapper);
    }

    // Get Asset By Id
    public Optional<Portfolio> findById(Integer id) {

        String sql = """
                SELECT id,
                       symbol,
                       company_name,
                       asset_type,
                       quantity,
                       buy_price,
                       current_price,
                       purchase_date
                FROM portfolio
                WHERE id = ?
                """;

        List<Portfolio> portfolios = jdbcTemplate.query(sql, portfolioRowMapper, id);

        return portfolios.stream().findFirst();
    }

    public Optional<Portfolio> findBySymbolAndPurchaseDate(String symbol, LocalDate purchaseDate) {

        String sql = """
                SELECT id,
                       symbol,
                       company_name,
                       asset_type,
                       quantity,
                       buy_price,
                       current_price,
                       purchase_date
                FROM portfolio
                WHERE UPPER(symbol) = UPPER(?)
                                    AND purchase_date = ?
                """;

                                List<Portfolio> portfolios = jdbcTemplate.query(sql, portfolioRowMapper, symbol, purchaseDate);

        return portfolios.stream().findFirst();
    }

    // Save or Update Asset
    public Portfolio save(Portfolio portfolio) {

        if (portfolio.getId() == null && portfolio.getSymbol() != null && portfolio.getPurchaseDate() != null) {
            findBySymbolAndPurchaseDate(portfolio.getSymbol(), portfolio.getPurchaseDate())
                    .ifPresent(existing -> mergeWithExistingPortfolio(portfolio, existing));
        }

        if (portfolio.getId() == null) {

            String insertSql = """
                    INSERT INTO portfolio
                    (symbol, company_name, asset_type, quantity, buy_price, current_price, purchase_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

            jdbcTemplate.update(
                    insertSql,
                    portfolio.getSymbol(),
                    portfolio.getCompanyName(),
                    portfolio.getAssetType(),
                    portfolio.getQuantity(),
                    portfolio.getBuyPrice(),
                    portfolio.getCurrentPrice(),
                    portfolio.getPurchaseDate());

            Integer generatedId = jdbcTemplate.queryForObject(
                    "SELECT LAST_INSERT_ID()",
                    Integer.class);

            portfolio.setId(generatedId);

        } else {

            String updateSql = """
                    UPDATE portfolio
                    SET symbol = ?,
                        company_name = ?,
                        asset_type = ?,
                        quantity = ?,
                        buy_price = ?,
                        current_price = ?,
                        purchase_date = ?
                    WHERE id = ?
                    """;

            jdbcTemplate.update(
                    updateSql,
                    portfolio.getSymbol(),
                    portfolio.getCompanyName(),
                    portfolio.getAssetType(),
                    portfolio.getQuantity(),
                    portfolio.getBuyPrice(),
                    portfolio.getCurrentPrice(),
                    portfolio.getPurchaseDate(),
                    portfolio.getId());
        }

        saveHistorySnapshot(portfolio);

        return portfolio;
    }

    private void saveHistorySnapshot(Portfolio portfolio) {
        PortfolioHistory history = new PortfolioHistory();
        history.setPortfolioId(portfolio.getId());
        history.setSymbol(portfolio.getSymbol());
        history.setRecordedDate(LocalDate.now());
        history.setBuyPrice(portfolio.getBuyPrice());
        history.setCurrentPrice(portfolio.getCurrentPrice());
        history.setQuantity(portfolio.getQuantity());

        BigDecimal profit = BigDecimal.valueOf(
                (portfolio.getCurrentPrice() - portfolio.getBuyPrice()) * portfolio.getQuantity())
                .setScale(4, RoundingMode.HALF_UP);
        history.setProfit(profit);

        portfolioHistoryRepository.save(history);
    }

    private void mergeWithExistingPortfolio(Portfolio incoming, Portfolio existing) {
        incoming.setId(existing.getId());
        incoming.setQuantity(existing.getQuantity() + incoming.getQuantity());

        BigDecimal existingCost = BigDecimal.valueOf(existing.getBuyPrice())
                .multiply(BigDecimal.valueOf(existing.getQuantity()));
        BigDecimal incomingCost = BigDecimal.valueOf(incoming.getBuyPrice())
                .multiply(BigDecimal.valueOf(incoming.getQuantity() - existing.getQuantity()));
        BigDecimal totalQuantity = BigDecimal.valueOf(incoming.getQuantity());

        incoming.setBuyPrice(existingCost.add(incomingCost)
                .divide(totalQuantity, 4, RoundingMode.HALF_UP)
                .doubleValue());

        if (incoming.getCompanyName() == null || incoming.getCompanyName().isBlank()) {
            incoming.setCompanyName(existing.getCompanyName());
        }

        if (incoming.getAssetType() == null || incoming.getAssetType().isBlank()) {
            incoming.setAssetType(existing.getAssetType());
        }
    }

    // Delete Asset
    public void deleteById(Integer id) {

        String sql = """
                DELETE FROM portfolio
                WHERE id = ?
                """;

        jdbcTemplate.update(sql, id);
    }

    // Check if Asset Exists
    public boolean existsById(Integer id) {

        String sql = """
                SELECT COUNT(*)
                FROM portfolio
                WHERE id = ?
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);

        return count != null && count > 0;
    }
}