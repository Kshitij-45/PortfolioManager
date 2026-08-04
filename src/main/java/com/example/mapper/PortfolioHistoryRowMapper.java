package com.example.mapper;

import com.example.entity.PortfolioHistory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class PortfolioHistoryRowMapper implements RowMapper<PortfolioHistory> {

    @Override
    public PortfolioHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
        PortfolioHistory h = new PortfolioHistory();
        h.setId(rs.getInt("id"));
        h.setPortfolioId(rs.getInt("portfolio_id"));
        h.setSymbol(rs.getString("symbol"));
        h.setBuyPrice(rs.getDouble("buy_price"));
        h.setCurrentPrice(rs.getDouble("current_price"));
        h.setQuantity(rs.getInt("quantity"));
        h.setProfit(rs.getBigDecimal("profit"));

        java.sql.Date sqlDate = rs.getDate("recorded_date");
        if (sqlDate != null) {
            h.setRecordedDate(sqlDate.toLocalDate());
        }
        return h;
    }
}
