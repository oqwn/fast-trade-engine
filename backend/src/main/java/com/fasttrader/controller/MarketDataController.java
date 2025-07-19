package com.fasttrader.controller;

import com.fasttrader.controller.dto.MarketDataResponse;
import com.fasttrader.controller.dto.OHLCResponse;
import com.fasttrader.model.MarketData;
import com.fasttrader.service.MarketDataService;
import com.fasttrader.service.HistoricalDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/market-data")
@RequiredArgsConstructor
public class MarketDataController {
    
    private final MarketDataService marketDataService;
    private final HistoricalDataService historicalDataService;
    
    @GetMapping("/quote/{symbol}")
    public ResponseEntity<MarketDataResponse> getQuote(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "5") int depth) {
        
        MarketData marketData = marketDataService.getMarketData(symbol, depth);
        return ResponseEntity.ok(MarketDataResponse.from(marketData));
    }
    
    @GetMapping("/quotes")
    public ResponseEntity<List<MarketDataResponse>> getAllQuotes() {
        List<MarketData> allMarketData = marketDataService.getAllMarketData();
        
        List<MarketDataResponse> response = allMarketData.stream()
            .map(MarketDataResponse::from)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/ohlc/{symbol}")
    public ResponseEntity<OHLCResponse> getOHLC(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "100") int periods) {
        
        OHLCResponse ohlcData = historicalDataService.getOHLCData(symbol, interval, periods);
        return ResponseEntity.ok(ohlcData);
    }
    
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getAvailableSymbols() {
        List<String> symbols = historicalDataService.getAvailableSymbols();
        return ResponseEntity.ok(symbols);
    }
    
    private long parseInterval(String interval) {
        switch (interval.toLowerCase()) {
            case "1m":
                return 60000;
            case "5m":
                return 300000;
            case "15m":
                return 900000;
            case "30m":
                return 1800000;
            case "1h":
                return 3600000;
            case "4h":
                return 14400000;
            case "1d":
                return 86400000;
            default:
                return 60000;
        }
    }
}