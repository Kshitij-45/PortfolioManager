package com.example.service;

import com.example.dto.PortfolioDTO;
import com.example.entity.Portfolio;
import com.example.repository.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public Portfolio createPortfolio(PortfolioDTO portfolioDTO) {
        if (portfolioDTO == null) {
            throw new IllegalArgumentException("Portfolio cannot be null");
        }
        Portfolio portfolio = new Portfolio();
        portfolio.setSymbol(portfolioDTO.getSymbol());
        portfolio.setCompanyName(portfolioDTO.getCompanyName());
        portfolio.setAssetType(portfolioDTO.getAssetType());
        portfolio.setQuantity(portfolioDTO.getQuantity());
        portfolio.setBuyPrice(portfolioDTO.getBuyPrice());
        portfolio.setCurrentPrice(portfolioDTO.getCurrentPrice());
        return portfolioRepository.save(portfolio);
    }

    @Transactional(readOnly = true)
    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Portfolio getPortfolioById(Integer id) {
        validateId(id);
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found with id: " + id));
    }

    public Portfolio updatePortfolio(Integer id, PortfolioDTO portfolioDTO) {
        validateId(id);
        if (portfolioDTO == null) {
            throw new IllegalArgumentException("Portfolio cannot be null");
        }

        if (!portfolioRepository.existsById(id)) {
            throw new IllegalArgumentException("Portfolio not found with id: " + id);
        }

        Portfolio portfolio = new Portfolio();
        portfolio.setId(id);
        portfolio.setSymbol(portfolioDTO.getSymbol());
        portfolio.setCompanyName(portfolioDTO.getCompanyName());
        portfolio.setAssetType(portfolioDTO.getAssetType());
        portfolio.setQuantity(portfolioDTO.getQuantity());
        portfolio.setBuyPrice(portfolioDTO.getBuyPrice());
        portfolio.setCurrentPrice(portfolioDTO.getCurrentPrice());
        return portfolioRepository.save(portfolio);
    }

    public void deletePortfolio(Integer id) {
        validateId(id);
        if (!portfolioRepository.existsById(id)) {
            throw new IllegalArgumentException("Portfolio not found with id: " + id);
        }
        portfolioRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Integer id) {
        validateId(id);
        return portfolioRepository.existsById(id);
    }

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }
    }
}