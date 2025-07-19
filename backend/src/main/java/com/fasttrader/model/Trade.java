package com.fasttrader.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "tradeId")
public class Trade {
    
    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong(0);
    
    @Builder.Default
    private String tradeId = UUID.randomUUID().toString();
    
    private String symbol;
    private String buyOrderId;
    private String sellOrderId;
    private String buyAccountId;
    private String sellAccountId;
    private BigDecimal price;
    private Long quantity;
    
    @Builder.Default
    private Long timestamp = System.nanoTime();
    
    @Builder.Default
    private Long sequenceNumber = SEQUENCE_GENERATOR.incrementAndGet();
    
    @Builder.Default
    private Instant executionTime = Instant.now();
    
    private String aggressorSide;
    private BigDecimal buyCommission;
    private BigDecimal sellCommission;
    
    public static Trade create(Order buyOrder, Order sellOrder, BigDecimal price, Long quantity) {
        String aggressorSide = buyOrder.getTimestamp() > sellOrder.getTimestamp() ? "BUY" : "SELL";
        
        return Trade.builder()
                .symbol(buyOrder.getSymbol())
                .buyOrderId(buyOrder.getOrderId())
                .sellOrderId(sellOrder.getOrderId())
                .buyAccountId(buyOrder.getAccountId())
                .sellAccountId(sellOrder.getAccountId())
                .price(price)
                .quantity(quantity)
                .aggressorSide(aggressorSide)
                .build();
    }
    
    public BigDecimal getValue() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}