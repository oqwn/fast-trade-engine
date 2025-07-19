package com.fasttrader.config;

import com.fasttrader.engine.MatchingEngine;
import com.fasttrader.engine.MatchingEngineListener;
import com.fasttrader.model.Order;
import com.fasttrader.model.Trade;
import com.fasttrader.engine.CancelEvent;
import com.fasttrader.repository.TradeRepository;
import com.fasttrader.service.MarketDataService;
import com.fasttrader.websocket.MarketDataWebSocketService;
import com.fasttrader.websocket.OrderWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class TradingEngineConfig {
    
    private final MatchingEngine matchingEngine;
    private final TradeRepository tradeRepository;
    private final MarketDataService marketDataService;
    private final MarketDataWebSocketService marketDataWebSocketService;
    private final OrderWebSocketService orderWebSocketService;
    
    @PostConstruct
    public void initializeMatchingEngine() {
        matchingEngine.addListener(new MatchingEngineListener() {
            @Override
            public void onTrade(Trade trade) {
                log.debug("Trade event: {}", trade);
                tradeRepository.save(trade);
                marketDataService.recordTrade(trade);
                marketDataWebSocketService.publishTrade(trade);
            }
            
            @Override
            public void onOrderPlaced(Order order) {
                log.debug("Order placed: {}", order);
                orderWebSocketService.sendOrderUpdate(order);
            }
            
            @Override
            public void onOrderCancelled(CancelEvent event) {
                log.debug("Order cancelled: {}", event.getOrder().getOrderId());
                orderWebSocketService.sendOrderUpdate(event.getOrder());
                orderWebSocketService.sendOrderExecutionNotification(
                    event.getOrder(), 
                    "Order cancelled: " + event.getReason()
                );
            }
            
            @Override
            public void onOrderModified(Order oldOrder, Order newOrder) {
                log.debug("Order modified: {} -> {}", oldOrder.getOrderId(), newOrder.getOrderId());
                orderWebSocketService.sendOrderUpdate(newOrder);
                orderWebSocketService.sendOrderExecutionNotification(
                    newOrder, 
                    "Order modified successfully"
                );
            }
        });
        
        log.info("Trading engine initialized with WebSocket support");
    }
}