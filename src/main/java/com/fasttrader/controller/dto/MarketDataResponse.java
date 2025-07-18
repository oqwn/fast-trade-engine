package com.fasttrader.controller.dto;

import com.fasttrader.model.MarketData;
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
public class MarketDataResponse {
    
    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private Long bidSize;
    private Long askSize;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal previousClose;
    private Long volume;
    private Long trades;
    private BigDecimal change;
    private BigDecimal changePercent;
    private List<PriceLevel> bidDepth;
    private List<PriceLevel> askDepth;
    private Instant timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceLevel {
        private BigDecimal price;
        private Long quantity;
    }
    
    public static MarketDataResponse from(MarketData marketData) {
        MarketDataResponse.MarketDataResponseBuilder builder = MarketDataResponse.builder()
            .symbol(marketData.getSymbol())
            .lastPrice(marketData.getLastPrice())
            .bidPrice(marketData.getBidPrice())
            .askPrice(marketData.getAskPrice())
            .bidSize(marketData.getBidSize())
            .askSize(marketData.getAskSize())
            .openPrice(marketData.getOpenPrice())
            .highPrice(marketData.getHighPrice())
            .lowPrice(marketData.getLowPrice())
            .previousClose(marketData.getPreviousClose())
            .volume(marketData.getVolume())
            .trades(marketData.getTrades())
            .change(marketData.getChange())
            .changePercent(marketData.getChangePercent())
            .timestamp(marketData.getTimestamp());
        
        if (marketData.getBidDepth() != null) {
            List<PriceLevel> bidLevels = marketData.getBidDepth().entrySet().stream()
                .map(entry -> PriceLevel.builder()
                    .price(entry.getKey())
                    .quantity(entry.getValue())
                    .build())
                .collect(Collectors.toList());
            builder.bidDepth(bidLevels);
        }
        
        if (marketData.getAskDepth() != null) {
            List<PriceLevel> askLevels = marketData.getAskDepth().entrySet().stream()
                .map(entry -> PriceLevel.builder()
                    .price(entry.getKey())
                    .quantity(entry.getValue())
                    .build())
                .collect(Collectors.toList());
            builder.askDepth(askLevels);
        }
        
        return builder.build();
    }
}