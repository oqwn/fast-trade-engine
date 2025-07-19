package com.fasttrader.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketData {
    
    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private Long bidSize;
    private Long askSize;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal previousClose;
    private Long volume;
    private BigDecimal turnover;
    private Long trades;
    private Instant timestamp;
    
    private BigDecimal change;
    private BigDecimal changePercent;
    
    private Map<BigDecimal, Long> bidDepth;
    private Map<BigDecimal, Long> askDepth;
    
    public static MarketData fromOrderBook(OrderBook orderBook) {
        BigDecimal bestBid = orderBook.getBestBid();
        BigDecimal bestAsk = orderBook.getBestAsk();
        
        MarketData.MarketDataBuilder builder = MarketData.builder()
            .symbol(orderBook.getSymbol())
            .lastPrice(orderBook.getLastPrice())
            .bidPrice(bestBid)
            .askPrice(bestAsk)
            .openPrice(orderBook.getOpenPrice())
            .highPrice(orderBook.getHighPrice())
            .lowPrice(orderBook.getLowPrice())
            .previousClose(orderBook.getPreviousClose())
            .volume(orderBook.getTotalVolume())
            .trades(orderBook.getTotalTrades())
            .timestamp(Instant.now());
        
        if (bestBid != null) {
            builder.bidSize(orderBook.getBestBidQueue().getTotalQuantity());
        }
        
        if (bestAsk != null) {
            builder.askSize(orderBook.getBestAskQueue().getTotalQuantity());
        }
        
        if (orderBook.getLastPrice() != null && orderBook.getPreviousClose() != null) {
            BigDecimal change = orderBook.getLastPrice().subtract(orderBook.getPreviousClose());
            BigDecimal changePercent = change.divide(orderBook.getPreviousClose(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            builder.change(change).changePercent(changePercent);
        }
        
        return builder.build();
    }
    
    public MarketData withDepth(Map<BigDecimal, Long> bidDepth, Map<BigDecimal, Long> askDepth) {
        this.bidDepth = bidDepth;
        this.askDepth = askDepth;
        return this;
    }
}