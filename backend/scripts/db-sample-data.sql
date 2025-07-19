-- =====================================================
-- Fast Trade Engine - Sample Data Population
-- =====================================================
-- This script populates the database with realistic sample data
-- for development, testing, and demonstration purposes.

-- =====================================================
-- System Configuration
-- =====================================================

INSERT INTO system_config (config_key, config_value, description, config_type) VALUES
('trading.market_open', '09:30:00', 'Market opening time (EST)', 'TIME'),
('trading.market_close', '16:00:00', 'Market closing time (EST)', 'TIME'),
('trading.pre_market_start', '04:00:00', 'Pre-market trading start time', 'TIME'),
('trading.after_hours_end', '20:00:00', 'After-hours trading end time', 'TIME'),
('trading.tick_size_default', '0.01', 'Default tick size for instruments', 'DECIMAL'),
('trading.commission_rate', '0.005', 'Default commission rate (0.5%)', 'DECIMAL'),
('trading.max_order_quantity', '1000000', 'Maximum order quantity allowed', 'INTEGER'),
('trading.price_limit_percent', '10.0', 'Daily price limit percentage', 'DECIMAL'),
('trading.circuit_breaker_l1', '7.0', 'Level 1 circuit breaker percentage', 'DECIMAL'),
('trading.circuit_breaker_l2', '13.0', 'Level 2 circuit breaker percentage', 'DECIMAL'),
('trading.circuit_breaker_l3', '20.0', 'Level 3 circuit breaker percentage', 'DECIMAL'),
('system.max_connections', '1000', 'Maximum concurrent connections', 'INTEGER'),
('system.order_timeout_seconds', '86400', 'Default order timeout in seconds', 'INTEGER'),
('system.maintenance_mode', 'false', 'System maintenance mode flag', 'BOOLEAN')
ON CONFLICT (config_key) DO UPDATE SET 
    config_value = EXCLUDED.config_value,
    updated_at = CURRENT_TIMESTAMP;

-- =====================================================
-- Market Sessions
-- =====================================================

INSERT INTO market_sessions (trading_date, session_type, start_time, end_time, timezone, is_active) VALUES
(CURRENT_DATE, 'PRE_MARKET', '04:00:00', '09:30:00', 'America/New_York', true),
(CURRENT_DATE, 'REGULAR', '09:30:00', '16:00:00', 'America/New_York', true),
(CURRENT_DATE, 'AFTER_HOURS', '16:00:00', '20:00:00', 'America/New_York', true),
(CURRENT_DATE + 1, 'PRE_MARKET', '04:00:00', '09:30:00', 'America/New_York', true),
(CURRENT_DATE + 1, 'REGULAR', '09:30:00', '16:00:00', 'America/New_York', true),
(CURRENT_DATE + 1, 'AFTER_HOURS', '16:00:00', '20:00:00', 'America/New_York', true);

-- =====================================================
-- Instruments (Popular Stocks)
-- =====================================================

INSERT INTO instruments (symbol, name, exchange, sector, industry, market_cap, tick_size, lot_size, active) VALUES
-- Technology
('AAPL', 'Apple Inc.', 'NASDAQ', 'Technology', 'Consumer Electronics', 3000000000000, 0.01, 1, true),
('MSFT', 'Microsoft Corporation', 'NASDAQ', 'Technology', 'Software', 2800000000000, 0.01, 1, true),
('GOOGL', 'Alphabet Inc. Class A', 'NASDAQ', 'Technology', 'Internet Content & Information', 1700000000000, 0.01, 1, true),
('AMZN', 'Amazon.com Inc.', 'NASDAQ', 'Consumer Discretionary', 'Internet & Direct Marketing Retail', 1500000000000, 0.01, 1, true),
('TSLA', 'Tesla Inc.', 'NASDAQ', 'Consumer Discretionary', 'Automobiles', 800000000000, 0.01, 1, true),
('META', 'Meta Platforms Inc.', 'NASDAQ', 'Technology', 'Social Media', 750000000000, 0.01, 1, true),
('NVDA', 'NVIDIA Corporation', 'NASDAQ', 'Technology', 'Semiconductors', 1800000000000, 0.01, 1, true),
('NFLX', 'Netflix Inc.', 'NASDAQ', 'Communication Services', 'Entertainment', 200000000000, 0.01, 1, true),

