package com.fasttrader.model;

import com.fasttrader.model.enums.OrderSide;
import com.fasttrader.model.enums.OrderStatus;
import com.fasttrader.model.enums.OrderType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "orderId")
public class Order implements Comparable<Order> {
    
    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong(0);
    
    @Builder.Default
    private String orderId = UUID.randomUUID().toString();
    
    private String accountId;
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private Long quantity;
    
    @Builder.Default
    private Long filledQuantity = 0L;
    
    @Builder.Default
    private OrderStatus status = OrderStatus.NEW;
    
    @Builder.Default
    private Long timestamp = System.nanoTime();
    
    @Builder.Default
    private Long sequenceNumber = SEQUENCE_GENERATOR.incrementAndGet();
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    private Instant updatedAt;
    
    private BigDecimal stopPrice;
    private Long displayQuantity;
    private Long minQuantity;
    private Instant expireTime;
    
    private String clientOrderId;
    private String text;
    
    public Long getRemainingQuantity() {
        return quantity - filledQuantity;
    }
    
    public boolean isFilled() {
        return filledQuantity.equals(quantity);
    }
    
    public boolean isActive() {
        return status == OrderStatus.NEW || status == OrderStatus.PARTIALLY_FILLED;
    }
    
    public boolean isTerminal() {
        return status == OrderStatus.FILLED || 
               status == OrderStatus.CANCELLED || 
               status == OrderStatus.REJECTED ||
               status == OrderStatus.EXPIRED;
    }
    
    public void fill(Long fillQuantity) {
        this.filledQuantity += fillQuantity;
        if (this.filledQuantity.equals(this.quantity)) {
            this.status = OrderStatus.FILLED;
        } else {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
        this.updatedAt = Instant.now();
    }
    
    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
    
    public void reject(String reason) {
        this.status = OrderStatus.REJECTED;
        this.text = reason;
        this.updatedAt = Instant.now();
    }
    
    @Override
    public int compareTo(Order other) {
        if (this.timestamp.equals(other.timestamp)) {
            return this.sequenceNumber.compareTo(other.sequenceNumber);
        }
        return this.timestamp.compareTo(other.timestamp);
    }
    
    public boolean isPriceCompatible(Order other) {
        if (this.side == other.side) {
            return false;
        }
        
        if (this.type == OrderType.MARKET || other.type == OrderType.MARKET) {
            return true;
        }
        
        if (this.side == OrderSide.BUY) {
            return this.price.compareTo(other.price) >= 0;
        } else {
            return this.price.compareTo(other.price) <= 0;
        }
    }
    
    public BigDecimal getExecutablePrice(Order other) {
        if (this.type == OrderType.MARKET && other.type == OrderType.MARKET) {
            throw new IllegalStateException("Cannot match two market orders");
        }
        
        if (this.type == OrderType.MARKET) {
            return other.price;
        }
        
        if (other.type == OrderType.MARKET) {
            return this.price;
        }
        
        return this.timestamp < other.timestamp ? this.price : other.price;
    }
}