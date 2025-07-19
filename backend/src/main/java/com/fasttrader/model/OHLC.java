package com.fasttrader.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OHLC {
    private String timestamp;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;
}