-- Financial
('JPM', 'JPMorgan Chase & Co.', 'NYSE', 'Financial Services', 'Banks', 450000000000, 0.01, 1, true),
('BAC', 'Bank of America Corporation', 'NYSE', 'Financial Services', 'Banks', 250000000000, 0.01, 1, true),
('WFC', 'Wells Fargo & Company', 'NYSE', 'Financial Services', 'Banks', 180000000000, 0.01, 1, true),
('GS', 'The Goldman Sachs Group Inc.', 'NYSE', 'Financial Services', 'Investment Banking', 120000000000, 0.01, 1, true),

-- Healthcare
('JNJ', 'Johnson & Johnson', 'NYSE', 'Healthcare', 'Pharmaceuticals', 450000000000, 0.01, 1, true),
('PFE', 'Pfizer Inc.', 'NYSE', 'Healthcare', 'Pharmaceuticals', 280000000000, 0.01, 1, true),
('UNH', 'UnitedHealth Group Incorporated', 'NYSE', 'Healthcare', 'Health Insurance', 500000000000, 0.01, 1, true),

-- Consumer Goods
('KO', 'The Coca-Cola Company', 'NYSE', 'Consumer Staples', 'Beverages', 250000000000, 0.01, 1, true),
('PEP', 'PepsiCo Inc.', 'NASDAQ', 'Consumer Staples', 'Beverages', 230000000000, 0.01, 1, true),
('WMT', 'Walmart Inc.', 'NYSE', 'Consumer Staples', 'Discount Stores', 400000000000, 0.01, 1, true),

-- Energy
('XOM', 'Exxon Mobil Corporation', 'NYSE', 'Energy', 'Oil & Gas', 450000000000, 0.01, 1, true),
('CVX', 'Chevron Corporation', 'NYSE', 'Energy', 'Oil & Gas', 350000000000, 0.01, 1, true),

-- ETFs for diversification
('SPY', 'SPDR S&P 500 ETF Trust', 'NYSE', 'ETF', 'Large Cap Equity', 400000000000, 0.01, 1, true),
('QQQ', 'Invesco QQQ Trust', 'NASDAQ', 'ETF', 'Technology', 180000000000, 0.01, 1, true),
('IWM', 'iShares Russell 2000 ETF', 'NYSE', 'ETF', 'Small Cap Equity', 60000000000, 0.01, 1, true)
ON CONFLICT (symbol) DO UPDATE SET 
    name = EXCLUDED.name,
    market_cap = EXCLUDED.market_cap;

-- =====================================================
-- Sample Users
-- =====================================================

INSERT INTO users (user_id, username, email, password_hash, first_name, last_name, phone, verified, active) VALUES
('11111111-1111-1111-1111-111111111111', 'alice_trader', 'alice@fasttrader.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IlZjGKvf8XQGqN8E4dE2Q5E9E5E5E5', 'Alice', 'Johnson', '+1-555-0101', true, true),
('22222222-2222-2222-2222-222222222222', 'bob_investor', 'bob@fasttrader.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IlZjGKvf8XQGqN8E4dE2Q5E9E5E5E5', 'Bob', 'Smith', '+1-555-0102', true, true),
('33333333-3333-3333-3333-333333333333', 'charlie_daytrader', 'charlie@fasttrader.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IlZjGKvf8XQGqN8E4dE2Q5E9E5E5E5', 'Charlie', 'Brown', '+1-555-0103', true, true),
('44444444-4444-4444-4444-444444444444', 'diana_quant', 'diana@fasttrader.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IlZjGKvf8XQGqN8E4dE2Q5E9E5E5E5', 'Diana', 'Miller', '+1-555-0104', true, true),
('55555555-5555-5555-5555-555555555555', 'eve_scalper', 'eve@fasttrader.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IlZjGKvf8XQGqN8E4dE2Q5E9E5E5E5', 'Eve', 'Davis', '+1-555-0105', true, true),
('66666666-6666-6666-6666-666666666666', 'frank_swing', 'frank@fasttrader.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IlZjGKvf8XQGqN8E4dE2Q5E9E5E5E5', 'Frank', 'Wilson', '+1-555-0106', true, true),
('77777777-7777-7777-7777-777777777777', 'grace_momentum', 'grace@fasttrader.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IlZjGKvf8XQGqN8E4dE2Q5E9E5E5E5', 'Grace', 'Lee', '+1-555-0107', true, true),
('88888888-8888-8888-8888-888888888888', 'henry_value', 'henry@fasttrader.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IlZjGKvf8XQGqN8E4dE2Q5E9E5E5E5', 'Henry', 'Taylor', '+1-555-0108', true, true),
('99999999-9999-9999-9999-999999999999', 'admin_user', 'admin@fasttrader.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IlZjGKvf8XQGqN8E4dE2Q5E9E5E5E5', 'Admin', 'User', '+1-555-0100', true, true),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'demo_user', 'demo@fasttrader.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IlZjGKvf8XQGqN8E4dE2Q5E9E5E5E5', 'Demo', 'User', '+1-555-0109', true, true)
ON CONFLICT (user_id) DO NOTHING;

