import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import { WebSocketMessage } from '@/types'

class WebSocketService {
  private client: Client | null = null
  private isConnected = false
  private subscriptions: Map<string, StompSubscription> = new Map()
  private messageHandlers: Map<string, Set<(data: any) => void>> = new Map()

  connect(url = 'ws://localhost:20010/ws'): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.isConnected) {
        console.log('WebSocket already connected')
        resolve()
        return
      }

      this.client = new Client({
        brokerURL: url,
        connectHeaders: {},
        debug: (str) => {
          console.log('STOMP Debug:', str)
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      })

      this.client.onConnect = () => {
        console.log('WebSocket connected')
        this.isConnected = true
        this.setupSubscriptions()
        resolve()
      }

      this.client.onDisconnect = () => {
        console.log('WebSocket disconnected')
        this.isConnected = false
        this.subscriptions.clear()
      }

      this.client.onStompError = (frame) => {
        console.error('WebSocket STOMP error:', frame)
        reject(new Error(`STOMP error: ${frame.headers?.message}`))
      }

      this.client.activate()
    })
  }

  private setupSubscriptions(): void {
    // Subscribe to heartbeat
    this.subscribe('/topic/heartbeat', (message) => {
      console.log('Heartbeat received:', message)
    })
  }

  private handleMessage(destination: string, message: IMessage): void {
    try {
      const data = JSON.parse(message.body)
      const handlers = this.messageHandlers.get(destination)
      if (handlers) {
        handlers.forEach(callback => callback(data))
      }

      // Also emit to wildcard subscribers
      const wildcardHandlers = this.messageHandlers.get('*')
      if (wildcardHandlers) {
        wildcardHandlers.forEach(callback => 
          callback({ destination, data, timestamp: Date.now() })
        )
      }
    } catch (error) {
      console.error('Error parsing WebSocket message:', error)
    }
  }

  subscribe(destination: string, callback: (data: any) => void): () => void {
    if (!this.messageHandlers.has(destination)) {
      this.messageHandlers.set(destination, new Set())
    }
    
    this.messageHandlers.get(destination)!.add(callback)

    // Subscribe to the destination on the server if connected
    if (this.isConnected && this.client && destination !== '*') {
      const subscription = this.client.subscribe(destination, (message) => {
        this.handleMessage(destination, message)
      })
      this.subscriptions.set(destination, subscription)
    }

    // Return unsubscribe function
    return () => {
      const handlers = this.messageHandlers.get(destination)
      if (handlers) {
        handlers.delete(callback)
        if (handlers.size === 0) {
          this.messageHandlers.delete(destination)
          // Unsubscribe from the destination on the server
          const subscription = this.subscriptions.get(destination)
          if (subscription) {
            subscription.unsubscribe()
            this.subscriptions.delete(destination)
          }
        }
      }
    }
  }

  send(destination: string, data?: any): void {
    if (this.isConnected && this.client) {
      this.client.publish({
        destination,
        body: JSON.stringify(data || {}),
      })
    } else {
      console.warn('WebSocket not connected, cannot send to:', destination)
    }
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate()
      this.client = null
      this.isConnected = false
      this.subscriptions.clear()
      this.messageHandlers.clear()
    }
  }

  getConnectionStatus(): boolean {
    return this.isConnected
  }

  // Specific subscription methods for trading data
  subscribeToMarketData(symbol: string, callback: (data: any) => void): () => void {
    // Send subscription request
    this.send('/subscribe/marketdata/' + symbol, { symbol })
    return this.subscribe('/topic/marketdata/' + symbol, callback)
  }

  subscribeToOrderBook(symbol: string, callback: (data: any) => void): () => void {
    // Send subscription request
    this.send('/subscribe/orderbook/' + symbol, { symbol })
    return this.subscribe('/topic/orderbook/' + symbol, callback)
  }

  subscribeToTrades(symbol: string, callback: (data: any) => void): () => void {
    // Send subscription request
    this.send('/subscribe/trades/' + symbol, { symbol })
    return this.subscribe('/topic/trades/' + symbol, callback)
  }

  subscribeToOrderUpdates(callback: (data: any) => void): () => void {
    return this.subscribe('/topic/orders', callback)
  }

  subscribeToTradeUpdates(callback: (data: any) => void): () => void {
    return this.subscribe('/topic/trade-executions', callback)
  }

  subscribeToNotifications(callback: (data: any) => void): () => void {
    return this.subscribe('/topic/notifications', callback)
  }

  // Send a ping to keep the connection alive
  ping(): void {
    this.send('/ping', { timestamp: Date.now() })
  }
}

// Create a singleton instance
const wsService = new WebSocketService()

export default wsService