package com.fasttrader.controller;

import com.fasttrader.controller.dto.OrderRequest;
import com.fasttrader.controller.dto.OrderResponse;
import com.fasttrader.controller.dto.OrderBookResponse;
import com.fasttrader.controller.dto.ModifyOrderRequest;
import com.fasttrader.engine.MatchResult;
import com.fasttrader.model.Order;
import com.fasttrader.model.OrderBook;
import com.fasttrader.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Received order request: {}", request);
        
        Order order = request.toOrder();
        MatchResult result = orderService.placeOrder(order);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(OrderResponse.from(result));
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId)
            .map(OrderResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String status) {
        
        List<Order> orders;
        
        if (accountId != null) {
            orders = orderService.getOrdersByAccount(accountId);
        } else if (symbol != null) {
            orders = orderService.getOrdersBySymbol(symbol);
        } else {
            // Return all orders if no specific filter is provided
            orders = orderService.getAllOrders();
        }
        
        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            orders = orders.stream()
                .filter(order -> order.getStatus().name().equals(status.toUpperCase()))
                .collect(Collectors.toList());
        }
        
        List<OrderResponse> response = orders.stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId) {
        log.info("Cancelling order: {}", orderId);
        
        Order cancelledOrder = orderService.cancelOrder(orderId);
        if (cancelledOrder != null) {
            return ResponseEntity.ok(OrderResponse.from(cancelledOrder));
        }
        
        return ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{orderId}")
    public ResponseEntity<OrderResponse> modifyOrder(
            @PathVariable String orderId,
            @Valid @RequestBody ModifyOrderRequest request) {
        
        log.info("Modifying order: {} with {}", orderId, request);
        
        Order modifiedOrder = orderService.modifyOrder(
            orderId, request.getPrice(), request.getQuantity());
        
        if (modifiedOrder != null) {
            return ResponseEntity.ok(OrderResponse.from(modifiedOrder));
        }
        
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/book/{symbol}")
    public ResponseEntity<OrderBookResponse> getOrderBook(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "10") int depth) {
        
        OrderBook orderBook = orderService.getOrderBook(symbol);
        return ResponseEntity.ok(OrderBookResponse.from(orderBook, depth));
    }
}