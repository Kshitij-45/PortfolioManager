package com.example.service;

import com.example.dto.IndicatorDTO;
import com.example.dto.MarketCandleDTO;

import java.util.List;

public interface TechnicalIndicatorService {

    IndicatorDTO calculateIndicators(List<MarketCandleDTO> candles);
}
