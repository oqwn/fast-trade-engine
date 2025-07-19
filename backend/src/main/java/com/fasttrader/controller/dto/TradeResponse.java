package com.fasttrader.controller.dto;

import com.fasttrader.model.Trade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {
    
    private String tradeId;
    private String symbol;
    private String buyOrderId;
    private String sellOrderId;
    private String buyAccountId;
    private String sellAccountId;
    private BigDecimal price;
    private Long quantity;
    private BigDecimal value;
    private String aggressorSide;
    private Instant executionTime;
    
    public static TradeResponse from(Trade trade) {
        return TradeResponse.builder()
            .tradeId(trade.getTradeId())
            .symbol(trade.getSymbol())
            .buyOrderId(trade.getBuyOrderId())
            .sellOrderId(trade.getSellOrderId())
            .buyAccountId(trade.getBuyAccountId())
            .sellAccountId(trade.getSellAccountId())
            .price(trade.getPrice())
            .quantity(trade.getQuantity())
            .value(trade.getValue())
            .aggressorSide(trade.getAggressorSide())
            .executionTime(trade.getExecutionTime())
            .build();
    }
}