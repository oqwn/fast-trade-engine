-- =====================================================
-- Fast Trade Engine - Complete Database Initialization
-- =====================================================
-- This script sets up the entire database schema, indexes, 
-- and sample data for development and testing.

-- Drop existing tables if they exist (for clean re-initialization)
DROP TABLE IF EXISTS trade_executions CASCADE;
DROP TABLE IF EXISTS trades CASCADE;
DROP TABLE IF EXISTS order_audit_log CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS positions CASCADE;
DROP TABLE IF EXISTS account_transactions CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS instruments CASCADE;
DROP TABLE IF EXISTS market_data_ticks CASCADE;
DROP TABLE IF EXISTS market_data_ohlc CASCADE;
DROP TABLE IF EXISTS market_sessions CASCADE;
DROP TABLE IF EXISTS system_config CASCADE;
DROP TABLE IF EXISTS migrations CASCADE;

-- Drop schemas
DROP SCHEMA IF EXISTS trading CASCADE;
DROP SCHEMA IF EXISTS reporting CASCADE;
DROP SCHEMA IF EXISTS audit CASCADE;

-- Create schemas
CREATE SCHEMA IF NOT EXISTS trading;
CREATE SCHEMA IF NOT EXISTS reporting;
CREATE SCHEMA IF NOT EXISTS audit;

-- =====================================================
-- Migration tracking table
-- =====================================================
CREATE TABLE migrations (
    id SERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    checksum VARCHAR(64)
);

-- =====================================================
-- Core Business Tables
-- =====================================================

-- Users table
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

-- Accounts table
CREATE TABLE accounts (
    account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) DEFAULT 'INDIVIDUAL',
    balance DECIMAL(20,8) NOT NULL DEFAULT 0,
    available_balance DECIMAL(20,8) NOT NULL DEFAULT 0,
    frozen_balance DECIMAL(20,8) NOT NULL DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    risk_limit DECIMAL(20,8) DEFAULT 1000000,
    daily_loss_limit DECIMAL(20,8) DEFAULT 10000
);

