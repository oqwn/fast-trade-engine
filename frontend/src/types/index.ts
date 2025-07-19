export interface Order {
  orderId: string
  accountId: string
  symbol: string
  side: 'BUY' | 'SELL'
  type: 'LIMIT' | 'MARKET' | 'STOP' | 'STOP_LIMIT'
  price?: number
  quantity: number
  filledQuantity: number
  remainingQuantity: number
  status: 'NEW' | 'PARTIALLY_FILLED' | 'FILLED' | 'CANCELLED' | 'REJECTED' | 'EXPIRED'
  createdAt: string
  updatedAt?: string
  clientOrderId?: string
  averagePrice?: number
}

export interface Trade {
  tradeId: string
  symbol: string
  buyOrderId: string
  sellOrderId: string
  buyAccountId: string
  sellAccountId: string
  price: number
  quantity: number
  value: number
  aggressorSide: 'BUY' | 'SELL'
  executionTime: string
}

export interface OrderBookLevel {
  price: number
  quantity: number
  orderCount?: number
}

export interface OrderBookData {
  symbol: string
  bestBid?: number
  bestAsk?: number
  bestBidSize?: number
  bestAskSize?: number
  lastPrice?: number
  spread?: number
  bidLevels: OrderBookLevel[]
  askLevels: OrderBookLevel[]
  timestamp: string
}

export interface MarketData {
  symbol: string
  lastPrice?: number
  bidPrice?: number
  askPrice?: number
  bidSize?: number
  askSize?: number
  openPrice?: number
  highPrice?: number
  lowPrice?: number
  previousClose?: number
  volume?: number
  trades?: number
  change?: number
  changePercent?: number
  timestamp: string
}

export interface Account {
  accountId: string
  accountName: string
  balance: number
  availableBalance: number
  frozenBalance: number
  active: boolean
  createdAt: string
  updatedAt?: string
}

export interface Position {
  symbol: string
  quantity: number
  averagePrice: number
  currentPrice?: number
  marketValue?: number
  realizedPnL: number
  unrealizedPnL?: number
  totalPnL?: number
  side: 'LONG' | 'SHORT' | 'FLAT'
  createdAt: string
  updatedAt?: string
}

export interface OrderRequest {
  accountId: string
  symbol: string
  side: 'BUY' | 'SELL'
  type: 'LIMIT' | 'MARKET'
  price?: number
  quantity: number
  clientOrderId?: string
}

export interface WebSocketMessage {
  type: string
  data: any
  timestamp: number
}

export interface Notification {
  id: string
  type: 'info' | 'success' | 'warning' | 'error'
  title: string
  message: string
  timestamp: number
}

export interface OHLC {
  timestamp: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export interface OHLCResponse {
  symbol: string
  interval: string
  data: OHLC[]
}

export interface BalanceResponse {
  balance: number
  availableBalance: number
  frozenBalance: number
}

export interface EquityResponse {
  totalEquity: number
  timestamp: number
}

export interface DepositWithdrawRequest {
  amount: number
}

export interface ModifyOrderRequest {
  price?: number
  quantity?: number
}