package com.fasttrader.engine;

import com.fasttrader.model.Order;
import com.fasttrader.model.Trade;

public interface MatchingEngineListener {
    
    void onTrade(Trade trade);
    
    void onOrderPlaced(Order order);
    
    void onOrderCancelled(CancelEvent event);
    
    void onOrderModified(Order oldOrder, Order newOrder);
}