-- Instruments table
CREATE TABLE instruments (
    symbol VARCHAR(10) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    exchange VARCHAR(10) NOT NULL,
    sector VARCHAR(50),
    industry VARCHAR(100),
    market_cap DECIMAL(20,2),
    tick_size DECIMAL(10,4) DEFAULT 0.01,
    lot_size INTEGER DEFAULT 1,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(account_id),
    client_order_id VARCHAR(50),
    symbol VARCHAR(10) NOT NULL REFERENCES instruments(symbol),
    side VARCHAR(4) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    type VARCHAR(20) NOT NULL CHECK (type IN ('MARKET', 'LIMIT', 'STOP', 'STOP_LIMIT')),
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    price DECIMAL(20,8),
    stop_price DECIMAL(20,8),
    filled_quantity BIGINT DEFAULT 0 CHECK (filled_quantity >= 0),
    remaining_quantity BIGINT GENERATED ALWAYS AS (quantity - filled_quantity) STORED,
    average_fill_price DECIMAL(20,8),
    status VARCHAR(20) DEFAULT 'NEW' CHECK (status IN ('NEW', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')),
    time_in_force VARCHAR(10) DEFAULT 'DAY' CHECK (time_in_force IN ('DAY', 'GTC', 'IOC', 'FOK')),
    display_quantity BIGINT,
    min_quantity BIGINT,
    expire_time TIMESTAMP,
    text TEXT,
    sequence_number BIGSERIAL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    filled_at TIMESTAMP,
    cancelled_at TIMESTAMP
);

-- Trades table
CREATE TABLE trades (
    trade_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol VARCHAR(10) NOT NULL REFERENCES instruments(symbol),
    buy_order_id UUID NOT NULL REFERENCES orders(order_id),
    sell_order_id UUID NOT NULL REFERENCES orders(order_id),
    buy_account_id UUID NOT NULL REFERENCES accounts(account_id),
    sell_account_id UUID NOT NULL REFERENCES accounts(account_id),
    price DECIMAL(20,8) NOT NULL,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    value DECIMAL(20,8) GENERATED ALWAYS AS (price * quantity) STORED,
    aggressor_side VARCHAR(4) NOT NULL CHECK (aggressor_side IN ('BUY', 'SELL')),
    execution_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    settlement_date DATE,
    commission_buy DECIMAL(10,4) DEFAULT 0,
    commission_sell DECIMAL(10,4) DEFAULT 0,
    sequence_number BIGSERIAL
);

-- Positions table
CREATE TABLE positions (
    position_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(account_id),
    symbol VARCHAR(10) NOT NULL REFERENCES instruments(symbol),
    quantity BIGINT NOT NULL DEFAULT 0,
    average_price DECIMAL(20,8),
    realized_pnl DECIMAL(20,8) DEFAULT 0,
    unrealized_pnl DECIMAL(20,8) DEFAULT 0,
    total_pnl DECIMAL(20,8) GENERATED ALWAYS AS (realized_pnl + unrealized_pnl) STORED,
    market_value DECIMAL(20,8),
    last_update_price DECIMAL(20,8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(account_id, symbol)
);

-- Account transactions table
CREATE TABLE account_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(account_id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRADE_SETTLEMENT', 'COMMISSION', 'DIVIDEND', 'INTEREST', 'FEE')),
    amount DECIMAL(20,8) NOT NULL,
    balance_after DECIMAL(20,8) NOT NULL,
    reference_id UUID,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Market Data Tables
-- =====================================================

-- Market data ticks (real-time)
CREATE TABLE market_data_ticks (
    tick_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol VARCHAR(10) NOT NULL REFERENCES instruments(symbol),
    timestamp TIMESTAMP NOT NULL,
    last_price DECIMAL(20,8),
    bid_price DECIMAL(20,8),
    ask_price DECIMAL(20,8),
    bid_size BIGINT,
    ask_size BIGINT,
    volume BIGINT DEFAULT 0,
    high DECIMAL(20,8),
    low DECIMAL(20,8),
    change_amount DECIMAL(20,8),
    change_percent DECIMAL(8,4),
    vwap DECIMAL(20,8)
) PARTITION BY RANGE (timestamp);

-- OHLC data (historical candles)
CREATE TABLE market_data_ohlc (
    ohlc_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol VARCHAR(10) NOT NULL REFERENCES instruments(symbol),
    timeframe VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    open_price DECIMAL(20,8) NOT NULL,
    high_price DECIMAL(20,8) NOT NULL,
    low_price DECIMAL(20,8) NOT NULL,
    close_price DECIMAL(20,8) NOT NULL,
    volume BIGINT DEFAULT 0,
    trade_count INTEGER DEFAULT 0,
    vwap DECIMAL(20,8),
    UNIQUE(symbol, timeframe, timestamp)
) PARTITION BY RANGE (timestamp);

-- Market sessions
CREATE TABLE market_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trading_date DATE NOT NULL,
    session_type VARCHAR(20) DEFAULT 'REGULAR',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    timezone VARCHAR(50) DEFAULT 'America/New_York',
    is_active BOOLEAN DEFAULT TRUE
);

-- =====================================================
-- Audit and Configuration Tables
-- =====================================================

-- Order audit log
CREATE TABLE audit.order_audit_log (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    old_quantity BIGINT,
    new_quantity BIGINT,
    price DECIMAL(20,8),
    reason TEXT,
    user_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- System configuration
CREATE TABLE system_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description TEXT,
    config_type VARCHAR(20) DEFAULT 'STRING',
    is_encrypted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Create Partitions for Market Data (last 12 months)
-- =====================================================

-- Current year partitions for market_data_ticks
DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
BEGIN
    FOR i IN 0..11 LOOP
        start_date := date_trunc('month', CURRENT_DATE - INTERVAL '1 month' * i);
        end_date := start_date + INTERVAL '1 month';
        partition_name := 'market_data_ticks_' || to_char(start_date, 'YYYY_MM');
        
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I PARTITION OF market_data_ticks
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date);
    END LOOP;
END $$;

-- Current year partitions for market_data_ohlc
DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
BEGIN
    FOR i IN 0..11 LOOP
        start_date := date_trunc('month', CURRENT_DATE - INTERVAL '1 month' * i);
        end_date := start_date + INTERVAL '1 month';
        partition_name := 'market_data_ohlc_' || to_char(start_date, 'YYYY_MM');
        
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I PARTITION OF market_data_ohlc
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date);
    END LOOP;
END $$;

-- =====================================================
-- Indexes for Performance
-- =====================================================

-- Users indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(active);

-- Accounts indexes
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_active ON accounts(active);
CREATE INDEX idx_accounts_created_at ON accounts(created_at);

-- Orders indexes
CREATE INDEX idx_orders_account_id ON orders(account_id);
CREATE INDEX idx_orders_symbol ON orders(symbol);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_side ON orders(side);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_sequence ON orders(sequence_number);
CREATE INDEX idx_orders_symbol_status ON orders(symbol, status);
CREATE INDEX idx_orders_account_status ON orders(account_id, status);

-- Trades indexes
CREATE INDEX idx_trades_symbol ON trades(symbol);
CREATE INDEX idx_trades_execution_time ON trades(execution_time);
CREATE INDEX idx_trades_buy_account ON trades(buy_account_id);
CREATE INDEX idx_trades_sell_account ON trades(sell_account_id);
CREATE INDEX idx_trades_buy_order ON trades(buy_order_id);
CREATE INDEX idx_trades_sell_order ON trades(sell_order_id);
CREATE INDEX idx_trades_symbol_time ON trades(symbol, execution_time);

-- Positions indexes
CREATE INDEX idx_positions_account_id ON positions(account_id);
CREATE INDEX idx_positions_symbol ON positions(symbol);

-- Market data indexes
CREATE INDEX idx_market_ticks_symbol_time ON market_data_ticks(symbol, timestamp);
CREATE INDEX idx_market_ohlc_symbol_timeframe_time ON market_data_ohlc(symbol, timeframe, timestamp);

-- Transaction indexes
CREATE INDEX idx_transactions_account_id ON account_transactions(account_id);
CREATE INDEX idx_transactions_type ON account_transactions(type);
CREATE INDEX idx_transactions_created_at ON account_transactions(created_at);

-- =====================================================
-- Functions and Triggers
-- =====================================================

-- Update timestamp function
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply update timestamp triggers
CREATE TRIGGER trigger_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_positions_updated_at
    BEFORE UPDATE ON positions
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Order audit trigger function
CREATE OR REPLACE FUNCTION audit_order_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF OLD.status != NEW.status OR OLD.filled_quantity != NEW.filled_quantity THEN
            INSERT INTO audit.order_audit_log 
            (order_id, action, old_status, new_status, old_quantity, new_quantity, price, reason)
            VALUES 
            (NEW.order_id, 'UPDATE', OLD.status, NEW.status, OLD.filled_quantity, NEW.filled_quantity, NEW.price, NEW.text);
        END IF;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO audit.order_audit_log 
        (order_id, action, new_status, new_quantity, price)
        VALUES 
        (NEW.order_id, 'INSERT', NEW.status, NEW.quantity, NEW.price);
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Apply audit trigger
CREATE TRIGGER trigger_orders_audit
    AFTER INSERT OR UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION audit_order_changes();

-- =====================================================
-- Views for Reporting
-- =====================================================

-- Active orders view
CREATE VIEW reporting.active_orders AS
SELECT 
    o.*,
    a.account_name,
    u.username,
    i.name as instrument_name
FROM orders o
JOIN accounts a ON o.account_id = a.account_id
JOIN users u ON a.user_id = u.user_id
JOIN instruments i ON o.symbol = i.symbol
WHERE o.status IN ('NEW', 'PARTIALLY_FILLED');

-- Account positions with market data
CREATE VIEW reporting.account_positions AS
SELECT 
    p.*,
    a.account_name,
    u.username,
    i.name as instrument_name,
    CASE 
        WHEN p.quantity > 0 THEN 'LONG'
        WHEN p.quantity < 0 THEN 'SHORT'
        ELSE 'FLAT'
    END as position_side
FROM positions p
JOIN accounts a ON p.account_id = a.account_id
JOIN users u ON a.user_id = u.user_id
JOIN instruments i ON p.symbol = i.symbol
WHERE p.quantity != 0;

-- Daily trading summary
CREATE VIEW reporting.daily_trading_summary AS
SELECT 
    DATE(execution_time) as trading_date,
    symbol,
    COUNT(*) as trade_count,
    SUM(quantity) as total_volume,
    SUM(value) as total_value,
    MIN(price) as low_price,
    MAX(price) as high_price,
    (array_agg(price ORDER BY execution_time))[1] as open_price,
    (array_agg(price ORDER BY execution_time DESC))[1] as close_price,
    AVG(price) as avg_price
FROM trades
GROUP BY DATE(execution_time), symbol;

-- =====================================================
-- Grant Permissions
-- =====================================================

-- Create application user if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'fasttrader') THEN
        CREATE ROLE fasttrader WITH LOGIN PASSWORD 'fasttrader123';
    END IF;
END $$;

-- Grant schema permissions
GRANT USAGE ON SCHEMA public TO fasttrader;
GRANT USAGE ON SCHEMA trading TO fasttrader;
GRANT USAGE ON SCHEMA reporting TO fasttrader;
GRANT USAGE ON SCHEMA audit TO fasttrader;

-- Grant table permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO fasttrader;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA trading TO fasttrader;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA reporting TO fasttrader;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA audit TO fasttrader;

-- Grant sequence permissions
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO fasttrader;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA trading TO fasttrader;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA reporting TO fasttrader;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA audit TO fasttrader;

-- Record migration
INSERT INTO migrations (version, description, checksum) VALUES 
('001', 'Initial database schema creation', 'init_001');

COMMIT;