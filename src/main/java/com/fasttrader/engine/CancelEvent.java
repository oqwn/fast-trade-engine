package com.fasttrader.engine;

import com.fasttrader.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelEvent {
    
    private Order order;
    private Instant timestamp;
    private String reason;
    
    public CancelEvent(Order order) {
        this.order = order;
        this.timestamp = Instant.now();
        this.reason = "User requested";
    }
    
    public CancelEvent(Order order, String reason) {
        this.order = order;
        this.timestamp = Instant.now();
        this.reason = reason;
    }
}