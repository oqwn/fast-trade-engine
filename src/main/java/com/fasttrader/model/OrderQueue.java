package com.fasttrader.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

@Getter
public class OrderQueue {
    
    private final BigDecimal price;
    private final Queue<Order> orders;
    private Long totalQuantity;
    private int orderCount;
    
    public OrderQueue(BigDecimal price) {
        this.price = price;
        this.orders = new LinkedList<>();
        this.totalQuantity = 0L;
        this.orderCount = 0;
    }
    
    public synchronized void addOrder(Order order) {
        if (!order.getPrice().equals(price)) {
            throw new IllegalArgumentException(
                String.format("Order price %s does not match queue price %s", 
                    order.getPrice(), price));
        }
        
        orders.offer(order);
        totalQuantity += order.getRemainingQuantity();
        orderCount++;
    }
    
    public synchronized Order removeOrder(Order order) {
        if (orders.remove(order)) {
            totalQuantity -= order.getRemainingQuantity();
            orderCount--;
            return order;
        }
        return null;
    }
    
    public synchronized Order peekFirst() {
        return orders.peek();
    }
    
    public synchronized Order pollFirst() {
        Order order = orders.poll();
        if (order != null) {
            totalQuantity -= order.getRemainingQuantity();
            orderCount--;
        }
        return order;
    }
    
    public synchronized void updateQuantity(Order order, Long oldRemainingQty) {
        Long newRemainingQty = order.getRemainingQuantity();
        totalQuantity = totalQuantity - oldRemainingQty + newRemainingQty;
    }
    
    public synchronized boolean isEmpty() {
        return orders.isEmpty();
    }
    
    public synchronized int size() {
        return orderCount;
    }
    
    public synchronized Iterator<Order> iterator() {
        return new LinkedList<>(orders).iterator();
    }
    
    public synchronized void removeFilledOrders() {
        Iterator<Order> it = orders.iterator();
        while (it.hasNext()) {
            Order order = it.next();
            if (order.isFilled() || !order.isActive()) {
                it.remove();
                totalQuantity -= order.getRemainingQuantity();
                orderCount--;
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format("OrderQueue{price=%s, orders=%d, quantity=%d}", 
            price, orderCount, totalQuantity);
    }
}