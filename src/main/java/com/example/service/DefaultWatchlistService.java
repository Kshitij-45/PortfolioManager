package com.example.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultWatchlistService implements WatchlistService {

    private static final List<String> STOCKS = List.of(
            "AAPL", "MSFT", "GOOGL", "AMZN", "META", "NVDA", "TSLA", "JPM",
            "V", "NFLX", "AMD", "INTC", "ORCL", "CRM", "COST"
    );

    private static final List<String> CRYPTO = List.of(
            "BTC-USD", "ETH-USD", "SOL-USD", "BNB-USD", "XRP-USD", "ADA-USD", "DOGE-USD",
            "AVAX-USD", "DOT-USD", "LINK-USD", "MATIC-USD", "TRX-USD", "LTC-USD"
    );

    private static final List<String> FUNDS = List.of(
            "SPY", "VOO", "QQQ", "VTI", "IVV", "DIA", "IWM", "SCHD",
            "XLK", "XLF", "XLE", "VNQ", "ARKK", "VUG"
    );

    private static final List<String> BONDS = List.of(
            "BND", "AGG", "TLT", "IEF", "LQD", "SHY",
            "BSV", "VCIT", "HYG", "JNK", "TIP", "MUB"
    );

    @Override
    public List<String> getStockWatchlist() {
        return STOCKS;
    }

    @Override
    public List<String> getCryptoWatchlist() {
        return CRYPTO;
    }

    @Override
    public List<String> getFundWatchlist() {
        return FUNDS;
    }

    @Override
    public List<String> getBondWatchlist() {
        return BONDS;
    }
}
