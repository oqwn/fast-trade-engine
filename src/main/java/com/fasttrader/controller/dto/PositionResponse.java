package com.fasttrader.controller.dto;

import com.fasttrader.model.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionResponse {
    
    private String symbol;
    private Long quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    private BigDecimal realizedPnL;
    private BigDecimal unrealizedPnL;
    private BigDecimal totalPnL;
    private String side;
    private Instant createdAt;
    private Instant updatedAt;
    
    public static PositionResponse from(Position position, BigDecimal currentPrice) {
        PositionResponse.PositionResponseBuilder builder = PositionResponse.builder()
            .symbol(position.getSymbol())
            .quantity(position.getQuantity())
            .averagePrice(position.getAveragePrice())
            .realizedPnL(position.getRealizedPnL())
            .createdAt(position.getCreatedAt())
            .updatedAt(position.getUpdatedAt());
        
        if (position.isLong()) {
            builder.side("LONG");
        } else if (position.isShort()) {
            builder.side("SHORT");
        } else {
            builder.side("FLAT");
        }
        
        if (currentPrice != null) {
            builder.currentPrice(currentPrice)
                .marketValue(position.getMarketValue(currentPrice))
                .unrealizedPnL(position.getUnrealizedPnL(currentPrice))
                .totalPnL(position.getTotalPnL(currentPrice));
        }
        
        return builder.build();
    }
}