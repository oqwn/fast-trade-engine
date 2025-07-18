package com.fasttrader.controller.dto;

import com.fasttrader.model.Order;
import com.fasttrader.model.enums.OrderSide;
import com.fasttrader.model.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    
    @NotBlank(message = "Account ID is required")
    private String accountId;
    
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    @NotNull(message = "Side is required")
    private OrderSide side;
    
    @NotNull(message = "Type is required")
    private OrderType type;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Long quantity;
    
    private String clientOrderId;
    
    private BigDecimal stopPrice;
    
    private Long displayQuantity;
    
    public Order toOrder() {
        return Order.builder()
            .accountId(accountId)
            .symbol(symbol.toUpperCase())
            .side(side)
            .type(type)
            .price(price)
            .quantity(quantity)
            .clientOrderId(clientOrderId)
            .stopPrice(stopPrice)
            .displayQuantity(displayQuantity)
            .build();
    }
}