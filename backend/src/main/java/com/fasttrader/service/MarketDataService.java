package com.fasttrader.service;

import com.fasttrader.engine.MatchingEngine;
import com.fasttrader.model.MarketData;
import com.fasttrader.model.OrderBook;
import com.fasttrader.model.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {
    
    private final MatchingEngine matchingEngine;
    private final Map<String, List<Trade>> recentTrades = new ConcurrentHashMap<>();
    private final Map<String, TreeMap<Long, OHLCData>> ohlcData = new ConcurrentHashMap<>();
    
    public OrderBook getOrderBook(String symbol) {
        return matchingEngine.getOrderBook(symbol);
    }
    
    public MarketData getMarketData(String symbol, int depth) {
        OrderBook orderBook = getOrderBook(symbol);
        MarketData marketData = MarketData.fromOrderBook(orderBook);
        
        if (depth > 0) {
            Map<BigDecimal, Long> bidDepth = orderBook.getBidLevels(depth);
            Map<BigDecimal, Long> askDepth = orderBook.getAskLevels(depth);
            marketData.withDepth(bidDepth, askDepth);
        }
        
        return marketData;
    }
    
    public List<MarketData> getAllMarketData() {
        return matchingEngine.getAllOrderBooks().values().stream()
            .map(MarketData::fromOrderBook)
            .collect(Collectors.toList());
    }
    
    public void recordTrade(Trade trade) {
        recentTrades.computeIfAbsent(trade.getSymbol(), k -> new ArrayList<>()).add(trade);
        
        updateOHLC(trade);
        
        limitRecentTrades(trade.getSymbol(), 1000);
    }
    
    public List<Trade> getRecentTrades(String symbol, int limit) {
        List<Trade> trades = recentTrades.getOrDefault(symbol, new ArrayList<>());
        int size = trades.size();
        return trades.subList(Math.max(0, size - limit), size);
    }
    
    public Map<String, BigDecimal> getCurrentPrices() {
        Map<String, BigDecimal> prices = new HashMap<>();
        matchingEngine.getAllOrderBooks().forEach((symbol, orderBook) -> {
            BigDecimal lastPrice = orderBook.getLastPrice();
            if (lastPrice != null) {
                prices.put(symbol, lastPrice);
            }
        });
        return prices;
    }
    
    public OHLCData getOHLC(String symbol, long intervalMillis) {
        TreeMap<Long, OHLCData> symbolOHLC = ohlcData.get(symbol);
        if (symbolOHLC == null || symbolOHLC.isEmpty()) {
            return null;
        }
        
        long currentInterval = System.currentTimeMillis() / intervalMillis;
        return symbolOHLC.get(currentInterval);
    }
    
    public List<OHLCData> getHistoricalOHLC(String symbol, long intervalMillis, int periods) {
        TreeMap<Long, OHLCData> symbolOHLC = ohlcData.get(symbol);
        if (symbolOHLC == null || symbolOHLC.isEmpty()) {
            return new ArrayList<>();
        }
        
        long currentInterval = System.currentTimeMillis() / intervalMillis;
        long startInterval = currentInterval - periods + 1;
        
        return new ArrayList<>(symbolOHLC.subMap(startInterval, true, currentInterval, true).values());
    }
    
    private void updateOHLC(Trade trade) {
        long oneMinuteInterval = trade.getExecutionTime().toEpochMilli() / 60000;
        
        ohlcData.computeIfAbsent(trade.getSymbol(), k -> new TreeMap<>())
            .compute(oneMinuteInterval, (k, existing) -> {
                if (existing == null) {
                    return new OHLCData(
                        trade.getSymbol(),
                        trade.getPrice(),
                        trade.getPrice(),
                        trade.getPrice(),
                        trade.getPrice(),
                        trade.getQuantity(),
                        1L,
                        oneMinuteInterval * 60000
                    );
                } else {
                    existing.update(trade.getPrice(), trade.getQuantity());
                    return existing;
                }
            });
    }
    
    private void limitRecentTrades(String symbol, int maxSize) {
        List<Trade> trades = recentTrades.get(symbol);
        if (trades != null && trades.size() > maxSize) {
            trades.subList(0, trades.size() - maxSize).clear();
        }
    }
    
    public static class OHLCData {
        private final String symbol;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;
        private Long trades;
        private final long timestamp;
        
        public OHLCData(String symbol, BigDecimal open, BigDecimal high, 
                       BigDecimal low, BigDecimal close, Long volume, Long trades, long timestamp) {
            this.symbol = symbol;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.trades = trades;
            this.timestamp = timestamp;
        }
        
        public void update(BigDecimal price, Long quantity) {
            if (high.compareTo(price) < 0) {
                high = price;
            }
            if (low.compareTo(price) > 0) {
                low = price;
            }
            close = price;
            volume += quantity;
            trades++;
        }
        
        public String getSymbol() { return symbol; }
        public BigDecimal getOpen() { return open; }
        public BigDecimal getHigh() { return high; }
        public BigDecimal getLow() { return low; }
        public BigDecimal getClose() { return close; }
        public Long getVolume() { return volume; }
        public Long getTrades() { return trades; }
        public long getTimestamp() { return timestamp; }
    }
}