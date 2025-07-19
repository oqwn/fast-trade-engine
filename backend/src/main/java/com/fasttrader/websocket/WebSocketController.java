package com.fasttrader.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {
    
    @MessageMapping("/subscribe/marketdata/{symbol}")
    @SendTo("/topic/subscription/response")
    public SubscriptionResponse subscribeMarketData(
            @DestinationVariable String symbol,
            Principal principal) {
        
        log.info("User {} subscribed to market data for {}", 
            principal != null ? principal.getName() : "anonymous", symbol);
        
        return SubscriptionResponse.builder()
            .channel("marketdata/" + symbol)
            .status("subscribed")
            .message("Successfully subscribed to market data for " + symbol)
            .build();
    }
    
    @MessageMapping("/subscribe/orderbook/{symbol}")
    @SendTo("/topic/subscription/response")
    public SubscriptionResponse subscribeOrderBook(
            @DestinationVariable String symbol,
            Principal principal) {
        
        log.info("User {} subscribed to order book for {}", 
            principal != null ? principal.getName() : "anonymous", symbol);
        
        return SubscriptionResponse.builder()
            .channel("orderbook/" + symbol)
            .status("subscribed")
            .message("Successfully subscribed to order book for " + symbol)
            .build();
    }
    
    @MessageMapping("/subscribe/trades/{symbol}")
    @SendTo("/topic/subscription/response")
    public SubscriptionResponse subscribeTrades(
            @DestinationVariable String symbol,
            Principal principal) {
        
        log.info("User {} subscribed to trades for {}", 
            principal != null ? principal.getName() : "anonymous", symbol);
        
        return SubscriptionResponse.builder()
            .channel("trades/" + symbol)
            .status("subscribed")
            .message("Successfully subscribed to trades for " + symbol)
            .build();
    }
    
    @SubscribeMapping("/topic/heartbeat")
    public Map<String, Object> handleHeartbeatSubscription() {
        return Map.of(
            "type", "heartbeat",
            "timestamp", System.currentTimeMillis(),
            "status", "connected"
        );
    }
    
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, Object> handlePing(Map<String, Object> message) {
        return Map.of(
            "type", "pong",
            "timestamp", System.currentTimeMillis(),
            "echo", message.get("timestamp")
        );
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SubscriptionResponse {
        private String channel;
        private String status;
        private String message;
        private Long timestamp;
        
        @lombok.Builder.Default
        private Long responseTimestamp = System.currentTimeMillis();
    }
}