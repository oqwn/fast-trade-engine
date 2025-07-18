package com.fasttrader.service;

import com.fasttrader.model.Order;
import com.fasttrader.model.OrderBook;
import com.fasttrader.model.enums.OrderSide;
import com.fasttrader.model.enums.OrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketSimulator {
    
    private final OrderService orderService;
    private final AccountService accountService;
    
    private final Random random = new Random();
    private final AtomicBoolean simulationEnabled = new AtomicBoolean(false);
    
    private static final List<String> SYMBOLS = Arrays.asList("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA");
    private static final List<String> TEST_ACCOUNTS = Arrays.asList("MM001", "MM002", "TRADER001", "TRADER002");
    
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initializeSimulation() {
        log.info("Initializing market simulation...");
        
        try {
            for (String accountId : TEST_ACCOUNTS) {
                accountService.createAccount(accountId, "Test Account " + accountId, 
                    new BigDecimal("1000000.00"));
            }
            
            placeInitialOrders();
            
            simulationEnabled.set(true);
            log.info("Market simulation initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize market simulation", e);
        }
    }
    
    private void placeInitialOrders() {
        for (String symbol : SYMBOLS) {
            BigDecimal basePrice = getInitialPrice(symbol);
            
            for (int i = 1; i <= 5; i++) {
                BigDecimal bidPrice = basePrice.subtract(
                    BigDecimal.valueOf(i * 0.01)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal askPrice = basePrice.add(
                    BigDecimal.valueOf(i * 0.01)).setScale(2, RoundingMode.HALF_UP);
                
                Order buyOrder = Order.builder()
                    .accountId("MM001")
                    .symbol(symbol)
                    .side(OrderSide.BUY)
                    .type(OrderType.LIMIT)
                    .price(bidPrice)
                    .quantity(1000L + random.nextInt(4000))
                    .build();
                
                Order sellOrder = Order.builder()
                    .accountId("MM002")
                    .symbol(symbol)
                    .side(OrderSide.SELL)
                    .type(OrderType.LIMIT)
                    .price(askPrice)
                    .quantity(1000L + random.nextInt(4000))
                    .build();
                
                try {
                    orderService.placeOrder(buyOrder);
                    orderService.placeOrder(sellOrder);
                } catch (Exception e) {
                    log.error("Failed to place initial order", e);
                }
            }
        }
    }
    
    @Scheduled(fixedDelay = 2000)
    public void simulateMarketActivity() {
        if (!simulationEnabled.get()) {
            return;
        }
        
        try {
            String symbol = SYMBOLS.get(random.nextInt(SYMBOLS.size()));
            String accountId = TEST_ACCOUNTS.get(random.nextInt(TEST_ACCOUNTS.size()));
            
            if (random.nextDouble() < 0.7) {
                placeRandomOrder(symbol, accountId);
            } else if (random.nextDouble() < 0.9) {
                modifyRandomOrder(accountId);
            } else {
                cancelRandomOrder(accountId);
            }
            
        } catch (Exception e) {
            log.error("Error in market simulation", e);
        }
    }
    
    private void placeRandomOrder(String symbol, String accountId) {
        try {
            OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;
            OrderType type = random.nextDouble() < 0.8 ? OrderType.LIMIT : OrderType.MARKET;
            
            BigDecimal currentPrice = getCurrentPrice(symbol);
            BigDecimal orderPrice = null;
            
            if (type == OrderType.LIMIT) {
                double priceVariation = (random.nextDouble() - 0.5) * 0.02;
                orderPrice = currentPrice.multiply(BigDecimal.valueOf(1 + priceVariation))
                    .setScale(2, RoundingMode.HALF_UP);
            }
            
            Long quantity = (long) ((random.nextInt(50) + 1) * 100);
            
            Order order = Order.builder()
                .accountId(accountId)
                .symbol(symbol)
                .side(side)
                .type(type)
                .price(orderPrice)
                .quantity(quantity)
                .build();
            
            orderService.placeOrder(order);
            log.debug("Placed simulated order: {} {} {} @ {}", 
                side, quantity, symbol, orderPrice);
            
        } catch (Exception e) {
            log.debug("Failed to place simulated order: {}", e.getMessage());
        }
    }
    
    private void modifyRandomOrder(String accountId) {
        try {
            List<Order> activeOrders = orderService.getActiveOrdersByAccount(accountId);
            if (!activeOrders.isEmpty()) {
                Order order = activeOrders.get(random.nextInt(activeOrders.size()));
                
                BigDecimal newPrice = order.getPrice().multiply(
                    BigDecimal.valueOf(1 + (random.nextDouble() - 0.5) * 0.01))
                    .setScale(2, RoundingMode.HALF_UP);
                
                orderService.modifyOrder(order.getOrderId(), newPrice, null);
                log.debug("Modified simulated order: {} to price {}", 
                    order.getOrderId(), newPrice);
            }
        } catch (Exception e) {
            log.debug("Failed to modify simulated order: {}", e.getMessage());
        }
    }
    
    private void cancelRandomOrder(String accountId) {
        try {
            List<Order> activeOrders = orderService.getActiveOrdersByAccount(accountId);
            if (!activeOrders.isEmpty()) {
                Order order = activeOrders.get(random.nextInt(activeOrders.size()));
                orderService.cancelOrder(order.getOrderId());
                log.debug("Cancelled simulated order: {}", order.getOrderId());
            }
        } catch (Exception e) {
            log.debug("Failed to cancel simulated order: {}", e.getMessage());
        }
    }
    
    private BigDecimal getInitialPrice(String symbol) {
        switch (symbol) {
            case "AAPL":
                return new BigDecimal("150.00");
            case "GOOGL":
                return new BigDecimal("2800.00");
            case "MSFT":
                return new BigDecimal("300.00");
            case "AMZN":
                return new BigDecimal("3300.00");
            case "TSLA":
                return new BigDecimal("900.00");
            default:
                return new BigDecimal("100.00");
        }
    }
    
    private BigDecimal getCurrentPrice(String symbol) {
        OrderBook orderBook = orderService.getOrderBook(symbol);
        
        if (orderBook.getLastPrice() != null) {
            return orderBook.getLastPrice();
        } else if (orderBook.getMidPrice() != null) {
            return orderBook.getMidPrice();
        } else {
            return getInitialPrice(symbol);
        }
    }
    
    public void enableSimulation(boolean enable) {
        simulationEnabled.set(enable);
        log.info("Market simulation {}", enable ? "enabled" : "disabled");
    }
}