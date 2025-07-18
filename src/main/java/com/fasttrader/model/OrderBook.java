package com.fasttrader.model;

import com.fasttrader.model.enums.MarketState;
import com.fasttrader.model.enums.OrderSide;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Getter
public class OrderBook {
    
    private final String symbol;
    private final ConcurrentSkipListMap<BigDecimal, OrderQueue> bidLevels;
    private final ConcurrentSkipListMap<BigDecimal, OrderQueue> askLevels;
    private final Map<String, Order> orderMap;
    private final ReadWriteLock lock;
    
    private volatile MarketState marketState;
    private volatile BigDecimal lastPrice;
    private volatile Long lastUpdateTime;
    private volatile BigDecimal previousClose;
    private volatile BigDecimal openPrice;
    private volatile BigDecimal highPrice;
    private volatile BigDecimal lowPrice;
    private volatile Long totalVolume;
    private volatile Long totalTrades;
    
    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.bidLevels = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        this.askLevels = new ConcurrentSkipListMap<>();
        this.orderMap = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.marketState = MarketState.CLOSED;
        this.totalVolume = 0L;
        this.totalTrades = 0L;
    }
    
    public void addOrder(Order order) {
        lock.writeLock().lock();
        try {
            if (orderMap.containsKey(order.getOrderId())) {
                throw new IllegalArgumentException("Order already exists: " + order.getOrderId());
            }
            
            orderMap.put(order.getOrderId(), order);
            
            ConcurrentSkipListMap<BigDecimal, OrderQueue> levels = 
                order.getSide() == OrderSide.BUY ? bidLevels : askLevels;
            
            OrderQueue queue = levels.computeIfAbsent(order.getPrice(), 
                k -> new OrderQueue(order.getPrice()));
            queue.addOrder(order);
            
            lastUpdateTime = System.nanoTime();
            log.debug("Added order {} to {} book at price {}", 
                order.getOrderId(), order.getSide(), order.getPrice());
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public Order removeOrder(String orderId) {
        lock.writeLock().lock();
        try {
            Order order = orderMap.remove(orderId);
            if (order == null) {
                return null;
            }
            
            ConcurrentSkipListMap<BigDecimal, OrderQueue> levels = 
                order.getSide() == OrderSide.BUY ? bidLevels : askLevels;
            
            OrderQueue queue = levels.get(order.getPrice());
            if (queue != null) {
                queue.removeOrder(order);
                if (queue.isEmpty()) {
                    levels.remove(order.getPrice());
                }
            }
            
            lastUpdateTime = System.nanoTime();
            return order;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public Order getOrder(String orderId) {
        lock.readLock().lock();
        try {
            return orderMap.get(orderId);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public BigDecimal getBestBid() {
        return bidLevels.isEmpty() ? null : bidLevels.firstKey();
    }
    
    public BigDecimal getBestAsk() {
        return askLevels.isEmpty() ? null : askLevels.firstKey();
    }
    
    public OrderQueue getBestBidQueue() {
        return bidLevels.isEmpty() ? null : bidLevels.firstEntry().getValue();
    }
    
    public OrderQueue getBestAskQueue() {
        return askLevels.isEmpty() ? null : askLevels.firstEntry().getValue();
    }
    
    public BigDecimal getMidPrice() {
        BigDecimal bestBid = getBestBid();
        BigDecimal bestAsk = getBestAsk();
        
        if (bestBid == null || bestAsk == null) {
            return lastPrice;
        }
        
        return bestBid.add(bestAsk).divide(BigDecimal.valueOf(2));
    }
    
    public BigDecimal getSpread() {
        BigDecimal bestBid = getBestBid();
        BigDecimal bestAsk = getBestAsk();
        
        if (bestBid == null || bestAsk == null) {
            return null;
        }
        
        return bestAsk.subtract(bestBid);
    }
    
    public Long getBidDepth(int levels) {
        lock.readLock().lock();
        try {
            return bidLevels.entrySet().stream()
                .limit(levels)
                .mapToLong(e -> e.getValue().getTotalQuantity())
                .sum();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public Long getAskDepth(int levels) {
        lock.readLock().lock();
        try {
            return askLevels.entrySet().stream()
                .limit(levels)
                .mapToLong(e -> e.getValue().getTotalQuantity())
                .sum();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public Map<BigDecimal, Long> getBidLevels(int depth) {
        lock.readLock().lock();
        try {
            Map<BigDecimal, Long> result = new LinkedHashMap<>();
            bidLevels.entrySet().stream()
                .limit(depth)
                .forEach(e -> result.put(e.getKey(), e.getValue().getTotalQuantity()));
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public Map<BigDecimal, Long> getAskLevels(int depth) {
        lock.readLock().lock();
        try {
            Map<BigDecimal, Long> result = new LinkedHashMap<>();
            askLevels.entrySet().stream()
                .limit(depth)
                .forEach(e -> result.put(e.getKey(), e.getValue().getTotalQuantity()));
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void updateMarketState(MarketState newState) {
        this.marketState = newState;
        log.info("Market state changed to {} for symbol {}", newState, symbol);
    }
    
    public void updateLastPrice(BigDecimal price) {
        this.lastPrice = price;
        
        if (this.openPrice == null) {
            this.openPrice = price;
        }
        
        if (this.highPrice == null || price.compareTo(this.highPrice) > 0) {
            this.highPrice = price;
        }
        
        if (this.lowPrice == null || price.compareTo(this.lowPrice) < 0) {
            this.lowPrice = price;
        }
    }
    
    public void recordTrade(Trade trade) {
        updateLastPrice(trade.getPrice());
        this.totalVolume += trade.getQuantity();
        this.totalTrades++;
    }
    
    public void reset() {
        lock.writeLock().lock();
        try {
            bidLevels.clear();
            askLevels.clear();
            orderMap.clear();
            lastUpdateTime = System.nanoTime();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void resetDailyStats() {
        this.previousClose = this.lastPrice;
        this.openPrice = null;
        this.highPrice = null;
        this.lowPrice = null;
        this.totalVolume = 0L;
        this.totalTrades = 0L;
    }
    
    public int getOrderCount() {
        lock.readLock().lock();
        try {
            return orderMap.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return bidLevels.isEmpty() && askLevels.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }
}