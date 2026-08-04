package com.example.service;

import com.example.dto.ProfitSummaryDTO;
import com.example.entity.Portfolio;
import com.example.entity.PortfolioHistory;
import com.example.repository.PortfolioHistoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioHistoryService {

    private final PortfolioHistoryRepository historyRepository;

    public PortfolioHistoryService(PortfolioHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Records a snapshot for a portfolio entry.
     * Called on every create or update so profit is tracked over time.
     */
    public void recordSnapshot(Portfolio portfolio) {
        BigDecimal profit = BigDecimal.valueOf(
                (portfolio.getCurrentPrice() - portfolio.getBuyPrice()) * portfolio.getQuantity()
        );

        PortfolioHistory history = new PortfolioHistory();
        history.setPortfolioId(portfolio.getId());
        history.setSymbol(portfolio.getSymbol());
        history.setRecordedDate(LocalDate.now());
        history.setBuyPrice(portfolio.getBuyPrice());
        history.setCurrentPrice(portfolio.getCurrentPrice());
        history.setQuantity(portfolio.getQuantity());
        history.setProfit(profit);

        historyRepository.save(history);
    }

    /**
     * Returns all snapshots for a specific portfolio entry.
     */
    public List<PortfolioHistory> getHistoryForPortfolio(Integer portfolioId) {
        return historyRepository.findByPortfolioId(portfolioId);
    }

    /**
     * Returns total profit grouped by date — used for the line graph.
     * Each element: { date, totalProfit }
     */
    public List<ProfitSummaryDTO> getDailyProfitSummary() {
        return historyRepository.findDailyProfitSummary()
                .stream()
                .map(row -> new ProfitSummaryDTO(
                        (LocalDate) row[0],
                        (BigDecimal) row[1]))
                .collect(Collectors.toList());
    }

    /**
     * Removes all history for a portfolio entry (called on portfolio delete).
     */
    public void deleteHistoryForPortfolio(Integer portfolioId) {
        historyRepository.deleteByPortfolioId(portfolioId);
    }
}
