package com.fasttrader.service;

import com.fasttrader.engine.MatchResult;
import com.fasttrader.engine.MatchingEngine;
import com.fasttrader.model.Order;
import com.fasttrader.model.OrderBook;
import com.fasttrader.model.enums.OrderStatus;
import com.fasttrader.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final MatchingEngine matchingEngine;
    private final OrderRepository orderRepository;
    private final AccountService accountService;
    private final ValidationService validationService;
    
    public MatchResult placeOrder(Order order) {
        log.info("Placing order: {}", order);
        
        validationService.validateOrder(order);
        
        accountService.validateAndFreezeForOrder(order);
        
        try {
            MatchResult result = matchingEngine.processOrder(order);
            
            orderRepository.save(result.getFinalOrder());
            
            result.getTrades().forEach(trade -> {
                accountService.processTrade(trade);
            });
            
            if (result.getFinalOrder().isTerminal()) {
                accountService.unfreezeRemainingFunds(result.getFinalOrder());
            }
            
            log.info("Order placed successfully. Result: {}", result);
            return result;
            
        } catch (Exception e) {
            accountService.unfreezeForOrder(order);
            log.error("Failed to place order: {}", order, e);
            throw new RuntimeException("Failed to place order", e);
        }
    }
    
    public Order cancelOrder(String orderId) {
        log.info("Cancelling order: {}", orderId);
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        
        Order order = orderOpt.get();
        if (!order.isActive()) {
            throw new IllegalStateException("Cannot cancel inactive order: " + orderId);
        }
        
        Order cancelledOrder = matchingEngine.cancelOrder(order.getSymbol(), orderId);
        if (cancelledOrder != null) {
            orderRepository.save(cancelledOrder);
            accountService.unfreezeForOrder(cancelledOrder);
        }
        
        return cancelledOrder;
    }
    
    public Order modifyOrder(String orderId, BigDecimal newPrice, Long newQuantity) {
        log.info("Modifying order: {} with price: {} and quantity: {}", 
            orderId, newPrice, newQuantity);
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        
        Order order = orderOpt.get();
        if (!order.isActive()) {
            throw new IllegalStateException("Cannot modify inactive order: " + orderId);
        }
        
        Order modifiedOrder = matchingEngine.modifyOrder(
            order.getSymbol(), orderId, newPrice, newQuantity);
        
        if (modifiedOrder != null) {
            orderRepository.save(modifiedOrder);
        }
        
        return modifiedOrder;
    }
    
    public Optional<Order> getOrder(String orderId) {
        return orderRepository.findById(orderId);
    }
    
    public List<Order> getOrdersByAccount(String accountId) {
        return orderRepository.findByAccountId(accountId);
    }
    
    public List<Order> getActiveOrdersByAccount(String accountId) {
        return orderRepository.findByAccountIdAndStatusIn(accountId, 
            List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED));
    }
    
    public List<Order> getOrdersBySymbol(String symbol) {
        return orderRepository.findBySymbol(symbol);
    }
    
    public OrderBook getOrderBook(String symbol) {
        return matchingEngine.getOrderBook(symbol);
    }
    
    public void cancelAllOrdersByAccount(String accountId) {
        List<Order> activeOrders = getActiveOrdersByAccount(accountId);
        activeOrders.forEach(order -> {
            try {
                cancelOrder(order.getOrderId());
            } catch (Exception e) {
                log.error("Failed to cancel order: {}", order.getOrderId(), e);
            }
        });
    }
}