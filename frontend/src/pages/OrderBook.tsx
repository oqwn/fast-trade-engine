import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { orderApi, marketDataApi } from '@/services/api'
import { OrderBookData, OrderBookLevel } from '@/types'
import wsService from '@/services/websocket'
import { 
  ChartBarIcon, 
  ArrowPathIcon,
  SignalIcon,
  ClockIcon,
  ScaleIcon,
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon
} from '@heroicons/react/24/outline'

export default function OrderBook() {
  const [selectedSymbol, setSelectedSymbol] = useState<string>('')
  const [depth, setDepth] = useState<number>(10)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [orderBookData, setOrderBookData] = useState<OrderBookData | null>(null)

  // Fetch available symbols
  const { data: symbols } = useQuery({
    queryKey: ['available-symbols'],
    queryFn: marketDataApi.getAvailableSymbols,
  })

  // Fetch order book data
  const { data: fetchedOrderBook, refetch } = useQuery({
    queryKey: ['orderbook', selectedSymbol, depth],
    queryFn: () => orderApi.getOrderBook(selectedSymbol, depth),
    enabled: !!selectedSymbol,
    refetchInterval: autoRefresh ? 1000 : false,
  })

  // Update local state when fetched data changes
  useEffect(() => {
    if (fetchedOrderBook) {
      setOrderBookData(fetchedOrderBook)
    }
  }, [fetchedOrderBook])

  // Subscribe to WebSocket updates
  useEffect(() => {
    if (!selectedSymbol || !autoRefresh) return

    const unsubscribe = wsService.subscribeToOrderBook(selectedSymbol, (data) => {
      setOrderBookData(data)
    })

    return () => {
      unsubscribe()
    }
  }, [selectedSymbol, autoRefresh])

  // Calculate order book metrics
  const calculateMetrics = () => {
    if (!orderBookData) return null

    const totalBidVolume = orderBookData.bidLevels.reduce((sum, level) => sum + level.quantity, 0)
    const totalAskVolume = orderBookData.askLevels.reduce((sum, level) => sum + level.quantity, 0)
    const volumeImbalance = totalBidVolume - totalAskVolume
    const imbalanceRatio = totalBidVolume + totalAskVolume > 0 
      ? (volumeImbalance / (totalBidVolume + totalAskVolume)) * 100 
      : 0

    const weightedBidPrice = totalBidVolume > 0
      ? orderBookData.bidLevels.reduce((sum, level) => sum + (level.price * level.quantity), 0) / totalBidVolume
      : 0

    const weightedAskPrice = totalAskVolume > 0
      ? orderBookData.askLevels.reduce((sum, level) => sum + (level.price * level.quantity), 0) / totalAskVolume
      : 0

    return {
      totalBidVolume,
      totalAskVolume,
      volumeImbalance,
      imbalanceRatio,
      weightedBidPrice,
      weightedAskPrice,
      spread: orderBookData.spread || 0,
      midPrice: ((orderBookData.bestBid || 0) + (orderBookData.bestAsk || 0)) / 2,
    }
  }

  const metrics = calculateMetrics()

  // Calculate cumulative volumes for depth visualization
  const calculateCumulativeVolumes = (levels: OrderBookLevel[]) => {
    let cumulative = 0
    return levels.map(level => {
      cumulative += level.quantity
      return { ...level, cumulativeQuantity: cumulative }
    })
  }

  const getMaxCumulativeVolume = () => {
    if (!orderBookData) return 0
    const bidCumulative = calculateCumulativeVolumes(orderBookData.bidLevels)
    const askCumulative = calculateCumulativeVolumes(orderBookData.askLevels)
    const maxBid = bidCumulative[bidCumulative.length - 1]?.cumulativeQuantity || 0
    const maxAsk = askCumulative[askCumulative.length - 1]?.cumulativeQuantity || 0
    return Math.max(maxBid, maxAsk)
  }

  return (
    <div>
      <div className="mb-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Order Book Depth
          </h1>
          <div className="flex items-center space-x-4">
            <label className="flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
                className="mr-2 rounded border-gray-300 text-blue-500 focus:ring-blue-500"
              />
              <span className="text-sm text-gray-700 dark:text-gray-300">
                Auto-refresh
              </span>
            </label>
            <button
              onClick={() => refetch()}
              className="btn-secondary flex items-center"
            >
              <ArrowPathIcon className="h-4 w-4 mr-2" />
              Refresh
            </button>
          </div>
        </div>

        {/* Symbol and Depth Selection */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Symbol
            </label>
            <select
              value={selectedSymbol}
              onChange={(e) => setSelectedSymbol(e.target.value)}
              className="input w-full"
            >
              <option value="">Select a symbol</option>
              {symbols?.map((symbol) => (
                <option key={symbol} value={symbol}>
                  {symbol}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Depth Levels
            </label>
            <select
              value={depth}
              onChange={(e) => setDepth(Number(e.target.value))}
              className="input w-full"
            >
              <option value={5}>5 Levels</option>
              <option value={10}>10 Levels</option>
              <option value={20}>20 Levels</option>
              <option value={50}>50 Levels</option>
            </select>
          </div>
        </div>

        {/* Market Metrics */}
        {selectedSymbol && orderBookData && metrics && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="card">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm text-gray-600 dark:text-gray-400">Mid Price</span>
                <ScaleIcon className="h-4 w-4 text-gray-400" />
              </div>
              <p className="text-xl font-bold">${metrics.midPrice.toFixed(2)}</p>
            </div>
            <div className="card">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm text-gray-600 dark:text-gray-400">Spread</span>
                <SignalIcon className="h-4 w-4 text-gray-400" />
              </div>
              <p className="text-xl font-bold">${metrics.spread.toFixed(2)}</p>
              <p className="text-xs text-gray-500">
                {orderBookData.bestBid && orderBookData.bestAsk 
                  ? ((metrics.spread / metrics.midPrice) * 100).toFixed(3) + '%'
                  : '-'}
              </p>
            </div>
            <div className="card">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm text-gray-600 dark:text-gray-400">Volume Imbalance</span>
                {metrics.volumeImbalance > 0 ? (
                  <ArrowTrendingUpIcon className="h-4 w-4 text-green-500" />
                ) : (
                  <ArrowTrendingDownIcon className="h-4 w-4 text-red-500" />
                )}
              </div>
              <p className={`text-xl font-bold ${
                metrics.volumeImbalance > 0 ? 'text-green-600' : 'text-red-600'
              }`}>
                {metrics.imbalanceRatio.toFixed(1)}%
              </p>
              <p className="text-xs text-gray-500">
                {metrics.volumeImbalance > 0 ? 'Buy' : 'Sell'} pressure
              </p>
            </div>
            <div className="card">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm text-gray-600 dark:text-gray-400">Last Update</span>
                <ClockIcon className="h-4 w-4 text-gray-400" />
              </div>
              <p className="text-sm font-medium">
                {new Date(orderBookData.timestamp).toLocaleTimeString()}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Order Book Display */}
      {selectedSymbol && orderBookData ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Bid/Ask Tables */}
          <div className="card">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <ChartBarIcon className="h-5 w-5 mr-2" />
              Order Book - {selectedSymbol}
            </h2>
            
            <div className="grid grid-cols-2 gap-4">
              {/* Bids */}
              <div>
                <h3 className="text-sm font-medium text-green-600 mb-2">Bids</h3>
                <div className="space-y-1">
                  <div className="grid grid-cols-3 gap-2 text-xs text-gray-500 pb-1 border-b dark:border-gray-700">
                    <span>Price</span>
                    <span className="text-right">Quantity</span>
                    <span className="text-right">Total</span>
                  </div>
                  {orderBookData.bidLevels.map((bid, index) => {
                    const cumulative = orderBookData.bidLevels
                      .slice(0, index + 1)
                      .reduce((sum, level) => sum + level.quantity, 0)
                    const percentage = (bid.quantity / getMaxCumulativeVolume()) * 100
                    
                    return (
                      <div key={index} className="relative">
                        <div
                          className="absolute inset-0 bg-green-100 dark:bg-green-900/20 opacity-50"
                          style={{ width: `${percentage}%` }}
                        />
                        <div className="relative grid grid-cols-3 gap-2 text-sm py-1">
                          <span className="font-medium text-green-600">
                            ${bid.price.toFixed(2)}
                          </span>
                          <span className="text-right">{bid.quantity.toLocaleString()}</span>
                          <span className="text-right text-gray-500">{cumulative.toLocaleString()}</span>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>

              {/* Asks */}
              <div>
                <h3 className="text-sm font-medium text-red-600 mb-2">Asks</h3>
                <div className="space-y-1">
                  <div className="grid grid-cols-3 gap-2 text-xs text-gray-500 pb-1 border-b dark:border-gray-700">
                    <span>Price</span>
                    <span className="text-right">Quantity</span>
                    <span className="text-right">Total</span>
                  </div>
                  {orderBookData.askLevels.map((ask, index) => {
                    const cumulative = orderBookData.askLevels
                      .slice(0, index + 1)
                      .reduce((sum, level) => sum + level.quantity, 0)
                    const percentage = (ask.quantity / getMaxCumulativeVolume()) * 100
                    
                    return (
                      <div key={index} className="relative">
                        <div
                          className="absolute inset-0 bg-red-100 dark:bg-red-900/20 opacity-50"
                          style={{ width: `${percentage}%` }}
                        />
                        <div className="relative grid grid-cols-3 gap-2 text-sm py-1">
                          <span className="font-medium text-red-600">
                            ${ask.price.toFixed(2)}
                          </span>
                          <span className="text-right">{ask.quantity.toLocaleString()}</span>
                          <span className="text-right text-gray-500">{cumulative.toLocaleString()}</span>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            </div>

            {/* Best Bid/Ask Summary */}
            <div className="mt-4 pt-4 border-t dark:border-gray-700">
              <div className="grid grid-cols-2 gap-4">
                <div className="text-center">
                  <p className="text-xs text-gray-500">Best Bid</p>
                  <p className="text-lg font-bold text-green-600">
                    ${orderBookData.bestBid?.toFixed(2) || '-'}
                  </p>
                  <p className="text-xs text-gray-500">×{orderBookData.bestBidSize || '-'}</p>
                </div>
                <div className="text-center">
                  <p className="text-xs text-gray-500">Best Ask</p>
                  <p className="text-lg font-bold text-red-600">
                    ${orderBookData.bestAsk?.toFixed(2) || '-'}
                  </p>
                  <p className="text-xs text-gray-500">×{orderBookData.bestAskSize || '-'}</p>
                </div>
              </div>
            </div>
          </div>

          {/* Depth Visualization */}
          <div className="card">
            <h3 className="text-lg font-semibold mb-4">Market Depth Visualization</h3>
            
            {/* Simple ASCII-style depth chart */}
            <div className="space-y-4">
              <div>
                <h4 className="text-sm font-medium text-gray-600 mb-2">Price Levels Distribution</h4>
                <div className="space-y-1 text-xs">
                  {/* Combine and sort levels for visualization */}
                  {[...orderBookData.askLevels].reverse().map((ask, index) => {
                    const percentage = (ask.quantity / getMaxCumulativeVolume()) * 100
                    return (
                      <div key={`ask-${index}`} className="flex items-center">
                        <span className="w-20 text-right text-red-600">${ask.price.toFixed(2)}</span>
                        <div className="flex-1 mx-2">
                          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded-sm overflow-hidden">
                            <div
                              className="h-full bg-red-500 rounded-sm"
                              style={{ width: `${percentage}%` }}
                            />
                          </div>
                        </div>
                        <span className="w-16 text-left text-gray-600">{ask.quantity}</span>
                      </div>
                    )
                  })}
                  
                  {/* Spread indicator */}
                  <div className="flex items-center py-2">
                    <span className="w-20 text-right text-gray-500 font-medium">SPREAD</span>
                    <div className="flex-1 mx-2 border-t-2 border-dashed border-gray-400"></div>
                    <span className="w-16 text-left text-gray-500">${metrics?.spread.toFixed(2)}</span>
                  </div>
                  
                  {orderBookData.bidLevels.map((bid, index) => {
                    const percentage = (bid.quantity / getMaxCumulativeVolume()) * 100
                    return (
                      <div key={`bid-${index}`} className="flex items-center">
                        <span className="w-20 text-right text-green-600">${bid.price.toFixed(2)}</span>
                        <div className="flex-1 mx-2">
                          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded-sm overflow-hidden">
                            <div
                              className="h-full bg-green-500 rounded-sm"
                              style={{ width: `${percentage}%` }}
                            />
                          </div>
                        </div>
                        <span className="w-16 text-left text-gray-600">{bid.quantity}</span>
                      </div>
                    )
                  })}
                </div>
              </div>

              {/* Volume Summary */}
              <div className="pt-4 border-t dark:border-gray-700">
                <h4 className="text-sm font-medium text-gray-600 mb-2">Volume Summary</h4>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="text-gray-500">Total Bid Volume</p>
                    <p className="font-bold text-green-600">{metrics?.totalBidVolume.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Total Ask Volume</p>
                    <p className="font-bold text-red-600">{metrics?.totalAskVolume.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Weighted Bid Price</p>
                    <p className="font-medium">${metrics?.weightedBidPrice.toFixed(2)}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Weighted Ask Price</p>
                    <p className="font-medium">${metrics?.weightedAskPrice.toFixed(2)}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="card text-center py-12">
          <p className="text-gray-500">Select a symbol to view order book depth</p>
        </div>
      )}
    </div>
  )
}