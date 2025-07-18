package com.fasttrader.model;

import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    
    private String accountId;
    private String symbol;
    private Long quantity;
    private BigDecimal averagePrice;
    private BigDecimal realizedPnL;
    private BigDecimal unrealizedPnL;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    private Instant updatedAt;
    
    public void addPosition(Long addQuantity, BigDecimal price) {
        if (quantity >= 0) {
            BigDecimal totalCost = averagePrice.multiply(BigDecimal.valueOf(quantity))
                .add(price.multiply(BigDecimal.valueOf(addQuantity)));
            quantity += addQuantity;
            if (quantity > 0) {
                averagePrice = totalCost.divide(BigDecimal.valueOf(quantity), 6, RoundingMode.HALF_UP);
            }
        } else {
            Long closeQuantity = Math.min(addQuantity, -quantity);
            BigDecimal realized = price.subtract(averagePrice).multiply(BigDecimal.valueOf(closeQuantity));
            realizedPnL = realizedPnL.add(realized);
            
            quantity += addQuantity;
            if (quantity > 0) {
                averagePrice = price;
            }
        }
        updatedAt = Instant.now();
    }
    
    public void reducePosition(Long reduceQuantity, BigDecimal price) {
        if (quantity <= 0) {
            BigDecimal totalCost = averagePrice.multiply(BigDecimal.valueOf(-quantity))
                .add(price.multiply(BigDecimal.valueOf(reduceQuantity)));
            quantity -= reduceQuantity;
            if (quantity < 0) {
                averagePrice = totalCost.divide(BigDecimal.valueOf(-quantity), 6, RoundingMode.HALF_UP);
            }
        } else {
            Long closeQuantity = Math.min(reduceQuantity, quantity);
            BigDecimal realized = averagePrice.subtract(price).multiply(BigDecimal.valueOf(closeQuantity));
            realizedPnL = realizedPnL.add(realized);
            
            quantity -= reduceQuantity;
            if (quantity < 0) {
                averagePrice = price;
            }
        }
        updatedAt = Instant.now();
    }
    
    public BigDecimal getMarketValue(BigDecimal marketPrice) {
        return marketPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    public BigDecimal getUnrealizedPnL(BigDecimal marketPrice) {
        if (quantity == 0) {
            return BigDecimal.ZERO;
        }
        
        if (quantity > 0) {
            return marketPrice.subtract(averagePrice).multiply(BigDecimal.valueOf(quantity));
        } else {
            return averagePrice.subtract(marketPrice).multiply(BigDecimal.valueOf(-quantity));
        }
    }
    
    public BigDecimal getTotalPnL(BigDecimal marketPrice) {
        return realizedPnL.add(getUnrealizedPnL(marketPrice));
    }
    
    public boolean isLong() {
        return quantity > 0;
    }
    
    public boolean isShort() {
        return quantity < 0;
    }
    
    public boolean isClosed() {
        return quantity == 0;
    }
    
    public Long getAbsoluteQuantity() {
        return Math.abs(quantity);
    }
}