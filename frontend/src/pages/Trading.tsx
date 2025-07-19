import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { marketDataApi, orderApi, accountApi } from '@/services/api'
import { OrderRequest } from '@/types'
import toast from 'react-hot-toast'
import { 
  CurrencyDollarIcon, 
  ChartBarIcon, 
  ArrowTrendingUpIcon, 
  ArrowTrendingDownIcon,
  BanknotesIcon,
  ShieldCheckIcon,
  ExclamationTriangleIcon
} from '@heroicons/react/24/outline'

export default function Trading() {
  const [searchParams] = useSearchParams()
  const queryClient = useQueryClient()
  
  // Order form state
  const [selectedSymbol, setSelectedSymbol] = useState(searchParams.get('symbol') || '')
  const [orderType, setOrderType] = useState<'MARKET' | 'LIMIT' | 'STOP'>('MARKET')
  const [orderSide, setOrderSide] = useState<'BUY' | 'SELL'>('BUY')
  const [quantity, setQuantity] = useState('')
  const [limitPrice, setLimitPrice] = useState('')
  const [stopPrice, setStopPrice] = useState('')
  const [accountId, setAccountId] = useState<string>('')

  // Fetch available symbols
  const { data: symbols } = useQuery({
    queryKey: ['available-symbols'],
    queryFn: marketDataApi.getAvailableSymbols,
  })

  // Fetch available accounts
  const { data: availableAccounts } = useQuery({
    queryKey: ['available-accounts'],
    queryFn: accountApi.getAllAccountIds,
  })

  // Set first available account as default
  useEffect(() => {
    if (availableAccounts && availableAccounts.length > 0 && !accountId) {
      setAccountId(availableAccounts[0])
    }
  }, [availableAccounts, accountId])

  // Fetch market data for selected symbol
  const { data: marketData } = useQuery({
    queryKey: ['market-quote', selectedSymbol],
    queryFn: () => marketDataApi.getQuote(selectedSymbol),
    enabled: !!selectedSymbol,
    refetchInterval: 2000,
  })

  // Place order mutation
  const placeOrderMutation = useMutation({
    mutationFn: (orderRequest: OrderRequest) => orderApi.placeOrder(orderRequest),
    onSuccess: () => {
      toast.success('Order placed successfully!')
      // Reset form
      setQuantity('')
      setLimitPrice('')
      setStopPrice('')
      // Invalidate orders query
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to place order')
    },
  })

  // Auto-populate limit price with current market price
  useEffect(() => {
    if (marketData && orderType === 'LIMIT' && !limitPrice) {
      const referencePrice = orderSide === 'BUY' 
        ? marketData.askPrice 
        : marketData.bidPrice
      setLimitPrice(referencePrice?.toFixed(2) || '')
    }
  }, [marketData, orderType, orderSide, limitPrice])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!selectedSymbol || !quantity) {
      toast.error('Please fill all required fields')
      return
    }

    const orderRequest: OrderRequest = {
      accountId,
      symbol: selectedSymbol,
      side: orderSide,
      type: orderType,
      quantity: parseInt(quantity),
    }

    if (orderType === 'LIMIT' && limitPrice) {
      orderRequest.price = parseFloat(limitPrice)
    }

    if (orderType === 'STOP' && stopPrice) {
      orderRequest.stopPrice = parseFloat(stopPrice)
    }

    // Note: Stop orders would need backend support
    if (orderType === 'STOP' && stopPrice) {
      toast.error('Stop orders are not yet supported by the backend')
      return
    }

    placeOrderMutation.mutate(orderRequest)
  }

  const estimatedCost = () => {
    if (!quantity) return 0
    const qty = parseInt(quantity) || 0
    
    if (orderType === 'MARKET' && marketData) {
      const price = orderSide === 'BUY' ? marketData.askPrice : marketData.bidPrice
      return qty * (price || 0)
    } else if (orderType === 'LIMIT' && limitPrice) {
      return qty * parseFloat(limitPrice)
    }
    return 0
  }

  return (
    <div className="max-w-7xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
        Trading Terminal
      </h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Order Entry Form */}
        <div className="lg:col-span-2">
          <div className="card">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <CurrencyDollarIcon className="h-5 w-5 mr-2" />
              Place Order
            </h2>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Account Selection */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Trading Account
                </label>
                <select
                  value={accountId}
                  onChange={(e) => setAccountId(e.target.value)}
                  className="input w-full"
                  required
                >
                  <option value="">Select an account</option>
                  {availableAccounts?.map((id) => (
                    <option key={id} value={id}>
                      {id}
                    </option>
                  ))}
                </select>
              </div>

              {/* Symbol Selection */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Symbol
                </label>
                <select
                  value={selectedSymbol}
                  onChange={(e) => setSelectedSymbol(e.target.value)}
                  className="input w-full"
                  required
                >
                  <option value="">Select a symbol</option>
                  {symbols?.map((symbol) => (
                    <option key={symbol} value={symbol}>
                      {symbol}
                    </option>
                  ))}
                </select>
              </div>

              {/* Order Side */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Side
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setOrderSide('BUY')}
                    className={`py-3 px-4 rounded-lg font-medium transition-colors flex items-center justify-center ${
                      orderSide === 'BUY'
                        ? 'bg-green-500 text-white'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                    }`}
                  >
                    <ArrowTrendingUpIcon className="h-5 w-5 mr-2" />
                    Buy
                  </button>
                  <button
                    type="button"
                    onClick={() => setOrderSide('SELL')}
                    className={`py-3 px-4 rounded-lg font-medium transition-colors flex items-center justify-center ${
                      orderSide === 'SELL'
                        ? 'bg-red-500 text-white'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                    }`}
                  >
                    <ArrowTrendingDownIcon className="h-5 w-5 mr-2" />
                    Sell
                  </button>
                </div>
              </div>

              {/* Order Type */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Order Type
                </label>
                <div className="grid grid-cols-3 gap-2">
                  <button
                    type="button"
                    onClick={() => setOrderType('MARKET')}
                    className={`py-2 px-3 rounded-lg text-sm font-medium transition-colors ${
                      orderType === 'MARKET'
                        ? 'bg-blue-500 text-white'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                    }`}
                  >
                    Market
                  </button>
                  <button
                    type="button"
                    onClick={() => setOrderType('LIMIT')}
                    className={`py-2 px-3 rounded-lg text-sm font-medium transition-colors ${
                      orderType === 'LIMIT'
                        ? 'bg-blue-500 text-white'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                    }`}
                  >
                    Limit
                  </button>
                  <button
                    type="button"
                    onClick={() => setOrderType('STOP')}
                    className={`py-2 px-3 rounded-lg text-sm font-medium transition-colors ${
                      orderType === 'STOP'
                        ? 'bg-blue-500 text-white'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                    }`}
                  >
                    Stop
                  </button>
                </div>
              </div>

              {/* Quantity */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Quantity
                </label>
                <input
                  type="number"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  className="input w-full"
                  placeholder="Enter quantity"
                  min="1"
                  step="100"
                  required
                />
                <p className="text-xs text-gray-500 mt-1">Orders must be in multiples of 100</p>
              </div>

              {/* Limit Price (for limit orders) */}
              {orderType === 'LIMIT' && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Limit Price
                  </label>
                  <input
                    type="number"
                    value={limitPrice}
                    onChange={(e) => setLimitPrice(e.target.value)}
                    className="input w-full"
                    placeholder="Enter limit price"
                    min="0.01"
                    step="0.01"
                    required
                  />
                </div>
              )}

              {/* Stop Price (for stop orders) */}
              {orderType === 'STOP' && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Stop Price
                  </label>
                  <input
                    type="number"
                    value={stopPrice}
                    onChange={(e) => setStopPrice(e.target.value)}
                    className="input w-full"
                    placeholder="Enter stop price"
                    min="0.01"
                    step="0.01"
                    required
                  />
                  <p className="text-xs text-yellow-600 mt-1 flex items-center">
                    <ExclamationTriangleIcon className="h-3 w-3 mr-1" />
                    Stop orders are not yet supported by the backend
                  </p>
                </div>
              )}

              {/* Order Summary */}
              <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 space-y-2">
                <h3 className="font-medium text-sm text-gray-900 dark:text-white">Order Summary</h3>
                <div className="text-sm space-y-1">
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Estimated Cost:</span>
                    <span className="font-medium">${estimatedCost().toFixed(2)}</span>
                  </div>
                  {orderType === 'MARKET' && marketData && (
                    <div className="flex justify-between">
                      <span className="text-gray-600 dark:text-gray-400">
                        Est. Price ({orderSide}):
                      </span>
                      <span className="font-medium">
                        ${orderSide === 'BUY' 
                          ? marketData.askPrice?.toFixed(2) 
                          : marketData.bidPrice?.toFixed(2)}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={placeOrderMutation.isPending}
                className={`btn-primary w-full flex items-center justify-center ${
                  orderSide === 'BUY' ? 'bg-green-500 hover:bg-green-600' : 'bg-red-500 hover:bg-red-600'
                }`}
              >
                {placeOrderMutation.isPending ? (
                  'Placing Order...'
                ) : (
                  <>
                    <ShieldCheckIcon className="h-5 w-5 mr-2" />
                    Place {orderSide} Order
                  </>
                )}
              </button>
            </form>
          </div>
        </div>

        {/* Market Info Panel */}
        <div className="space-y-6">
          {/* Current Market Data */}
          {selectedSymbol && marketData && (
            <div className="card">
              <h3 className="text-lg font-semibold mb-4 flex items-center">
                <ChartBarIcon className="h-5 w-5 mr-2" />
                {selectedSymbol} Market Data
              </h3>
              
              <div className="space-y-3">
                <div>
                  <p className="text-sm text-gray-600 dark:text-gray-400">Last Price</p>
                  <p className="text-2xl font-bold">${marketData.lastPrice?.toFixed(2)}</p>
                  <p className={`text-sm ${
                    (marketData.change || 0) >= 0 ? 'text-green-500' : 'text-red-500'
                  }`}>
                    {(marketData.change || 0) >= 0 ? '+' : ''}{marketData.change?.toFixed(2)} 
                    ({marketData.changePercent?.toFixed(2)}%)
                  </p>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-green-50 dark:bg-green-900/20 rounded-lg p-3">
                    <p className="text-xs text-gray-600 dark:text-gray-400">Bid</p>
                    <p className="font-semibold text-green-600 dark:text-green-400">
                      ${marketData.bidPrice?.toFixed(2)}
                    </p>
                    <p className="text-xs text-gray-500">×{marketData.bidSize || '100'}</p>
                  </div>
                  <div className="bg-red-50 dark:bg-red-900/20 rounded-lg p-3">
                    <p className="text-xs text-gray-600 dark:text-gray-400">Ask</p>
                    <p className="font-semibold text-red-600 dark:text-red-400">
                      ${marketData.askPrice?.toFixed(2)}
                    </p>
                    <p className="text-xs text-gray-500">×{marketData.askSize || '100'}</p>
                  </div>
                </div>

                <div className="pt-3 border-t dark:border-gray-700 space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Day High</span>
                    <span className="font-medium">${marketData.highPrice?.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Day Low</span>
                    <span className="font-medium">${marketData.lowPrice?.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Volume</span>
                    <span className="font-medium">{marketData.volume?.toLocaleString()}</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Trading Tips */}
          <div className="card bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
            <h3 className="text-sm font-semibold text-blue-900 dark:text-blue-100 mb-2">
              Trading Tips
            </h3>
            <ul className="text-xs text-blue-800 dark:text-blue-200 space-y-1">
              <li>• Market orders execute immediately at best available price</li>
              <li>• Limit orders execute only at your specified price or better</li>
              <li>• All orders must be in multiples of 100 shares</li>
              <li>• Check the bid/ask spread before placing orders</li>
            </ul>
          </div>

          {/* Account Info - Placeholder */}
          <div className="card">
            <h3 className="text-sm font-semibold mb-2 flex items-center">
              <BanknotesIcon className="h-4 w-4 mr-2" />
              Account Balance
            </h3>
            <p className="text-2xl font-bold">$100,000.00</p>
            <p className="text-sm text-gray-600 dark:text-gray-400">Demo Account</p>
          </div>
        </div>
      </div>
    </div>
  )
}