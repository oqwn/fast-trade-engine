package com.fasttrader.engine;

import com.fasttrader.model.Order;
import com.fasttrader.model.Trade;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class MatchResult {
    
    private final Order originalOrder;
    private Order finalOrder;
    private final List<Trade> trades;
    private Long totalFilledQuantity;
    private BigDecimal averagePrice;
    
    public MatchResult(Order originalOrder) {
        this.originalOrder = originalOrder;
        this.trades = new ArrayList<>();
        this.totalFilledQuantity = 0L;
        this.averagePrice = BigDecimal.ZERO;
    }
    
    public void addTrade(Trade trade) {
        trades.add(trade);
        
        Long tradeQuantity = trade.getQuantity();
        BigDecimal tradeValue = trade.getPrice().multiply(BigDecimal.valueOf(tradeQuantity));
        
        BigDecimal totalValue = averagePrice.multiply(BigDecimal.valueOf(totalFilledQuantity))
            .add(tradeValue);
        
        totalFilledQuantity += tradeQuantity;
        
        if (totalFilledQuantity > 0) {
            averagePrice = totalValue.divide(BigDecimal.valueOf(totalFilledQuantity), 
                6, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    public boolean isFullyFilled() {
        return finalOrder != null && finalOrder.isFilled();
    }
    
    public boolean isPartiallyFilled() {
        return totalFilledQuantity > 0 && !isFullyFilled();
    }
    
    public Long getRemainingQuantity() {
        if (finalOrder != null) {
            return finalOrder.getRemainingQuantity();
        }
        return originalOrder.getQuantity() - totalFilledQuantity;
    }
    
    public BigDecimal getTotalValue() {
        return trades.stream()
            .map(Trade::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}