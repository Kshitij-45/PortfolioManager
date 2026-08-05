package com.example.service;

import com.example.exception.InvalidIdException;
import com.example.exception.InvalidPortfolioException;
import com.example.exception.PortfolioNotFoundException;
import com.example.exception.UnsupportedAssetTypeException;
import com.example.dto.PortfolioDTO;
import com.example.entity.Portfolio;
import com.example.repository.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.List;

@Service
@Transactional
public class PortfolioService {

    private static final Set<String> SUPPORTED_ASSET_TYPES = Set.of(
            "stock", "bond", "crypto", "mutual fund", "cash", "etf", "other");

    private final PortfolioRepository portfolioRepository;
    private final BalanceService balanceService;
    private final PortfolioHistoryService portfolioHistoryService;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            BalanceService balanceService,
                            PortfolioHistoryService portfolioHistoryService) {
        this.portfolioRepository = portfolioRepository;
        this.balanceService = balanceService;
        this.portfolioHistoryService = portfolioHistoryService;
    }

    public Portfolio createPortfolio(PortfolioDTO portfolioDTO) {
        if (portfolioDTO == null) {
            throw new InvalidPortfolioException("Portfolio data cannot be null");
        }
        validateAssetType(portfolioDTO.getAssetType());

        BigDecimal requiredAmount = purchaseAmount(portfolioDTO.getAssetType(), portfolioDTO.getQuantity(), portfolioDTO.getBuyPrice());
        adjustBalanceForRequiredAmount(requiredAmount);

        Portfolio existing = portfolioRepository.findBySymbolAndPurchaseDate(
            portfolioDTO.getSymbol(),
            portfolioDTO.getPurchaseDate())
                .orElse(null);

        if (existing != null) {
            Portfolio mergedPortfolio = mergePortfolio(existing, portfolioDTO);
            Portfolio saved = portfolioRepository.save(mergedPortfolio);
            saved.setAvailableBalance(balanceService.getBalance().getAvailableBalance());
            return saved;
        }

        Portfolio portfolio = new Portfolio();
        portfolio.setSymbol(portfolioDTO.getSymbol());
        portfolio.setCompanyName(portfolioDTO.getCompanyName());
        portfolio.setAssetType(portfolioDTO.getAssetType());
        portfolio.setQuantity(portfolioDTO.getQuantity());
        portfolio.setBuyPrice(portfolioDTO.getBuyPrice());
        portfolio.setCurrentPrice(portfolioDTO.getCurrentPrice());
        portfolio.setPurchaseDate(portfolioDTO.getPurchaseDate());
        Portfolio saved = portfolioRepository.save(portfolio);
        saved.setAvailableBalance(balanceService.getBalance().getAvailableBalance());
        return saved;
    }

    private Portfolio mergePortfolio(Portfolio existing, PortfolioDTO portfolioDTO) {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(existing.getId());
        portfolio.setSymbol(existing.getSymbol());
        portfolio.setCompanyName(portfolioDTO.getCompanyName());
        portfolio.setAssetType(existing.getAssetType());
        portfolio.setQuantity(existing.getQuantity() + portfolioDTO.getQuantity());
        portfolio.setBuyPrice(calculateWeightedBuyPrice(existing, portfolioDTO));
        portfolio.setCurrentPrice(portfolioDTO.getCurrentPrice());
        portfolio.setPurchaseDate(portfolioDTO.getPurchaseDate());
        return portfolio;
    }

    private Double calculateWeightedBuyPrice(Portfolio existing, PortfolioDTO portfolioDTO) {
        BigDecimal existingCost = BigDecimal.valueOf(existing.getBuyPrice())
                .multiply(BigDecimal.valueOf(existing.getQuantity()));
        BigDecimal newCost = BigDecimal.valueOf(portfolioDTO.getBuyPrice())
                .multiply(BigDecimal.valueOf(portfolioDTO.getQuantity()));
        BigDecimal totalQuantity = BigDecimal.valueOf(existing.getQuantity() + portfolioDTO.getQuantity());

        return existingCost.add(newCost)
                .divide(totalQuantity, 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Transactional(readOnly = true)
    public List<Portfolio> getAllPortfolios() {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        BigDecimal balance = balanceService.getBalance().getAvailableBalance();
        portfolios.forEach(p -> p.setAvailableBalance(balance));
        return portfolios;
    }

    @Transactional(readOnly = true)
    public Portfolio getPortfolioById(Integer id) {
        validateId(id);
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(id));
        portfolio.setAvailableBalance(balanceService.getBalance().getAvailableBalance());
        return portfolio;
    }

    public Portfolio updatePortfolio(Integer id, PortfolioDTO portfolioDTO) {
        validateId(id);
        if (portfolioDTO == null) {
            throw new InvalidPortfolioException("Portfolio data cannot be null");
        }
        validateAssetType(portfolioDTO.getAssetType());

        Portfolio existing = portfolioRepository.findById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(id));

        BigDecimal existingAmount = purchaseAmount(existing.getAssetType(), existing.getQuantity(), existing.getBuyPrice());
        BigDecimal newAmount = purchaseAmount(portfolioDTO.getAssetType(), portfolioDTO.getQuantity(), portfolioDTO.getBuyPrice());
        BigDecimal delta = newAmount.subtract(existingAmount);

        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            adjustBalanceForRequiredAmount(delta);
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            creditBalance(delta.abs());
        }

        Portfolio portfolio = new Portfolio();
        portfolio.setId(id);
        portfolio.setSymbol(portfolioDTO.getSymbol());
        portfolio.setCompanyName(portfolioDTO.getCompanyName());
        portfolio.setAssetType(portfolioDTO.getAssetType());
        portfolio.setQuantity(portfolioDTO.getQuantity());
        portfolio.setBuyPrice(portfolioDTO.getBuyPrice());
        portfolio.setCurrentPrice(portfolioDTO.getCurrentPrice());
        portfolio.setPurchaseDate(portfolioDTO.getPurchaseDate());
        Portfolio saved = portfolioRepository.save(portfolio);
        saved.setAvailableBalance(balanceService.getBalance().getAvailableBalance());
        return saved;
    }

    public void deletePortfolio(Integer id) {
        validateId(id);

        Portfolio existing = portfolioRepository.findById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(id));

        BigDecimal amountToRefund = purchaseAmount(existing.getAssetType(), existing.getQuantity(), existing.getBuyPrice());
        if (amountToRefund.compareTo(BigDecimal.ZERO) > 0) {
            creditBalance(amountToRefund);
        }

        portfolioHistoryService.deleteHistoryForPortfolio(id);
        portfolioRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Integer id) {
        validateId(id);
        return portfolioRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public double getAvailableBalance() {
        return balanceService.getBalance().getAvailableBalance().doubleValue();
    }

    private BigDecimal purchaseAmount(String assetType, Integer quantity, Double buyPrice) {
        if (!isStockAsset(assetType)) {
            return BigDecimal.ZERO;
        }

        BigDecimal qty = BigDecimal.valueOf(quantity == null ? 0 : quantity);
        BigDecimal price = BigDecimal.valueOf(buyPrice == null ? 0.0 : buyPrice);
        return qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isStockAsset(String assetType) {
        return assetType != null && assetType.equalsIgnoreCase("Stock");
    }

    private void adjustBalanceForRequiredAmount(BigDecimal requiredAmount) {

    if (requiredAmount.compareTo(BigDecimal.ZERO) <= 0) {
        return;
    }

    balanceService.deductBalance(requiredAmount);
}

    private void creditBalance(BigDecimal amount) {

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        return;
    }

    balanceService.addBalance(amount);
}

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new InvalidIdException(id);
        }
    }

    private void validateAssetType(String assetType) {
        if (assetType == null || assetType.isBlank()) {
            throw new UnsupportedAssetTypeException(String.valueOf(assetType));
        }
        if (!SUPPORTED_ASSET_TYPES.contains(assetType.trim().toLowerCase())) {
            throw new UnsupportedAssetTypeException(assetType);
        }
    }
}