package com.fasttrader.controller.dto;

import com.fasttrader.model.OrderBook;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookResponse {
    
    private String symbol;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;
    private Long bestBidSize;
    private Long bestAskSize;
    private BigDecimal lastPrice;
    private BigDecimal spread;
    private List<PriceLevel> bidLevels;
    private List<PriceLevel> askLevels;
    private Instant timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceLevel {
        private BigDecimal price;
        private Long quantity;
        private int orderCount;
    }
    
    public static OrderBookResponse from(OrderBook orderBook, int depth) {
        OrderBookResponse.OrderBookResponseBuilder builder = OrderBookResponse.builder()
            .symbol(orderBook.getSymbol())
            .bestBid(orderBook.getBestBid())
            .bestAsk(orderBook.getBestAsk())
            .lastPrice(orderBook.getLastPrice())
            .spread(orderBook.getSpread())
            .timestamp(Instant.now());
        
        if (orderBook.getBestBidQueue() != null) {
            builder.bestBidSize(orderBook.getBestBidQueue().getTotalQuantity());
        }
        
        if (orderBook.getBestAskQueue() != null) {
            builder.bestAskSize(orderBook.getBestAskQueue().getTotalQuantity());
        }
        
        Map<BigDecimal, Long> bidDepth = orderBook.getBidLevels(depth);
        List<PriceLevel> bidLevels = bidDepth.entrySet().stream()
            .map(entry -> PriceLevel.builder()
                .price(entry.getKey())
                .quantity(entry.getValue())
                .build())
            .collect(Collectors.toList());
        builder.bidLevels(bidLevels);
        
        Map<BigDecimal, Long> askDepth = orderBook.getAskLevels(depth);
        List<PriceLevel> askLevels = askDepth.entrySet().stream()
            .map(entry -> PriceLevel.builder()
                .price(entry.getKey())
                .quantity(entry.getValue())
                .build())
            .collect(Collectors.toList());
        builder.askLevels(askLevels);
        
        return builder.build();
    }
}