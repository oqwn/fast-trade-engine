package com.fasttrader.controller;

import com.fasttrader.controller.dto.TradeResponse;
import com.fasttrader.model.Trade;
import com.fasttrader.repository.TradeRepository;
import com.fasttrader.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradeController {
    
    private final TradeRepository tradeRepository;
    private final MarketDataService marketDataService;
    
    @GetMapping
    public ResponseEntity<List<TradeResponse>> getTrades(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "100") int limit) {
        
        List<Trade> trades;
        
        if (symbol != null && from != null && to != null) {
            Instant fromInstant = from.toInstant(ZoneOffset.UTC);
            Instant toInstant = to.toInstant(ZoneOffset.UTC);
            trades = tradeRepository.findBySymbolAndExecutionTimeBetween(symbol, fromInstant, toInstant);
        } else if (symbol != null) {
            trades = tradeRepository.findBySymbol(symbol);
        } else if (accountId != null) {
            trades = tradeRepository.findByAccountId(accountId);
        } else if (from != null && to != null) {
            Instant fromInstant = from.toInstant(ZoneOffset.UTC);
            Instant toInstant = to.toInstant(ZoneOffset.UTC);
            trades = tradeRepository.findByExecutionTimeBetween(fromInstant, toInstant);
        } else {
            trades = tradeRepository.findAll();
        }
        
        List<TradeResponse> response = trades.stream()
            .limit(limit)
            .map(TradeResponse::from)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{tradeId}")
    public ResponseEntity<TradeResponse> getTrade(@PathVariable String tradeId) {
        return tradeRepository.findById(tradeId)
            .map(TradeResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/recent/{symbol}")
    public ResponseEntity<List<TradeResponse>> getRecentTrades(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<Trade> recentTrades = marketDataService.getRecentTrades(symbol, limit);
        
        List<TradeResponse> response = recentTrades.stream()
            .map(TradeResponse::from)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
}