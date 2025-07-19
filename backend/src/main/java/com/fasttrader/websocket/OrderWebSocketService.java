package com.fasttrader.websocket;

import com.fasttrader.controller.dto.OrderResponse;
import com.fasttrader.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;
    
    @Async
    public void sendOrderUpdate(Order order) {
        try {
            OrderResponse response = OrderResponse.from(order);
            
            messagingTemplate.convertAndSend(
                "/topic/orders/" + order.getAccountId(), 
                response);
            
            messagingTemplate.convertAndSendToUser(
                order.getAccountId(),
                "/queue/orders",
                response);
            
            log.debug("Sent order update to account {}: {}", 
                order.getAccountId(), order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order update", e);
        }
    }
    
    @Async
    public void sendOrderExecutionNotification(Order order, String message) {
        try {
            OrderNotification notification = OrderNotification.builder()
                .orderId(order.getOrderId())
                .accountId(order.getAccountId())
                .symbol(order.getSymbol())
                .status(order.getStatus().toString())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
            
            messagingTemplate.convertAndSendToUser(
                order.getAccountId(),
                "/queue/notifications",
                notification);
            
            log.debug("Sent execution notification to account {}: {}", 
                order.getAccountId(), message);
        } catch (Exception e) {
            log.error("Failed to send order notification", e);
        }
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OrderNotification {
        private String orderId;
        private String accountId;
        private String symbol;
        private String status;
        private String message;
        private Long timestamp;
    }
}