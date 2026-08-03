package com.example.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import com.example.mapper.PortfolioRowMapper;

import org.springframework.stereotype.Repository;

import com.example.entity.Portfolio;


@Repository
public class PortfolioRepository {

    // private final JdbcTemplate jdbcTemplate;

    // public PortfolioRepository(JdbcTemplate jdbcTemplate) {
    //     this.jdbcTemplate = jdbcTemplate;
    // }

    private final JdbcTemplate jdbcTemplate;
    private final PortfolioRowMapper portfolioRowMapper;

    public PortfolioRepository(JdbcTemplate jdbcTemplate,
                           PortfolioRowMapper portfolioRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.portfolioRowMapper = portfolioRowMapper;
        ensureTableExists();
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

    // Save or Update Asset
    public Portfolio save(Portfolio portfolio) {

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

        return portfolio;
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