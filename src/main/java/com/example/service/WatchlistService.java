package com.example.service;

import java.util.List;

public interface WatchlistService {

    List<String> getStockWatchlist();

    List<String> getCryptoWatchlist();

    List<String> getFundWatchlist();

    List<String> getBondWatchlist();
}
