package com.fasttrader.engine;

import com.fasttrader.model.*;
import com.fasttrader.model.enums.OrderSide;
import com.fasttrader.model.enums.OrderStatus;
import com.fasttrader.model.enums.OrderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Component
public class MatchingEngine {
    
    private final Map<String, OrderBook> orderBooks;
    private final ReadWriteLock lock;
    private final List<MatchingEngineListener> listeners;
    
    public MatchingEngine() {
        this.orderBooks = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.listeners = new ArrayList<>();
    }
    
    public void addListener(MatchingEngineListener listener) {
        listeners.add(listener);
    }
    
    public OrderBook getOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol, OrderBook::new);
    }
    
    public MatchResult processOrder(Order order) {
        validateOrder(order);
        
        OrderBook orderBook = getOrderBook(order.getSymbol());
        MatchResult result = new MatchResult(order);
        
        orderBook.getLock().writeLock().lock();
        try {
            if (order.getType() == OrderType.MARKET) {
                matchMarketOrder(order, orderBook, result);
            } else if (order.getType() == OrderType.LIMIT) {
                matchLimitOrder(order, orderBook, result);
            } else {
                throw new UnsupportedOperationException("Order type not supported: " + order.getType());
            }
            
            notifyListeners(result);
            
        } finally {
            orderBook.getLock().writeLock().unlock();
        }
        
        return result;
    }
    
    private void matchMarketOrder(Order order, OrderBook orderBook, MatchResult result) {
        ConcurrentSkipListMap<BigDecimal, OrderQueue> oppositeSide = 
            order.getSide() == OrderSide.BUY ? orderBook.getAskLevels() : orderBook.getBidLevels();
        
        Long remainingQuantity = order.getQuantity();
        
        while (remainingQuantity > 0 && !oppositeSide.isEmpty()) {
            Map.Entry<BigDecimal, OrderQueue> bestLevel = oppositeSide.firstEntry();
            OrderQueue queue = bestLevel.getValue();
            
            while (remainingQuantity > 0 && !queue.isEmpty()) {
                Order passiveOrder = queue.peekFirst();
                if (passiveOrder == null || !passiveOrder.isActive()) {
                    queue.pollFirst();
                    continue;
                }
                
                Long matchQuantity = Math.min(remainingQuantity, passiveOrder.getRemainingQuantity());
                BigDecimal matchPrice = passiveOrder.getPrice();
                
                Trade trade = executeTrade(order, passiveOrder, matchPrice, matchQuantity);
                result.addTrade(trade);
                
                updateOrderPostTrade(passiveOrder, matchQuantity, queue);
                remainingQuantity -= matchQuantity;
                
                orderBook.recordTrade(trade);
            }
            
            if (queue.isEmpty()) {
                oppositeSide.remove(bestLevel.getKey());
            }
        }
        
        order.setFilledQuantity(order.getQuantity() - remainingQuantity);
        if (remainingQuantity == 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.CANCELLED);
            log.warn("Market order {} partially filled. Filled: {}, Remaining: {}", 
                order.getOrderId(), order.getFilledQuantity(), remainingQuantity);
        }
        
        result.setFinalOrder(order);
    }
    
    private void matchLimitOrder(Order order, OrderBook orderBook, MatchResult result) {
        ConcurrentSkipListMap<BigDecimal, OrderQueue> oppositeSide = 
            order.getSide() == OrderSide.BUY ? orderBook.getAskLevels() : orderBook.getBidLevels();
        
        Long remainingQuantity = order.getRemainingQuantity();
        
        Iterator<Map.Entry<BigDecimal, OrderQueue>> it = oppositeSide.entrySet().iterator();
        while (remainingQuantity > 0 && it.hasNext()) {
            Map.Entry<BigDecimal, OrderQueue> entry = it.next();
            BigDecimal levelPrice = entry.getKey();
            OrderQueue queue = entry.getValue();
            
            if (!isPriceMatch(order, levelPrice)) {
                break;
            }
            
            while (remainingQuantity > 0 && !queue.isEmpty()) {
                Order passiveOrder = queue.peekFirst();
                if (passiveOrder == null || !passiveOrder.isActive()) {
                    queue.pollFirst();
                    continue;
                }
                
                Long matchQuantity = Math.min(remainingQuantity, passiveOrder.getRemainingQuantity());
                BigDecimal matchPrice = determineMatchPrice(order, passiveOrder);
                
                Trade trade = executeTrade(order, passiveOrder, matchPrice, matchQuantity);
                result.addTrade(trade);
                
                updateOrderPostTrade(passiveOrder, matchQuantity, queue);
                remainingQuantity -= matchQuantity;
                
                orderBook.recordTrade(trade);
            }
            
            if (queue.isEmpty()) {
                it.remove();
            }
        }
        
        order.fill(order.getQuantity() - remainingQuantity);
        
        if (remainingQuantity > 0 && order.isActive()) {
            orderBook.addOrder(order);
            log.debug("Added remaining limit order {} to book. Remaining: {}", 
                order.getOrderId(), remainingQuantity);
        }
        
        result.setFinalOrder(order);
    }
    
    private boolean isPriceMatch(Order aggressiveOrder, BigDecimal passivePrice) {
        if (aggressiveOrder.getSide() == OrderSide.BUY) {
            return aggressiveOrder.getPrice().compareTo(passivePrice) >= 0;
        } else {
            return aggressiveOrder.getPrice().compareTo(passivePrice) <= 0;
        }
    }
    
    private BigDecimal determineMatchPrice(Order aggressive, Order passive) {
        return passive.getTimestamp() < aggressive.getTimestamp() ? 
            passive.getPrice() : aggressive.getPrice();
    }
    
    private Trade executeTrade(Order buyOrder, Order sellOrder, BigDecimal price, Long quantity) {
        Order actualBuy = buyOrder.getSide() == OrderSide.BUY ? buyOrder : sellOrder;
        Order actualSell = buyOrder.getSide() == OrderSide.SELL ? buyOrder : sellOrder;
        
        Trade trade = Trade.create(actualBuy, actualSell, price, quantity);
        
        log.info("Trade executed: {} @ {} x {} between {} and {}", 
            trade.getSymbol(), price, quantity, actualBuy.getOrderId(), actualSell.getOrderId());
        
        return trade;
    }
    
    private void updateOrderPostTrade(Order order, Long filledQuantity, OrderQueue queue) {
        Long oldRemaining = order.getRemainingQuantity();
        order.fill(filledQuantity);
        
        if (order.isFilled()) {
            queue.pollFirst();
        } else {
            queue.updateQuantity(order, oldRemaining);
        }
    }
    
    public Order cancelOrder(String symbol, String orderId) {
        OrderBook orderBook = getOrderBook(symbol);
        
        orderBook.getLock().writeLock().lock();
        try {
            Order order = orderBook.removeOrder(orderId);
            if (order != null) {
                order.cancel();
                log.info("Order cancelled: {}", orderId);
                
                CancelEvent event = new CancelEvent(order);
                listeners.forEach(listener -> listener.onOrderCancelled(event));
            }
            return order;
            
        } finally {
            orderBook.getLock().writeLock().unlock();
        }
    }
    
    public Order modifyOrder(String symbol, String orderId, BigDecimal newPrice, Long newQuantity) {
        OrderBook orderBook = getOrderBook(symbol);
        
        orderBook.getLock().writeLock().lock();
        try {
            Order order = orderBook.removeOrder(orderId);
            if (order == null) {
                return null;
            }
            
            if (newQuantity != null && newQuantity < order.getFilledQuantity()) {
                throw new IllegalArgumentException("New quantity cannot be less than filled quantity");
            }
            
            Order newOrder = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .accountId(order.getAccountId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .type(order.getType())
                .price(newPrice != null ? newPrice : order.getPrice())
                .quantity(newQuantity != null ? newQuantity : order.getQuantity())
                .filledQuantity(order.getFilledQuantity())
                .status(order.getFilledQuantity() > 0 ? OrderStatus.PARTIALLY_FILLED : OrderStatus.NEW)
                .clientOrderId(order.getClientOrderId())
                .build();
            
            MatchResult result = processOrder(newOrder);
            
            log.info("Order modified: {} -> {}", orderId, newOrder.getOrderId());
            return result.getFinalOrder();
            
        } finally {
            orderBook.getLock().writeLock().unlock();
        }
    }
    
    private void validateOrder(Order order) {
        if (order.getSymbol() == null || order.getSymbol().isEmpty()) {
            throw new IllegalArgumentException("Order symbol is required");
        }
        
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive");
        }
        
        if (order.getType() == OrderType.LIMIT && 
            (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Limit order must have positive price");
        }
        
        if (order.getSide() == null) {
            throw new IllegalArgumentException("Order side is required");
        }
        
        if (order.getAccountId() == null || order.getAccountId().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }
    }
    
    private void notifyListeners(MatchResult result) {
        result.getTrades().forEach(trade -> 
            listeners.forEach(listener -> listener.onTrade(trade)));
        
        if (result.getFinalOrder() != null && result.getFinalOrder().isActive()) {
            listeners.forEach(listener -> 
                listener.onOrderPlaced(result.getFinalOrder()));
        }
    }
    
    public Map<String, OrderBook> getAllOrderBooks() {
        return Collections.unmodifiableMap(orderBooks);
    }
    
    public void reset() {
        lock.writeLock().lock();
        try {
            orderBooks.values().forEach(OrderBook::reset);
            log.info("All order books reset");
        } finally {
            lock.writeLock().unlock();
        }
    }
}