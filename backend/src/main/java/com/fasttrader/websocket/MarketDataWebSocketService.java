package com.fasttrader.websocket;

import com.fasttrader.controller.dto.MarketDataResponse;
import com.fasttrader.controller.dto.OrderBookResponse;
import com.fasttrader.controller.dto.TradeResponse;
import com.fasttrader.model.MarketData;
import com.fasttrader.model.OrderBook;
import com.fasttrader.model.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataWebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @Async
    public void publishTrade(Trade trade) {
        try {
            TradeResponse tradeResponse = TradeResponse.from(trade);
            
            messagingTemplate.convertAndSend(
                "/topic/trades/" + trade.getSymbol(), 
                tradeResponse);
            
            messagingTemplate.convertAndSend(
                "/topic/trades", 
                tradeResponse);
            
            log.debug("Published trade to WebSocket: {}", trade.getTradeId());
        } catch (Exception e) {
            log.error("Failed to publish trade to WebSocket", e);
        }
    }
    
    @Async
    public void publishMarketData(MarketData marketData) {
        try {
            MarketDataResponse response = MarketDataResponse.from(marketData);
            
            messagingTemplate.convertAndSend(
                "/topic/marketdata/" + marketData.getSymbol(), 
                response);
            
            log.debug("Published market data to WebSocket: {}", marketData.getSymbol());
        } catch (Exception e) {
            log.error("Failed to publish market data to WebSocket", e);
        }
    }
    
    @Async
    public void publishOrderBook(OrderBook orderBook, int depth) {
        try {
            OrderBookResponse response = OrderBookResponse.from(orderBook, depth);
            
            messagingTemplate.convertAndSend(
                "/topic/orderbook/" + orderBook.getSymbol(), 
                response);
            
            log.debug("Published order book to WebSocket: {}", orderBook.getSymbol());
        } catch (Exception e) {
            log.error("Failed to publish order book to WebSocket", e);
        }
    }
    
    @Async
    public void publishOrderBookUpdate(String symbol, String updateType, Object update) {
        try {
            OrderBookUpdate bookUpdate = OrderBookUpdate.builder()
                .symbol(symbol)
                .updateType(updateType)
                .data(update)
                .timestamp(System.currentTimeMillis())
                .build();
            
            messagingTemplate.convertAndSend(
                "/topic/orderbook/updates/" + symbol, 
                bookUpdate);
            
            log.debug("Published order book update to WebSocket: {} - {}", symbol, updateType);
        } catch (Exception e) {
            log.error("Failed to publish order book update to WebSocket", e);
        }
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OrderBookUpdate {
        private String symbol;
        private String updateType;
        private Object data;
        private Long timestamp;
    }
}