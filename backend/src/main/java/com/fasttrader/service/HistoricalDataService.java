package com.fasttrader.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasttrader.controller.dto.OHLCResponse;
import com.fasttrader.model.OHLC;
import java.math.BigDecimal;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HistoricalDataService {

    private final ObjectMapper objectMapper;
    private final Map<String, List<OHLC>> historicalData = new HashMap<>();

    @Value("${app.data.path:data/historical}")
    private String dataPath;

    public HistoricalDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadHistoricalData() {
        try {
            File dataDir = new File(dataPath);
            if (!dataDir.exists()) {
                log.warn("Historical data directory not found: {}", dataPath);
                return;
            }

            File[] jsonFiles = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (jsonFiles == null) {
                log.warn("No JSON files found in data directory: {}", dataPath);
                return;
            }

            for (File file : jsonFiles) {
                String symbol = file.getName().replace(".json", "");
                try {
                    HistoricalDataFile dataFile = objectMapper.readValue(file, HistoricalDataFile.class);
                    List<OHLC> ohlcData = dataFile.getData().stream()
                        .map(this::convertToOHLC)
                        .collect(Collectors.toList());
                    
                    historicalData.put(symbol, ohlcData);
                    log.info("Loaded {} OHLC records for symbol: {}", ohlcData.size(), symbol);
                } catch (IOException e) {
                    log.error("Failed to load historical data for {}: {}", symbol, e.getMessage());
                }
            }

            log.info("Historical data loading completed. Loaded data for {} symbols", historicalData.size());
        } catch (Exception e) {
            log.error("Error during historical data initialization: {}", e.getMessage(), e);
        }
    }

    public OHLCResponse getOHLCData(String symbol, String interval, int periods) {
        List<OHLC> data = historicalData.get(symbol.toUpperCase());
        if (data == null || data.isEmpty()) {
            return OHLCResponse.builder()
                .symbol(symbol)
                .interval(interval)
                .candles(Collections.emptyList())
                .build();
        }

        // Take the last 'periods' number of records
        List<OHLCResponse.Candle> candles = data.stream()
            .skip(Math.max(0, data.size() - periods))
            .map(this::convertToCandle)
            .collect(Collectors.toList());

        return OHLCResponse.builder()
            .symbol(symbol)
            .interval(interval)
            .candles(candles)
            .build();
    }

    public List<String> getAvailableSymbols() {
        return new ArrayList<>(historicalData.keySet());
    }

    public boolean hasDataForSymbol(String symbol) {
        return historicalData.containsKey(symbol.toUpperCase());
    }

    private OHLC convertToOHLC(HistoricalDataPoint point) {
        return OHLC.builder()
            .timestamp(point.getTimestamp())
            .open(point.getOpen())
            .high(point.getHigh())
            .low(point.getLow())
            .close(point.getClose())
            .volume(point.getVolume())
            .build();
    }

    private OHLCResponse.Candle convertToCandle(OHLC ohlc) {
        return OHLCResponse.Candle.builder()
            .timestamp(Instant.parse(ohlc.getTimestamp()).toEpochMilli())
            .open(BigDecimal.valueOf(ohlc.getOpen()))
            .high(BigDecimal.valueOf(ohlc.getHigh()))
            .low(BigDecimal.valueOf(ohlc.getLow()))
            .close(BigDecimal.valueOf(ohlc.getClose()))
            .volume(ohlc.getVolume())
            .trades(0L) // Default value since we don't have this data
            .build();
    }

    // Inner classes for JSON deserialization
    public static class HistoricalDataFile {
        private String symbol;
        private List<HistoricalDataPoint> data;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public List<HistoricalDataPoint> getData() {
            return data;
        }

        public void setData(List<HistoricalDataPoint> data) {
            this.data = data;
        }
    }

    public static class HistoricalDataPoint {
        private String timestamp;
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Long volume;

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public Double getOpen() {
            return open;
        }

        public void setOpen(Double open) {
            this.open = open;
        }

        public Double getHigh() {
            return high;
        }

        public void setHigh(Double high) {
            this.high = high;
        }

        public Double getLow() {
            return low;
        }

        public void setLow(Double low) {
            this.low = low;
        }

        public Double getClose() {
            return close;
        }

        public void setClose(Double close) {
            this.close = close;
        }

        public Long getVolume() {
            return volume;
        }

        public void setVolume(Long volume) {
            this.volume = volume;
        }
    }
}