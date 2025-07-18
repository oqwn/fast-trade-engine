# Fast Trade Engine Architecture

## Overview

The Fast Trade Engine is a high-performance, Java-based electronic trading system designed to simulate real-world stock exchange functionality. It provides order matching, market data generation, risk management, and real-time trading capabilities.

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Client Layer                               │
├─────────────────────┬───────────────────┬───────────────────────────┤
│    Web UI          │   REST API Client  │   WebSocket Client        │
└─────────────────────┴───────────────────┴───────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────────────┐
│                          API Gateway                                 │
├─────────────────────┬───────────────────┬───────────────────────────┤
│    REST Controller  │ WebSocket Handler  │   Authentication         │
└─────────────────────┴───────────────────┴───────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────────────┐
│                      Business Logic Layer                            │
├──────────────┬──────────────┬──────────────┬───────────────────────┤
│Order Service │Market Service│Account Service│ Risk Management      │
└──────────────┴──────────────┴──────────────┴───────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────────────┐
│                      Core Engine Layer                               │
├─────────────────────┬───────────────────┬───────────────────────────┤
│  Matching Engine    │  Market Data Engine│   Settlement Engine      │
└─────────────────────┴───────────────────┴───────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────────────┐
│                       Data Layer                                     │
├─────────────────────┬───────────────────┬───────────────────────────┤
│   Order Book Store  │   Trade Store      │   Account Store          │
└─────────────────────┴───────────────────┴───────────────────────────┘
```

## Core Components

### 1. Matching Engine
The heart of the trading system, responsible for order matching and execution.

**Key Classes:**
- `MatchingEngine`: Core matching logic implementation
- `OrderBook`: Maintains buy/sell orders for each symbol
- `PriceTimePriorityMatcher`: Implements matching algorithm
- `OrderQueue`: Priority queue for orders at same price level

**Design Patterns:**
- Strategy Pattern for different matching algorithms
- Observer Pattern for trade notifications
- Command Pattern for order operations

### 2. Order Management System

**Key Classes:**
- `Order`: Base order class
- `LimitOrder`, `MarketOrder`, `StopOrder`: Order type implementations
- `OrderValidator`: Validates orders before processing
- `OrderRepository`: In-memory order storage with indexing

**Features:**
- Order lifecycle management (NEW → PARTIALLY_FILLED → FILLED/CANCELLED)
- Order ID generation using UUID
- Timestamp precision to nanoseconds for HFT scenarios

### 3. Market Data Engine

**Key Classes:**
- `MarketDataGenerator`: Generates real-time market data
- `Level1Quote`: Best bid/ask prices
- `Level2Data`: Full market depth
- `TickData`: Individual trade ticks
- `OHLCVAggregator`: Candlestick data generation

**Data Flow:**
```
Trade Event → Market Data Engine → Data Aggregators → WebSocket Publisher
                                                    ↓
                                                REST API Cache
