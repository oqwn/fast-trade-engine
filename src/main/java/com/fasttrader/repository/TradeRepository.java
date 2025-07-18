package com.fasttrader.repository;

import com.fasttrader.model.Trade;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class TradeRepository {
    
    private final Map<String, Trade> trades = new ConcurrentHashMap<>();
    
    public Trade save(Trade trade) {
        trades.put(trade.getTradeId(), trade);
        return trade;
    }
    
    public Optional<Trade> findById(String tradeId) {
        return Optional.ofNullable(trades.get(tradeId));
    }
    
    public List<Trade> findAll() {
        return trades.values().stream()
            .sorted((t1, t2) -> t2.getExecutionTime().compareTo(t1.getExecutionTime()))
            .collect(Collectors.toList());
    }
    
    public List<Trade> findBySymbol(String symbol) {
        return trades.values().stream()
            .filter(trade -> symbol.equals(trade.getSymbol()))
            .sorted((t1, t2) -> t2.getExecutionTime().compareTo(t1.getExecutionTime()))
            .collect(Collectors.toList());
    }
    
    public List<Trade> findByBuyAccountId(String buyAccountId) {
        return trades.values().stream()
            .filter(trade -> buyAccountId.equals(trade.getBuyAccountId()))
            .sorted((t1, t2) -> t2.getExecutionTime().compareTo(t1.getExecutionTime()))
            .collect(Collectors.toList());
    }
    
    public List<Trade> findBySellAccountId(String sellAccountId) {
        return trades.values().stream()
            .filter(trade -> sellAccountId.equals(trade.getSellAccountId()))
            .sorted((t1, t2) -> t2.getExecutionTime().compareTo(t1.getExecutionTime()))
            .collect(Collectors.toList());
    }
    
    public List<Trade> findByAccountId(String accountId) {
        return trades.values().stream()
            .filter(trade -> accountId.equals(trade.getBuyAccountId()) || 
                           accountId.equals(trade.getSellAccountId()))
            .sorted((t1, t2) -> t2.getExecutionTime().compareTo(t1.getExecutionTime()))
            .collect(Collectors.toList());
    }
    
    public List<Trade> findByExecutionTimeBetween(Instant start, Instant end) {
        return trades.values().stream()
            .filter(trade -> !trade.getExecutionTime().isBefore(start) && 
                           !trade.getExecutionTime().isAfter(end))
            .sorted((t1, t2) -> t2.getExecutionTime().compareTo(t1.getExecutionTime()))
            .collect(Collectors.toList());
    }
    
    public List<Trade> findBySymbolAndExecutionTimeBetween(String symbol, Instant start, Instant end) {
        return trades.values().stream()
            .filter(trade -> symbol.equals(trade.getSymbol()) &&
                           !trade.getExecutionTime().isBefore(start) && 
                           !trade.getExecutionTime().isAfter(end))
            .sorted((t1, t2) -> t2.getExecutionTime().compareTo(t1.getExecutionTime()))
            .collect(Collectors.toList());
    }
    
    public void deleteById(String tradeId) {
        trades.remove(tradeId);
    }
    
    public void deleteAll() {
        trades.clear();
    }
    
    public long count() {
        return trades.size();
    }
    
    public boolean existsById(String tradeId) {
        return trades.containsKey(tradeId);
    }
}