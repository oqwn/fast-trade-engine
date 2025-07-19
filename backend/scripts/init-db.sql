-- Fast Trade Engine Database Initialization

-- Create additional schemas if needed
CREATE SCHEMA IF NOT EXISTS trading;
CREATE SCHEMA IF NOT EXISTS reporting;

-- Create indexes for performance
-- These will be applied when using PostgreSQL

-- Orders table indexes (will be created by JPA)
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_account_id ON orders(account_id);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_symbol ON orders(symbol);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_status ON orders(status);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_created_at ON orders(created_at);

-- Trades table indexes
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_trades_symbol ON trades(symbol);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_trades_execution_time ON trades(execution_time);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_trades_buy_account ON trades(buy_account_id);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_trades_sell_account ON trades(sell_account_id);

-- Accounts table indexes
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_active ON accounts(active);

-- Create partitions for trades table by date (if needed for high volume)
-- This is for future optimization when handling millions of trades
/*
CREATE TABLE trades_y2024m01 PARTITION OF trades
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE trades_y2024m02 PARTITION OF trades
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
*/

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA trading TO fasttrader;
GRANT ALL PRIVILEGES ON SCHEMA reporting TO fasttrader;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO fasttrader;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO fasttrader;

-- Insert default configuration data
-- This could include market hours, trading calendars, etc.
INSERT INTO trading.market_config (key, value) VALUES 
    ('market_open', '09:30:00'),
    ('market_close', '16:00:00'),
    ('price_limit_pct', '10.0'),
    ('circuit_breaker_l1', '5.0'),
    ('circuit_breaker_l2', '7.0')
ON CONFLICT (key) DO NOTHING;