# Fast Trade Engine - Database Setup Guide

This guide provides comprehensive instructions for setting up the database infrastructure for the Fast Trade Engine.

## 🗄️ Database Architecture

The Fast Trade Engine uses a multi-database architecture:

- **PostgreSQL**: Primary OLTP database for accounts, orders, trades, positions
- **Redis**: In-memory cache for order books, market data, and real-time updates
- **Optional**: ClickHouse for analytics and time-series data (future enhancement)

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

1. **Start the database services:**
```bash
cd backend/scripts
docker-compose -f docker-compose.db.yml up -d
```

2. **Verify services are running:**
```bash
docker-compose -f docker-compose.db.yml ps
```

3. **Access the admin interfaces:**
- PgAdmin: http://localhost:8080 (admin@fasttrader.com / admin123)
- Redis Commander: http://localhost:8081

### Option 2: Manual Setup with Script

1. **Install PostgreSQL and Redis locally**

2. **Run the initialization script:**
```bash
cd backend/scripts
./run-db-init.sh
```

3. **For custom configuration:**
```bash
# Set environment variables
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=fast_trade_engine
export DB_USER=fasttrader
export DB_PASSWORD=fasttrader123

# Run with options
./run-db-init.sh --force-recreate  # Recreate all tables
./run-db-init.sh --skip-sample-data  # Skip sample data
```

## 📊 Database Schema Overview

### Core Tables

1. **users** - User accounts and authentication
2. **accounts** - Trading accounts with balances
3. **instruments** - Tradeable securities (stocks, ETFs)
4. **orders** - Buy/sell orders with all states
5. **trades** - Executed trade records
6. **positions** - Current positions per account
7. **account_transactions** - All account movements

### Market Data Tables

1. **market_data_ticks** - Real-time price updates (partitioned by month)
2. **market_data_ohlc** - Historical OHLC candles (partitioned by month)
3. **market_sessions** - Trading session definitions

### Audit & Configuration

1. **audit.order_audit_log** - Complete order change history
2. **system_config** - Application configuration parameters
3. **migrations** - Database version tracking

## 🔧 Configuration

### Spring Boot Profiles

The application supports multiple database profiles:

1. **Default (H2)**: `application.yml` - In-memory database for testing
2. **PostgreSQL**: `application-postgres.yml` - Production database
3. **Docker**: `application-docker.yml` - Container deployment

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | Database host |
| `DB_PORT` | 5432 | Database port |
| `DB_NAME` | fast_trade_engine | Database name |
| `DB_USER` | fasttrader | Database username |
| `DB_PASSWORD` | fasttrader123 | Database password |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6379 | Redis port |

### Running with PostgreSQL Profile

```bash
# Set the active profile
export SPRING_PROFILES_ACTIVE=postgres

# Or pass as application argument
java -jar target/fast-trade-engine.jar --spring.profiles.active=postgres
```

## 📈 Sample Data

The initialization includes realistic sample data:

### Users (10 traders)
- alice_trader, bob_investor, charlie_daytrader, diana_quant, etc.
- Each with different trading styles and account sizes

### Instruments (22 symbols)
- Major tech stocks: AAPL, MSFT, GOOGL, AMZN, TSLA, META, NVDA
- Financial stocks: JPM, BAC, WFC, GS
- Healthcare: JNJ, PFE, UNH
- Consumer goods: KO, PEP, WMT
- Energy: XOM, CVX
- ETFs: SPY, QQQ, IWM

### Market Data
- Real-time tick data for all instruments
- 30 days of historical OHLC data
- Realistic bid/ask spreads and volumes

### Trading Activity
- Active orders across different order types
- Historical trades with proper settlements
- Current positions with P&L calculations
- Account transaction history

## 🔍 Database Validation

### Health Checks

1. **Table existence:**
```sql
SELECT tablename FROM pg_tables WHERE schemaname = 'public';
```

2. **Sample data counts:**
```sql
SELECT 
    'users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'instruments', COUNT(*) FROM instruments
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'trades', COUNT(*) FROM trades;
```

3. **Migration status:**
```sql
SELECT version, description, executed_at FROM migrations ORDER BY executed_at;
```

### Performance Validation

1. **Index usage:**
```sql
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read 
FROM pg_stat_user_indexes 
ORDER BY idx_scan DESC;
```

2. **Active connections:**
```sql
SELECT count(*) as active_connections 
FROM pg_stat_activity 
WHERE state = 'active';
```

## 🔧 Administration

### Backup & Restore

1. **Create backup:**
```bash
pg_dump -h localhost -U fasttrader -d fast_trade_engine > backup.sql
```

2. **Restore backup:**
```bash
psql -h localhost -U fasttrader -d fast_trade_engine < backup.sql
```

### Performance Monitoring

1. **Enable query logging:**
```sql
ALTER SYSTEM SET log_statement = 'all';
SELECT pg_reload_conf();
```

2. **Monitor slow queries:**
```sql
SELECT query, mean_exec_time, calls 
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC 
LIMIT 10;
```

### Maintenance

1. **Update table statistics:**
```sql
ANALYZE;
```

2. **Vacuum tables:**
```sql
VACUUM ANALYZE;
```

3. **Check table sizes:**
```sql
SELECT 
    tablename,
    pg_size_pretty(pg_total_relation_size(tablename::text)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(tablename::text) DESC;
```

## 🚨 Troubleshooting

### Common Issues

1. **Connection refused:**
   - Check if PostgreSQL is running: `systemctl status postgresql`
   - Verify connection parameters
   - Check firewall settings

2. **Permission denied:**
   - Ensure user has proper permissions
   - Check pg_hba.conf authentication settings

3. **Out of connections:**
   - Monitor connection pool settings
   - Adjust max_connections in postgresql.conf

4. **Slow queries:**
   - Enable query logging
   - Check for missing indexes
   - Analyze query execution plans

### Log Files

- **PostgreSQL logs**: `/var/log/postgresql/`
- **Application logs**: `logs/fast-trade-engine.log`
- **Docker logs**: `docker-compose logs postgres`

## 🔄 Migration System

The database includes a built-in migration tracking system:

1. **migrations** table tracks all applied changes
2. Each script includes a version and checksum
3. Safe to re-run initialization scripts
4. Supports incremental updates

### Adding New Migrations

1. Create new migration file: `003-new-feature.sql`
2. Include migration record:
```sql
INSERT INTO migrations (version, description, checksum) VALUES 
('003', 'Add new feature tables', 'checksum_003');
```

## 🔗 Integration Points

### Backend Integration
- Spring Data JPA entities map to database tables
- Connection pooling via HikariCP
- Transaction management with @Transactional
- Query optimization with custom repositories

### Frontend Integration
- REST API endpoints for all operations
- WebSocket connections for real-time updates
- Proper error handling and validation
- Pagination for large datasets

## 📚 Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)