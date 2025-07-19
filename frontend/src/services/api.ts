import axios from 'axios'
import toast from 'react-hot-toast'
import type {
  Order,
  Trade,
  OrderBookData,
  MarketData,
  Account,
  Position,
  OrderRequest,
} from '@/types'

const API_BASE_URL = '/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const message = error.response.data?.message || 'An error occurred'
      toast.error(message)
    } else if (error.request) {
      toast.error('Network error. Please check your connection.')
    } else {
      toast.error('An unexpected error occurred')
    }
    return Promise.reject(error)
  }
)

// Order APIs
export const orderApi = {
  placeOrder: async (order: OrderRequest): Promise<Order> => {
    const { data } = await api.post('/orders', order)
    return data
  },

  cancelOrder: async (orderId: string): Promise<Order> => {
    const { data } = await api.delete(`/orders/${orderId}`)
    return data
  },

  getOrder: async (orderId: string): Promise<Order> => {
    const { data } = await api.get(`/orders/${orderId}`)
    return data
  },

  getOrders: async (params?: {
    accountId?: string
    symbol?: string
    status?: string
  }): Promise<Order[]> => {
    const { data } = await api.get('/orders', { params })
    return data
  },

  modifyOrder: async (
    orderId: string,
    updates: { price?: number; quantity?: number }
  ): Promise<Order> => {
    const { data } = await api.put(`/orders/${orderId}`, updates)
    return data
  },

  getOrderBook: async (symbol: string, depth = 10): Promise<OrderBookData> => {
    const { data } = await api.get(`/orders/book/${symbol}`, {
      params: { depth },
    })
    return data
  },
}

// Trade APIs
export const tradeApi = {
  getTrades: async (params?: {
    symbol?: string
    accountId?: string
    from?: string
    to?: string
    limit?: number
  }): Promise<Trade[]> => {
    const { data } = await api.get('/trades', { params })
    return data
  },

  getTrade: async (tradeId: string): Promise<Trade> => {
    const { data } = await api.get(`/trades/${tradeId}`)
    return data
  },

  getRecentTrades: async (symbol: string, limit = 50): Promise<Trade[]> => {
    const { data } = await api.get(`/trades/recent/${symbol}`, {
      params: { limit },
    })
    return data
  },
}

// Market Data APIs
export const marketDataApi = {
  getQuote: async (symbol: string, depth = 5): Promise<MarketData> => {
    const { data } = await api.get(`/market-data/quote/${symbol}`, {
      params: { depth },
    })
    return data
  },

  getAllQuotes: async (): Promise<MarketData[]> => {
    const { data } = await api.get('/market-data/quotes')
    return data
  },

  getOHLC: async (
    symbol: string,
    interval = '1m',
    periods = 100
  ): Promise<any> => {
    const { data } = await api.get(`/market-data/ohlc/${symbol}`, {
      params: { interval, periods },
    })
    return data
  },
}

// Account APIs
export const accountApi = {
  createAccount: async (account: {
    accountId: string
    accountName: string
    initialBalance: number
  }): Promise<Account> => {
    const { data } = await api.post('/accounts', account)
    return data
  },

  getAccount: async (accountId: string): Promise<Account> => {
    const { data } = await api.get(`/accounts/${accountId}`)
    return data
  },

  getBalance: async (
    accountId: string
  ): Promise<{
    balance: number
    availableBalance: number
    frozenBalance: number
  }> => {
    const { data } = await api.get(`/accounts/${accountId}/balance`)
    return data
  },

  getPositions: async (accountId: string): Promise<Position[]> => {
    const { data } = await api.get(`/accounts/${accountId}/positions`)
    return data
  },

  deposit: async (
    accountId: string,
    amount: number
  ): Promise<{ message: string }> => {
    const { data } = await api.post(`/accounts/${accountId}/deposit`, {
      amount,
    })
    return data
  },

  withdraw: async (
    accountId: string,
    amount: number
  ): Promise<{ message: string }> => {
    const { data } = await api.post(`/accounts/${accountId}/withdraw`, {
      amount,
    })
    return data
  },

  getTotalEquity: async (
    accountId: string
  ): Promise<{ totalEquity: number; timestamp: number }> => {
    const { data } = await api.get(`/accounts/${accountId}/equity`)
    return data
  },
}

export default api