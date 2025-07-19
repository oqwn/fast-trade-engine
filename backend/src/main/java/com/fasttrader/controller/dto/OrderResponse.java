package com.fasttrader.controller.dto;

import com.fasttrader.engine.MatchResult;
import com.fasttrader.model.Order;
import com.fasttrader.model.Trade;
import com.fasttrader.model.enums.OrderSide;
import com.fasttrader.model.enums.OrderStatus;
import com.fasttrader.model.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private String orderId;
    private String accountId;
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private Long quantity;
    private Long filledQuantity;
    private Long remainingQuantity;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String clientOrderId;
    private BigDecimal averagePrice;
    private List<TradeResponse> trades;
    
    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
            .orderId(order.getOrderId())
            .accountId(order.getAccountId())
            .symbol(order.getSymbol())
            .side(order.getSide())
            .type(order.getType())
            .price(order.getPrice())
            .quantity(order.getQuantity())
            .filledQuantity(order.getFilledQuantity())
            .remainingQuantity(order.getRemainingQuantity())
            .status(order.getStatus())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .clientOrderId(order.getClientOrderId())
            .build();
    }
    
    public static OrderResponse from(MatchResult result) {
        OrderResponse response = from(result.getFinalOrder());
        response.setAveragePrice(result.getAveragePrice());
        response.setTrades(result.getTrades().stream()
            .map(TradeResponse::from)
            .collect(Collectors.toList()));
        return response;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradeResponse {
        private String tradeId;
        private BigDecimal price;
        private Long quantity;
        private Instant executionTime;
        
        public static TradeResponse from(Trade trade) {
            return TradeResponse.builder()
                .tradeId(trade.getTradeId())
                .price(trade.getPrice())
                .quantity(trade.getQuantity())
                .executionTime(trade.getExecutionTime())
                .build();
        }
    }
}