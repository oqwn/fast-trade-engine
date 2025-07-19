package com.fasttrader.controller.dto;

import com.fasttrader.service.MarketDataService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OHLCResponse {
    
    private String symbol;
    private String interval;
    private List<Candle> candles;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candle {
        private long timestamp;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;
        private Long trades;
    }
    
    public static OHLCResponse from(String symbol, String interval, List<MarketDataService.OHLCData> ohlcData) {
        List<Candle> candles = ohlcData.stream()
            .map(data -> Candle.builder()
                .timestamp(data.getTimestamp())
                .open(data.getOpen())
                .high(data.getHigh())
                .low(data.getLow())
                .close(data.getClose())
                .volume(data.getVolume())
                .trades(data.getTrades())
                .build())
            .collect(Collectors.toList());
        
        return OHLCResponse.builder()
            .symbol(symbol)
            .interval(interval)
            .candles(candles)
            .build();
    }
}