import { io, Socket } from 'socket.io-client'
import { WebSocketMessage } from '@/types'

class WebSocketService {
  private socket: Socket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000
  private subscribers: Map<string, Set<(data: any) => void>> = new Map()

  connect(url = '/ws'): void {
    if (this.socket?.connected) {
      console.log('WebSocket already connected')
      return
    }

    this.socket = io(url, {
      transports: ['websocket', 'polling'],
      reconnection: true,
      reconnectionDelay: this.reconnectDelay,
      reconnectionDelayMax: 10000,
      reconnectionAttempts: this.maxReconnectAttempts,
    })

    this.setupEventHandlers()
  }

  private setupEventHandlers(): void {
    if (!this.socket) return

    this.socket.on('connect', () => {
      console.log('WebSocket connected')
      this.reconnectAttempts = 0
      this.emit('connection', { status: 'connected' })
    })

    this.socket.on('disconnect', (reason) => {
      console.log('WebSocket disconnected:', reason)
      this.emit('connection', { status: 'disconnected', reason })
    })

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket connection error:', error)
      this.reconnectAttempts++
      
      if (this.reconnectAttempts >= this.maxReconnectAttempts) {
        this.emit('connection', { 
          status: 'failed', 
          error: 'Max reconnection attempts reached' 
        })
      }
    })

    // Handle incoming messages
    this.socket.onAny((eventName, data) => {
      this.handleMessage(eventName, data)
    })
  }

  private handleMessage(eventName: string, data: any): void {
    const subscribers = this.subscribers.get(eventName)
    if (subscribers) {
      subscribers.forEach(callback => callback(data))
    }

    // Also emit to wildcard subscribers
    const wildcardSubscribers = this.subscribers.get('*')
    if (wildcardSubscribers) {
      wildcardSubscribers.forEach(callback => 
        callback({ type: eventName, data, timestamp: Date.now() })
      )
    }
  }

  subscribe(channel: string, callback: (data: any) => void): () => void {
    if (!this.subscribers.has(channel)) {
      this.subscribers.set(channel, new Set())
    }
    
    this.subscribers.get(channel)!.add(callback)

    // Subscribe to the channel on the server
    if (this.socket?.connected && channel !== '*') {
      this.socket.emit('subscribe', { channel })
    }

    // Return unsubscribe function
    return () => {
      const subscribers = this.subscribers.get(channel)
      if (subscribers) {
        subscribers.delete(callback)
        if (subscribers.size === 0) {
          this.subscribers.delete(channel)
          // Unsubscribe from the channel on the server
          if (this.socket?.connected && channel !== '*') {
            this.socket.emit('unsubscribe', { channel })
          }
        }
      }
    }
  }

  emit(event: string, data?: any): void {
    if (this.socket?.connected) {
      this.socket.emit(event, data)
    } else {
      console.warn('WebSocket not connected, cannot emit:', event)
    }
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.disconnect()
      this.socket = null
      this.subscribers.clear()
    }
  }

  isConnected(): boolean {
    return this.socket?.connected || false
  }

  // Specific subscription methods for trading data
  subscribeToOrders(accountId: string, callback: (order: any) => void): () => void {
    return this.subscribe(`orders:${accountId}`, callback)
  }

  subscribeToTrades(symbol: string, callback: (trade: any) => void): () => void {
    return this.subscribe(`trades:${symbol}`, callback)
  }

  subscribeToOrderBook(symbol: string, callback: (orderBook: any) => void): () => void {
    return this.subscribe(`orderbook:${symbol}`, callback)
  }

  subscribeToMarketData(symbol: string, callback: (marketData: any) => void): () => void {
    return this.subscribe(`marketdata:${symbol}`, callback)
  }

  subscribeToNotifications(callback: (notification: any) => void): () => void {
    return this.subscribe('notifications', callback)
  }

  // Send a ping to keep the connection alive
  ping(): void {
    this.emit('ping', { timestamp: Date.now() })
  }
}

// Create a singleton instance
const wsService = new WebSocketService()

export default wsService