```

### 4. Account Management

**Key Classes:**
- `Account`: User account with balance and positions
- `Position`: Stock positions with P&L tracking
- `TransactionManager`: Handles fund freezing/unfreezing
- `SettlementEngine`: T+1 settlement implementation

**State Management:**
- Available Balance vs Frozen Balance
- Real-time P&L calculation
- Position limits enforcement

### 5. Risk Management

**Key Components:**
- Pre-trade risk checks (balance, position limits)
- Price band validation (±10% daily limit)
- Circuit breaker implementation (5%, 7% triggers)
- Order size limits

### 6. API Layer

**REST Endpoints:**
```
POST   /api/orders              - Place new order
DELETE /api/orders/{orderId}    - Cancel order
GET    /api/orders              - Query orders
GET    /api/orderbook/{symbol}  - Get order book snapshot
GET    /api/trades              - Query executed trades
GET    /api/account/balance     - Get account balance
GET    /api/account/positions   - Get positions
```

**WebSocket Channels:**
```
/ws/orders      - Order status updates
/ws/trades      - Real-time trade feed
/ws/marketdata  - Market data stream
/ws/orderbook   - Order book updates
```

## Technology Stack

### Core Technologies
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Database**: H2 (in-memory) for development, PostgreSQL for production
- **Caching**: Caffeine for local caching
- **Messaging**: Disruptor for low-latency event processing

### Libraries
- **REST API**: Spring Web MVC
- **WebSocket**: Spring WebSocket with STOMP
- **JSON Processing**: Jackson
- **Validation**: Jakarta Bean Validation
- **Testing**: JUnit 5, Mockito, RestAssured
- **Logging**: SLF4J with Logback
- **Metrics**: Micrometer with Prometheus

## Data Models

### Order Model
```java
public class Order {
    private String orderId;
    private String accountId;
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private Long quantity;
    private Long filledQuantity;
    private OrderStatus status;
    private Long timestamp;
    private Long sequenceNumber;
}
```

### Trade Model
```java
public class Trade {
    private String tradeId;
    private String symbol;
    private String buyOrderId;
    private String sellOrderId;
    private BigDecimal price;
    private Long quantity;
    private Long timestamp;
    private String buyAccountId;
    private String sellAccountId;
}
```

### OrderBook Structure
```java
public class OrderBook {
    private String symbol;
    private TreeMap<BigDecimal, OrderQueue> bidLevels;
    private TreeMap<BigDecimal, OrderQueue> askLevels;
    private MarketState marketState;
    private BigDecimal lastPrice;
    private Long lastUpdateTime;
}
```

## Performance Considerations

### Optimization Strategies

1. **Lock-Free Data Structures**
   - Use of ConcurrentSkipListMap for order books
   - Atomic operations for critical updates
   - Lock striping for symbol-level isolation

2. **Memory Management**
   - Object pooling for Order and Trade objects
   - Pre-allocated buffers for market data
   - Off-heap storage for historical data

3. **Latency Optimization**
   - Disruptor for order processing pipeline
   - Zero-copy techniques for network I/O
   - Mechanical sympathy principles

4. **Throughput Targets**
   - 100,000+ orders per second
   - Sub-millisecond matching latency
   - 1 million concurrent WebSocket connections

### Benchmarking Approach
```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MatchingEngineBenchmark {
    @Benchmark
    public void benchmarkOrderMatching(BenchmarkState state) {
        // Benchmark implementation
    }
}
```

## Deployment Architecture

### Container Structure
```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   API Gateway   │  │ Matching Engine │  │  Market Data    │
│   (Port 8080)   │  │   (Port 8081)   │  │  (Port 8082)    │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │                      │
         └────────────────────┴──────────────────────┘
                              │
                    ┌─────────────────┐
                    │   PostgreSQL    │
                    │   (Port 5432)   │
                    └─────────────────┘
```

### Scaling Strategy
- Horizontal scaling for API Gateway
- Vertical scaling for Matching Engine (single instance per symbol)
- Partitioning by symbol for multi-instance deployment
- Read replicas for market data queries

## Security Architecture

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (TRADER, MARKET_MAKER, ADMIN)
- API key management for algorithmic trading

### Data Protection
- TLS 1.3 for all external communications
- Encryption at rest for sensitive data
- Audit logging for all trading activities

## Monitoring & Observability

### Metrics
- Order processing latency (p50, p95, p99)
- Throughput (orders/second, trades/second)
- Market data generation rate
- System resource utilization

### Logging Strategy
- Structured logging with correlation IDs
- Separate logs for orders, trades, and system events
- Log aggregation with ELK stack

### Health Checks
```
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

## Testing Strategy

### Unit Tests
- 90%+ code coverage target
- Parameterized tests for edge cases
- Property-based testing for matching engine

### Integration Tests
- REST API contract tests
- WebSocket connection tests
- End-to-end order flow tests

### Performance Tests
- Load testing with JMeter
- Stress testing for circuit breakers
- Latency profiling with JMH

## Future Enhancements

### Phase 1 Extensions
- FIX protocol support
- Multi-currency support
- Options and futures trading

### Phase 2 Features
- Machine learning for anomaly detection
- Smart order routing
- Dark pool implementation
- Blockchain integration for settlement

## Development Guidelines

### Code Organization
```
fast-trade-engine/
├── src/main/java/com/fasttrader/
│   ├── engine/          # Core matching engine
│   ├── model/           # Domain models
│   ├── service/         # Business services
│   ├── controller/      # REST controllers
│   ├── websocket/       # WebSocket handlers
│   ├── repository/      # Data access layer
│   ├── config/          # Configuration classes
│   └── util/            # Utility classes
├── src/test/java/       # Test classes
├── src/main/resources/  # Configuration files
└── pom.xml             # Maven configuration
```

### Coding Standards
- Google Java Style Guide
- Immutable objects where possible
- Builder pattern for complex objects
- Comprehensive JavaDoc for public APIs
- Meaningful variable and method names

### Git Workflow
- Feature branches from develop
- Pull requests with code review
- Semantic versioning
- Automated CI/CD pipeline

## Conclusion

This architecture provides a solid foundation for building a high-performance trading engine in Java. The modular design allows for incremental development while maintaining system integrity and performance. The use of modern Java features and proven design patterns ensures maintainability and scalability as the system grows.