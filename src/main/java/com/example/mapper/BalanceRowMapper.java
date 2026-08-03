package com.example.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.example.entity.Balance;

@Component
public class BalanceRowMapper implements RowMapper<Balance> {

    @Override
    public Balance mapRow(ResultSet rs, int rowNum) throws SQLException {

        Balance balance = new Balance(
                rs.getInt("id"),
                rs.getBigDecimal("available_balance")
        );

        return balance;
    }
}