package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Portfolio;

public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {
    
}
