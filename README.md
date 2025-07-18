# Fast Trade Engine

A high-performance electronic trading system built with Java and Spring Boot, designed to simulate real-world stock exchange functionality.

## Features

- **Order Matching Engine**: Price-time priority matching algorithm
- **Order Types**: Market, Limit orders (with extensibility for advanced types)
- **Real-time Updates**: WebSocket support for live market data and order updates
- **REST API**: Comprehensive API for order management and market data
- **Account Management**: Balance tracking, position management, P&L calculation
- **Market Data**: Level 1 & 2 quotes, trade feed, OHLC data
- **Risk Management**: Pre-trade validation, price limits, circuit breakers
- **Market Simulation**: Built-in market maker bots for testing

## Architecture

See [architecture.md](architecture.md) for detailed system design.

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Getting Started

### Build the project

```bash
mvn clean install
```

### Run the application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080/api`

### Access H2 Console

The H2 database console is available at: `http://localhost:8080/api/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

## API Documentation

### Order Management

#### Place Order
```bash
POST /api/orders
Content-Type: application/json

{
  "accountId": "TRADER001",
  "symbol": "AAPL",
  "side": "BUY",
  "type": "LIMIT",
  "price": 150.50,
  "quantity": 100
}
```

#### Cancel Order
```bash
DELETE /api/orders/{orderId}
```

#### Get Order Book
```bash
GET /api/orders/book/{symbol}?depth=10
```

### Market Data

#### Get Quote
```bash
GET /api/market-data/quote/{symbol}?depth=5
```

#### Get Recent Trades
```bash
GET /api/trades/recent/{symbol}?limit=50
```

#### Get OHLC Data
```bash
GET /api/market-data/ohlc/{symbol}?interval=1m&periods=100
```

### Account Management

#### Create Account
```bash
POST /api/accounts
Content-Type: application/json

{
  "accountId": "TRADER001",
  "accountName": "Test Trader",
  "initialBalance": 100000
}
```

#### Get Account Balance
```bash
GET /api/accounts/{accountId}/balance
```

#### Get Positions
```bash
GET /api/accounts/{accountId}/positions
```

## WebSocket Endpoints

Connect to WebSocket at: `ws://localhost:8080/api/ws`

### Subscribe to Market Data
- `/topic/marketdata/{symbol}` - Real-time quotes
- `/topic/orderbook/{symbol}` - Order book updates
- `/topic/trades/{symbol}` - Trade feed
- `/topic/trades` - All trades

### Subscribe to Order Updates
- `/topic/orders/{accountId}` - Order status updates
- `/queue/orders` - Personal order updates (requires authentication)
- `/queue/notifications` - Order notifications

## Market Simulation

The application includes a market simulator that automatically:
- Creates test accounts with initial balance
- Places initial orders to create market depth
- Simulates trading activity with market maker bots
- Generates realistic price movements

The simulation starts automatically when the application launches.

## Configuration

Key configuration properties in `application.yml`:

```yaml
app:
  trading:
    price-limit-percentage: 10
    circuit-breaker:
      level1-percentage: 5
      level2-percentage: 7
```

## Testing

### Run all tests
```bash
mvn test
```

### Run with specific profile
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

## Performance

The system is designed for high throughput and low latency:
- Target: 100,000+ orders per second
- Sub-millisecond matching latency
- Lock-free data structures where possible
- Memory-optimized with object pooling

## Development

### Project Structure
```
src/main/java/com/fasttrader/
├── engine/          # Core matching engine
├── model/           # Domain models
├── service/         # Business logic
├── controller/      # REST API endpoints
├── websocket/       # WebSocket handlers
├── repository/      # Data access layer
├── config/          # Configuration
└── exception/       # Exception handling
```

### Adding New Features

1. **New Order Types**: Extend the `Order` class and update `MatchingEngine`
2. **New Market Data**: Add to `MarketDataService` and create WebSocket publishers
3. **New Validations**: Add to `ValidationService`

## Monitoring

- Health check: `GET /api/actuator/health`
- Metrics: `GET /api/actuator/metrics`
- Prometheus: `GET /api/actuator/prometheus`

## License

This is a demonstration project for educational purposes.