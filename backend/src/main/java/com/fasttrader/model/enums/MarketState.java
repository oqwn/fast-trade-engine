package com.fasttrader.model.enums;

public enum MarketState {
    PRE_OPEN,
    OPENING_AUCTION,
    CONTINUOUS_TRADING,
    CLOSING_AUCTION,
    POST_CLOSE,
    HALTED,
    CIRCUIT_BREAKER_L1,
    CIRCUIT_BREAKER_L2,
    CLOSED
}