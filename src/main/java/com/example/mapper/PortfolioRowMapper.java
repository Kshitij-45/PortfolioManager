package com.example.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.example.entity.Portfolio;

@Component
public class PortfolioRowMapper implements RowMapper<Portfolio> {

    @Override
    public Portfolio mapRow(ResultSet rs, int rowNum) throws SQLException {

        Portfolio portfolio = new Portfolio();

        portfolio.setId(rs.getInt("id"));
        portfolio.setSymbol(rs.getString("symbol"));
        portfolio.setCompanyName(rs.getString("company_name"));
        portfolio.setAssetType(rs.getString("asset_type"));
        portfolio.setQuantity(rs.getInt("quantity"));
        portfolio.setBuyPrice(rs.getDouble("buy_price"));
        portfolio.setCurrentPrice(rs.getDouble("current_price"));

        java.sql.Date sqlDate = rs.getDate("purchase_date");
        if (sqlDate != null) {
            portfolio.setPurchaseDate(sqlDate.toLocalDate());
        }

        return portfolio;
    }
}