-- =====================================================
-- Sample Accounts
-- =====================================================

INSERT INTO accounts (account_id, user_id, account_name, account_type, balance, available_balance, frozen_balance, currency, risk_limit, daily_loss_limit) VALUES
('a0000001-0001-0001-0001-000000000001', '11111111-1111-1111-1111-111111111111', 'Alice Trading Account', 'INDIVIDUAL', 100000.00, 95000.00, 5000.00, 'USD', 500000, 5000),
('a0000002-0002-0002-0002-000000000002', '22222222-2222-2222-2222-222222222222', 'Bob Investment Account', 'INDIVIDUAL', 250000.00, 230000.00, 20000.00, 'USD', 1000000, 10000),
('a0000003-0003-0003-0003-000000000003', '33333333-3333-3333-3333-333333333333', 'Charlie Day Trading', 'INDIVIDUAL', 50000.00, 45000.00, 5000.00, 'USD', 200000, 2500),
('a0000004-0004-0004-0004-000000000004', '44444444-4444-4444-4444-444444444444', 'Diana Quant Fund', 'PROFESSIONAL', 1000000.00, 900000.00, 100000.00, 'USD', 5000000, 50000),
('a0000005-0005-0005-0005-000000000005', '55555555-5555-5555-5555-555555555555', 'Eve Scalping Account', 'INDIVIDUAL', 75000.00, 70000.00, 5000.00, 'USD', 300000, 3750),
('a0000006-0006-0006-0006-000000000006', '66666666-6666-6666-6666-666666666666', 'Frank Swing Trading', 'INDIVIDUAL', 150000.00, 140000.00, 10000.00, 'USD', 750000, 7500),
('a0000007-0007-0007-0007-000000000007', '77777777-7777-7777-7777-777777777777', 'Grace Momentum', 'INDIVIDUAL', 200000.00, 180000.00, 20000.00, 'USD', 1000000, 10000),
('a0000008-0008-0008-0008-000000000008', '88888888-8888-8888-8888-888888888888', 'Henry Value Investing', 'INDIVIDUAL', 500000.00, 450000.00, 50000.00, 'USD', 2000000, 25000),
('a0000009-0009-0009-0009-000000000009', '99999999-9999-9999-9999-999999999999', 'Admin Test Account', 'ADMIN', 1000000.00, 1000000.00, 0.00, 'USD', 10000000, 100000),
('a000000a-000a-000a-000a-00000000000a', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Demo Trading Account', 'DEMO', 10000.00, 10000.00, 0.00, 'USD', 50000, 1000)
ON CONFLICT (account_id) DO NOTHING;

-- =====================================================
-- Sample Market Data (Current Prices)
-- =====================================================

INSERT INTO market_data_ticks (symbol, timestamp, last_price, bid_price, ask_price, bid_size, ask_size, volume, high, low, change_amount, change_percent) VALUES
('AAPL', CURRENT_TIMESTAMP, 175.50, 175.48, 175.52, 100, 200, 1500000, 177.25, 174.80, 2.35, 1.36),
('MSFT', CURRENT_TIMESTAMP, 378.25, 378.20, 378.30, 150, 100, 850000, 380.50, 376.00, 5.75, 1.54),
('GOOGL', CURRENT_TIMESTAMP, 140.75, 140.70, 140.80, 75, 125, 620000, 142.30, 139.50, 0.85, 0.61),
('AMZN', CURRENT_TIMESTAMP, 151.25, 151.20, 151.30, 200, 180, 950000, 153.00, 150.00, -1.15, -0.75),
('TSLA', CURRENT_TIMESTAMP, 208.50, 208.45, 208.55, 300, 250, 2100000, 212.00, 206.75, 3.25, 1.58),
('META', CURRENT_TIMESTAMP, 485.75, 485.70, 485.80, 80, 90, 750000, 488.50, 483.25, 7.50, 1.57),
('NVDA', CURRENT_TIMESTAMP, 875.25, 875.15, 875.35, 60, 70, 1800000, 885.00, 870.50, 15.75, 1.83),
('NFLX', CURRENT_TIMESTAMP, 425.80, 425.75, 425.85, 110, 95, 450000, 428.50, 423.25, 3.55, 0.84),
('JPM', CURRENT_TIMESTAMP, 165.45, 165.40, 165.50, 120, 140, 380000, 166.75, 164.50, 1.25, 0.76),
('BAC', CURRENT_TIMESTAMP, 32.85, 32.84, 32.86, 500, 450, 650000, 33.15, 32.70, 0.15, 0.46),
('SPY', CURRENT_TIMESTAMP, 485.25, 485.20, 485.30, 200, 180, 2800000, 487.50, 483.75, 2.75, 0.57),
('QQQ', CURRENT_TIMESTAMP, 395.80, 395.75, 395.85, 150, 170, 1900000, 398.25, 394.50, 1.95, 0.49)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Sample Historical OHLC Data (Last 30 days, daily)
-- =====================================================

-- Generate 30 days of historical daily OHLC data for major symbols
DO $$
DECLARE
    symbol_rec RECORD;
    day_offset INTEGER;
    base_price DECIMAL(20,8);
    open_price DECIMAL(20,8);
    high_price DECIMAL(20,8);
    low_price DECIMAL(20,8);
    close_price DECIMAL(20,8);
    daily_volume BIGINT;
    price_change DECIMAL(20,8);
BEGIN
    FOR symbol_rec IN SELECT symbol, last_price FROM market_data_ticks WHERE symbol IN ('AAPL', 'MSFT', 'GOOGL', 'AMZN', 'TSLA', 'SPY') LOOP
        base_price := symbol_rec.last_price;
        
        FOR day_offset IN 1..30 LOOP
            -- Random price movement
            price_change := (random() - 0.5) * base_price * 0.05; -- ±2.5% daily movement
            open_price := base_price + price_change;
            
            -- High and low relative to open
            high_price := open_price + (random() * open_price * 0.03); -- Up to 3% higher
            low_price := open_price - (random() * open_price * 0.03);  -- Up to 3% lower
            
            -- Close price within high/low range
            close_price := low_price + (random() * (high_price - low_price));
            
            -- Random volume
            daily_volume := (500000 + random() * 2000000)::BIGINT;
            
            INSERT INTO market_data_ohlc (
                symbol, timeframe, timestamp, 
                open_price, high_price, low_price, close_price, 
                volume, trade_count, vwap
            ) VALUES (
                symbol_rec.symbol, '1D', 
                CURRENT_TIMESTAMP - INTERVAL '1 day' * day_offset,
                open_price, high_price, low_price, close_price,
                daily_volume, (daily_volume / 100)::INTEGER,
                (open_price + high_price + low_price + close_price) / 4
            ) ON CONFLICT (symbol, timeframe, timestamp) DO NOTHING;
            
            -- Update base price for next day
            base_price := close_price;
        END LOOP;
    END LOOP;
END $$;

-- =====================================================
-- Sample Orders (Mix of Active and Historical)
-- =====================================================

-- Active orders (NEW and PARTIALLY_FILLED)
INSERT INTO orders (order_id, account_id, client_order_id, symbol, side, type, quantity, price, status, time_in_force) VALUES
-- Alice's orders
('o0000001-0001-0001-0001-000000000001', 'a0000001-0001-0001-0001-000000000001', 'ALICE001', 'AAPL', 'BUY', 'LIMIT', 100, 174.50, 'NEW', 'GTC'),
('o0000002-0002-0002-0002-000000000002', 'a0000001-0001-0001-0001-000000000001', 'ALICE002', 'MSFT', 'BUY', 'LIMIT', 50, 375.00, 'NEW', 'DAY'),

-- Bob's orders
('o0000003-0003-0003-0003-000000000003', 'a0000002-0002-0002-0002-000000000002', 'BOB001', 'GOOGL', 'BUY', 'LIMIT', 200, 139.75, 'NEW', 'GTC'),
('o0000004-0004-0004-0004-000000000004', 'a0000002-0002-0002-0002-000000000002', 'BOB002', 'SPY', 'BUY', 'LIMIT', 500, 484.00, 'PARTIALLY_FILLED', 'GTC'),

-- Charlie's day trading orders
('o0000005-0005-0005-0005-000000000005', 'a0000003-0003-0003-0003-000000000003', 'CHARLIE001', 'TSLA', 'BUY', 'MARKET', 25, NULL, 'NEW', 'IOC'),
('o0000006-0006-0006-0006-000000000006', 'a0000003-0003-0003-0003-000000000003', 'CHARLIE002', 'NVDA', 'SELL', 'LIMIT', 10, 880.00, 'NEW', 'DAY'),

-- Diana's quant orders
('o0000007-0007-0007-0007-000000000007', 'a0000004-0004-0004-0004-000000000004', 'DIANA001', 'META', 'BUY', 'LIMIT', 1000, 485.00, 'NEW', 'GTC'),
('o0000008-0008-0008-0008-000000000008', 'a0000004-0004-0004-0004-000000000004', 'DIANA002', 'AMZN', 'SELL', 'LIMIT', 500, 152.00, 'NEW', 'GTC')
ON CONFLICT (order_id) DO NOTHING;

-- Update filled quantities for partially filled orders
UPDATE orders SET 
    filled_quantity = 250,
    average_fill_price = 484.25,
    updated_at = CURRENT_TIMESTAMP
WHERE order_id = 'o0000004-0004-0004-0004-000000000004';

-- =====================================================
-- Sample Trades (Historical)
-- =====================================================

INSERT INTO trades (trade_id, symbol, buy_order_id, sell_order_id, buy_account_id, sell_account_id, price, quantity, aggressor_side, execution_time, commission_buy, commission_sell) VALUES
-- Recent trades
('t0000001-0001-0001-0001-000000000001', 'SPY', 'o0000004-0004-0004-0004-000000000004', 'o0000009-0009-0009-0009-000000000009', 'a0000002-0002-0002-0002-000000000002', 'a0000009-0009-0009-0009-000000000009', 484.25, 250, 'BUY', CURRENT_TIMESTAMP - INTERVAL '1 hour', 2.42, 2.42),
('t0000002-0002-0002-0002-000000000002', 'AAPL', 'o0000010-0010-0010-0010-000000000010', 'o0000011-0011-0011-0011-000000000011', 'a0000003-0003-0003-0003-000000000003', 'a0000005-0005-0005-0005-000000000005', 175.00, 50, 'SELL', CURRENT_TIMESTAMP - INTERVAL '2 hours', 0.88, 0.88),
('t0000003-0003-0003-0003-000000000003', 'TSLA', 'o0000012-0012-0012-0012-000000000012', 'o0000013-0013-0013-0013-000000000013', 'a0000006-0006-0006-0006-000000000006', 'a0000007-0007-0007-0007-000000000007', 207.50, 100, 'BUY', CURRENT_TIMESTAMP - INTERVAL '3 hours', 2.08, 2.08)
ON CONFLICT (trade_id) DO NOTHING;

-- =====================================================
-- Sample Positions
-- =====================================================

INSERT INTO positions (account_id, symbol, quantity, average_price, realized_pnl, market_value, last_update_price) VALUES
-- Alice's positions
('a0000001-0001-0001-0001-000000000001', 'AAPL', 100, 172.50, 0, 17550.00, 175.50),
('a0000001-0001-0001-0001-000000000001', 'MSFT', 25, 380.00, 150.00, 9456.25, 378.25),

-- Bob's positions
('a0000002-0002-0002-0002-000000000002', 'SPY', 250, 484.25, 0, 121312.50, 485.25),
('a0000002-0002-0002-0002-000000000002', 'GOOGL', 150, 141.50, -112.50, 21112.50, 140.75),
('a0000002-0002-0002-0002-000000000002', 'JNJ', 200, 160.00, 500.00, 32000.00, 160.00),

-- Charlie's positions (day trader - smaller positions)
('a0000003-0003-0003-0003-000000000003', 'TSLA', 50, 205.00, 175.00, 10425.00, 208.50),

-- Diana's positions (large quant positions)
('a0000004-0004-0004-0004-000000000004', 'META', 500, 480.00, 2875.00, 242875.00, 485.75),
('a0000004-0004-0004-0004-000000000004', 'NVDA', 100, 850.00, 2525.00, 87525.00, 875.25),
('a0000004-0004-0004-0004-000000000004', 'AAPL', 1000, 170.00, 5500.00, 175500.00, 175.50),

-- Grace's momentum positions
('a0000007-0007-0007-0007-000000000007', 'TSLA', -100, 210.00, -150.00, -20850.00, 208.50),
('a0000007-0007-0007-0007-000000000007', 'NVDA', 50, 860.00, 762.50, 43762.50, 875.25),

-- Henry's value positions
('a0000008-0008-0008-0008-000000000008', 'JPM', 500, 162.00, 1725.00, 82725.00, 165.45),
('a0000008-0008-0008-0008-000000000008', 'BAC', 1000, 32.50, 350.00, 32850.00, 32.85),
('a0000008-0008-0008-0008-000000000008', 'WFC', 300, 45.00, -450.00, 13500.00, 45.00)
ON CONFLICT (account_id, symbol) DO NOTHING;

-- Update unrealized PnL for positions
UPDATE positions SET 
    unrealized_pnl = (last_update_price - average_price) * quantity,
    updated_at = CURRENT_TIMESTAMP
WHERE quantity != 0;

-- =====================================================
-- Sample Account Transactions
-- =====================================================

INSERT INTO account_transactions (account_id, type, amount, balance_after, reference_id, description) VALUES
-- Initial deposits
('a0000001-0001-0001-0001-000000000001', 'DEPOSIT', 100000.00, 100000.00, NULL, 'Initial account funding'),
('a0000002-0002-0002-0002-000000000002', 'DEPOSIT', 250000.00, 250000.00, NULL, 'Initial account funding'),
('a0000003-0003-0003-0003-000000000003', 'DEPOSIT', 50000.00, 50000.00, NULL, 'Initial account funding'),
('a0000004-0004-0004-0004-000000000004', 'DEPOSIT', 1000000.00, 1000000.00, NULL, 'Professional account funding'),

-- Trade settlements
('a0000002-0002-0002-0002-000000000002', 'TRADE_SETTLEMENT', -121062.50, 128937.50, 't0000001-0001-0001-0001-000000000001', 'Purchase of 250 shares SPY at $484.25'),
('a0000002-0002-0002-0002-000000000002', 'COMMISSION', -2.42, 128935.08, 't0000001-0001-0001-0001-000000000001', 'Commission for SPY trade'),

('a0000003-0003-0003-0003-000000000003', 'TRADE_SETTLEMENT', 8750.00, 58750.00, 't0000002-0002-0002-0002-000000000002', 'Sale of 50 shares AAPL at $175.00'),
('a0000003-0003-0003-0003-000000000003', 'COMMISSION', -0.88, 58749.12, 't0000002-0002-0002-0002-000000000002', 'Commission for AAPL trade'),

-- Dividends
('a0000008-0008-0008-0008-000000000008', 'DIVIDEND', 250.00, 500250.00, NULL, 'JPM quarterly dividend payment'),
('a0000002-0002-0002-0002-000000000002', 'DIVIDEND', 150.00, 129085.08, NULL, 'JNJ quarterly dividend payment')
ON CONFLICT DO NOTHING;

-- Record migration
INSERT INTO migrations (version, description, checksum) VALUES 
('002', 'Sample data population', 'sample_002')
ON CONFLICT (version) DO NOTHING;

-- =====================================================
-- Update Statistics
-- =====================================================

-- Analyze tables for better query performance
ANALYZE users;
ANALYZE accounts;
ANALYZE instruments;
ANALYZE orders;
ANALYZE trades;
ANALYZE positions;
ANALYZE market_data_ticks;
ANALYZE market_data_ohlc;

COMMIT;