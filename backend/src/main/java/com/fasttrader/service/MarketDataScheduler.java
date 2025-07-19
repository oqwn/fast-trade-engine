package com.fasttrader.service;

import com.fasttrader.model.MarketData;
import com.fasttrader.model.OrderBook;
import com.fasttrader.websocket.MarketDataWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataScheduler {
    
    private final MarketDataService marketDataService;
    private final MarketDataWebSocketService marketDataWebSocketService;
    
    @Scheduled(fixedDelay = 1000)
    public void publishMarketDataUpdates() {
        try {
            Map<String, OrderBook> orderBooks = marketDataService.getOrderBook("").getSymbol() != null ? 
                Map.of() : getAllOrderBooks();
            
            for (Map.Entry<String, OrderBook> entry : orderBooks.entrySet()) {
                String symbol = entry.getKey();
                OrderBook orderBook = entry.getValue();
                
                if (orderBook.getLastUpdateTime() != null) {
                    MarketData marketData = MarketData.fromOrderBook(orderBook);
                    marketDataWebSocketService.publishMarketData(marketData);
                    
                    marketDataWebSocketService.publishOrderBook(orderBook, 5);
                }
            }
        } catch (Exception e) {
            log.error("Error publishing market data updates", e);
        }
    }
    
    private Map<String, OrderBook> getAllOrderBooks() {
        return Map.of();
    }
}