package com.fasttrader.repository;

import com.fasttrader.model.Order;
import com.fasttrader.model.enums.OrderStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class OrderRepository {
    
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    
    public Order save(Order order) {
        orders.put(order.getOrderId(), order);
        return order;
    }
    
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
    
    public List<Order> findAll() {
        return orders.values().stream()
            .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
            .collect(Collectors.toList());
    }
    
    public List<Order> findByAccountId(String accountId) {
        return orders.values().stream()
            .filter(order -> accountId.equals(order.getAccountId()))
            .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
            .collect(Collectors.toList());
    }
    
    public List<Order> findBySymbol(String symbol) {
        return orders.values().stream()
            .filter(order -> symbol.equals(order.getSymbol()))
            .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
            .collect(Collectors.toList());
    }
    
    public List<Order> findByAccountIdAndStatusIn(String accountId, List<OrderStatus> statuses) {
        return orders.values().stream()
            .filter(order -> accountId.equals(order.getAccountId()) && 
                           statuses.contains(order.getStatus()))
            .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
            .collect(Collectors.toList());
    }
    
    public List<Order> findByCreatedAtBetween(Instant start, Instant end) {
        return orders.values().stream()
            .filter(order -> !order.getCreatedAt().isBefore(start) && 
                           !order.getCreatedAt().isAfter(end))
            .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
            .collect(Collectors.toList());
    }
    
    public void deleteById(String orderId) {
        orders.remove(orderId);
    }
    
    public void deleteAll() {
        orders.clear();
    }
    
    public long count() {
        return orders.size();
    }
    
    public boolean existsById(String orderId) {
        return orders.containsKey(orderId);
    }
}