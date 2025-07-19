package com.fasttrader.service;

import com.fasttrader.model.Order;
import com.fasttrader.model.OrderBook;
import com.fasttrader.model.enums.MarketState;
import com.fasttrader.model.enums.OrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {
    
    @Value("${app.trading.price-limit-percentage:10}")
    private BigDecimal priceLimitPercentage;
    
    @Value("${app.trading.circuit-breaker.level1-percentage:5}")
    private BigDecimal circuitBreakerLevel1;
    
    @Value("${app.trading.circuit-breaker.level2-percentage:7}")
    private BigDecimal circuitBreakerLevel2;
    
    private final MarketDataService marketDataService;
    
    public void validateOrder(Order order) {
        validateBasicOrderFields(order);
        validateOrderQuantity(order);
        validateOrderPrice(order);
        validateMarketState(order);
        validatePriceLimits(order);
    }
    
    private void validateBasicOrderFields(Order order) {
        if (order.getSymbol() == null || order.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Order symbol is required");
        }
        
        if (order.getAccountId() == null || order.getAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        
        if (order.getSide() == null) {
            throw new IllegalArgumentException("Order side is required");
        }
        
        if (order.getType() == null) {
            throw new IllegalArgumentException("Order type is required");
        }
    }
    
    private void validateOrderQuantity(Order order) {
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive");
        }
        
        if (order.getQuantity() % 100 != 0) {
            throw new IllegalArgumentException("Order quantity must be in board lots (multiples of 100)");
        }
        
        if (order.getQuantity() > 1000000) {
            throw new IllegalArgumentException("Order quantity exceeds maximum limit of 1,000,000");
        }
    }
    
    private void validateOrderPrice(Order order) {
        if (order.getType() == OrderType.MARKET) {
            return;
        }
        
        if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order price must be positive for limit orders");
        }
        
        BigDecimal tickSize = getTickSize(order.getPrice());
        BigDecimal remainder = order.getPrice().remainder(tickSize);
        
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                String.format("Order price must be in multiples of tick size %.4f", tickSize));
        }
    }
    
    private void validateMarketState(Order order) {
        OrderBook orderBook = marketDataService.getOrderBook(order.getSymbol());
        MarketState state = orderBook.getMarketState();
        
        switch (state) {
            case CLOSED:
                throw new IllegalStateException("Market is closed");
            
            case HALTED:
                throw new IllegalStateException("Trading is halted for " + order.getSymbol());
            
            case CIRCUIT_BREAKER_L1:
            case CIRCUIT_BREAKER_L2:
                if (order.getType() == OrderType.MARKET) {
                    throw new IllegalStateException("Market orders not allowed during circuit breaker");
                }
                break;
            
            case PRE_OPEN:
            case POST_CLOSE:
                if (order.getType() == OrderType.MARKET) {
                    throw new IllegalStateException("Market orders not allowed during pre/post market");
                }
                break;
        }
    }
    
    private void validatePriceLimits(Order order) {
        if (order.getType() == OrderType.MARKET) {
            return;
        }
        
        OrderBook orderBook = marketDataService.getOrderBook(order.getSymbol());
        BigDecimal previousClose = orderBook.getPreviousClose();
        
        if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("No previous close price for {}, skipping price limit validation", order.getSymbol());
            return;
        }
        
        BigDecimal upperLimit = previousClose.multiply(
            BigDecimal.ONE.add(priceLimitPercentage.divide(BigDecimal.valueOf(100))));
        BigDecimal lowerLimit = previousClose.multiply(
            BigDecimal.ONE.subtract(priceLimitPercentage.divide(BigDecimal.valueOf(100))));
        
        if (order.getPrice().compareTo(upperLimit) > 0) {
            throw new IllegalArgumentException(
                String.format("Order price %.2f exceeds upper limit %.2f", 
                    order.getPrice(), upperLimit));
        }
        
        if (order.getPrice().compareTo(lowerLimit) < 0) {
            throw new IllegalArgumentException(
                String.format("Order price %.2f below lower limit %.2f", 
                    order.getPrice(), lowerLimit));
        }
    }
    
    private BigDecimal getTickSize(BigDecimal price) {
        if (price.compareTo(new BigDecimal("0.01")) < 0) {
            return new BigDecimal("0.0001");
        } else if (price.compareTo(new BigDecimal("0.10")) < 0) {
            return new BigDecimal("0.001");
        } else if (price.compareTo(new BigDecimal("1.00")) < 0) {
            return new BigDecimal("0.01");
        } else if (price.compareTo(new BigDecimal("10.00")) < 0) {
            return new BigDecimal("0.01");
        } else if (price.compareTo(new BigDecimal("100.00")) < 0) {
            return new BigDecimal("0.05");
        } else if (price.compareTo(new BigDecimal("1000.00")) < 0) {
            return new BigDecimal("0.10");
        } else {
            return new BigDecimal("1.00");
        }
    }
    
    public boolean checkCircuitBreaker(String symbol, BigDecimal currentPrice) {
        OrderBook orderBook = marketDataService.getOrderBook(symbol);
        BigDecimal previousClose = orderBook.getPreviousClose();
        
        if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        BigDecimal change = currentPrice.subtract(previousClose);
        BigDecimal changePercent = change.divide(previousClose, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100)).abs();
        
        if (changePercent.compareTo(circuitBreakerLevel2) >= 0) {
            log.warn("Level 2 circuit breaker triggered for {} at {}%", symbol, changePercent);
            orderBook.updateMarketState(MarketState.CIRCUIT_BREAKER_L2);
            return true;
        } else if (changePercent.compareTo(circuitBreakerLevel1) >= 0) {
            log.warn("Level 1 circuit breaker triggered for {} at {}%", symbol, changePercent);
            orderBook.updateMarketState(MarketState.CIRCUIT_BREAKER_L1);
            return true;
        }
        
        return false;
    }
}