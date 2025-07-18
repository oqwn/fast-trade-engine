package com.fasttrader.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    
    private String accountId;
    private String accountName;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    
    @Builder.Default
    private Map<String, Position> positions = new ConcurrentHashMap<>();
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    private Instant updatedAt;
    
    @Builder.Default
    private boolean active = true;
    
    public synchronized void freezeFunds(BigDecimal amount) {
        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient available balance");
        }
        availableBalance = availableBalance.subtract(amount);
        frozenBalance = frozenBalance.add(amount);
        updatedAt = Instant.now();
    }
    
    public synchronized void unfreezeFunds(BigDecimal amount) {
        if (frozenBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient frozen balance");
        }
        frozenBalance = frozenBalance.subtract(amount);
        availableBalance = availableBalance.add(amount);
        updatedAt = Instant.now();
    }
    
    public synchronized void deductFunds(BigDecimal amount) {
        if (frozenBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient frozen balance");
        }
        frozenBalance = frozenBalance.subtract(amount);
        balance = balance.subtract(amount);
        updatedAt = Instant.now();
    }
    
    public synchronized void addFunds(BigDecimal amount) {
        balance = balance.add(amount);
        availableBalance = availableBalance.add(amount);
        updatedAt = Instant.now();
    }
    
    public Position getPosition(String symbol) {
        return positions.get(symbol);
    }
    
    public void updatePosition(String symbol, Long quantity, BigDecimal price, boolean isBuy) {
        positions.compute(symbol, (k, existingPosition) -> {
            if (existingPosition == null) {
                return Position.builder()
                    .accountId(accountId)
                    .symbol(symbol)
                    .quantity(isBuy ? quantity : -quantity)
                    .averagePrice(price)
                    .build();
            } else {
                if (isBuy) {
                    existingPosition.addPosition(quantity, price);
                } else {
                    existingPosition.reducePosition(quantity, price);
                }
                return existingPosition;
            }
        });
        updatedAt = Instant.now();
    }
    
    public BigDecimal getTotalEquity(Map<String, BigDecimal> marketPrices) {
        BigDecimal positionValue = positions.values().stream()
            .map(pos -> {
                BigDecimal marketPrice = marketPrices.get(pos.getSymbol());
                if (marketPrice == null) {
                    return BigDecimal.ZERO;
                }
                return pos.getMarketValue(marketPrice);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return balance.add(positionValue);
    }
    
    public boolean hasAvailableBalance(BigDecimal amount) {
        return availableBalance.compareTo(amount) >= 0;
    }
    
    public boolean canOpenPosition(String symbol, Long quantity, BigDecimal price) {
        BigDecimal requiredAmount = price.multiply(BigDecimal.valueOf(quantity));
        return hasAvailableBalance(requiredAmount);
    }
}