import { useQuery } from '@tanstack/react-query'
import { marketDataApi } from '@/services/api'
import { ArrowUpIcon, ArrowDownIcon, ClockIcon, ChartBarIcon } from '@heroicons/react/24/outline'
import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

export default function Dashboard() {
  const navigate = useNavigate()
  const [currentTime, setCurrentTime] = useState(new Date())

  const { data: quotes, isLoading } = useQuery({
    queryKey: ['market-quotes'],
    queryFn: marketDataApi.getAllQuotes,
    refetchInterval: 1000, // Refresh every second for real-time feel
  })

  const { data: symbols } = useQuery({
    queryKey: ['available-symbols'],
    queryFn: marketDataApi.getAvailableSymbols,
  })

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date())
    }, 1000)
    return () => clearInterval(timer)
  }, [])

  if (isLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="text-gray-500">Loading market data...</div>
      </div>
    )
  }

  return (
    <div>
      <div className="mb-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Market Dashboard
          </h1>
          <div className="flex items-center text-sm text-gray-500">
            <ClockIcon className="h-4 w-4 mr-1" />
            {currentTime.toLocaleTimeString()}
          </div>
        </div>
        
        {/* Market Summary */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="bg-gradient-to-r from-green-50 to-green-100 dark:from-green-900/20 dark:to-green-800/20 rounded-lg p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Market Status</p>
                <p className="text-2xl font-bold text-green-600 dark:text-green-400">OPEN</p>
              </div>
              <ChartBarIcon className="h-8 w-8 text-green-600 dark:text-green-400" />
            </div>
          </div>
          
          <div className="bg-gradient-to-r from-blue-50 to-blue-100 dark:from-blue-900/20 dark:to-blue-800/20 rounded-lg p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Active Symbols</p>
                <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">{symbols?.length || 0}</p>
              </div>
              <div className="text-blue-600 dark:text-blue-400">
                <svg className="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h10M7 12h10m-7 5h4" />
                </svg>
              </div>
            </div>
          </div>
          
          <div className="bg-gradient-to-r from-purple-50 to-purple-100 dark:from-purple-900/20 dark:to-purple-800/20 rounded-lg p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Total Volume</p>
                <p className="text-2xl font-bold text-purple-600 dark:text-purple-400">
                  {quotes?.reduce((sum, q) => sum + (q.volume || 0), 0).toLocaleString()}
                </p>
              </div>
              <div className="text-purple-600 dark:text-purple-400">
                <svg className="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>

      <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Market Quotes</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {quotes?.map((quote) => (
          <div 
            key={quote.symbol} 
            className="card hover:shadow-lg transition-shadow cursor-pointer"
            onClick={() => {
              navigate(`/trading/${quote.symbol}`)
            }}
          >
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-lg font-semibold">{quote.symbol}</h3>
                <p className="text-xs text-gray-500">Click to trade</p>
              </div>
              <div className="flex items-center">
                {quote.change && quote.change > 0 ? (
                  <ArrowUpIcon className="h-4 w-4 text-green-500 mr-1" />
                ) : (
                  <ArrowDownIcon className="h-4 w-4 text-red-500 mr-1" />
                )}
                <span className={`text-sm font-medium ${quote.change && quote.change > 0 ? 'text-green-500' : 'text-red-500'}`}>
                  {quote.changePercent?.toFixed(2)}%
                </span>
              </div>
            </div>
            
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-gray-500 text-sm">Last Price</span>
                <span className="font-bold text-xl">${quote.lastPrice?.toFixed(2)}</span>
              </div>
              
              <div className="grid grid-cols-2 gap-2 text-sm">
                <div className="bg-green-50 dark:bg-green-900/20 rounded p-2">
                  <p className="text-xs text-gray-600 dark:text-gray-400">Bid</p>
                  <p className="font-semibold text-green-600 dark:text-green-400">
                    ${quote.bidPrice?.toFixed(2)}
                  </p>
                  <p className="text-xs text-gray-500">×{quote.bidSize || '100'}</p>
                </div>
                <div className="bg-red-50 dark:bg-red-900/20 rounded p-2">
                  <p className="text-xs text-gray-600 dark:text-gray-400">Ask</p>
                  <p className="font-semibold text-red-600 dark:text-red-400">
                    ${quote.askPrice?.toFixed(2)}
                  </p>
                  <p className="text-xs text-gray-500">×{quote.askSize || '100'}</p>
                </div>
              </div>
              
              <div className="pt-2 border-t dark:border-gray-700">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Spread</span>
                  <span className="font-medium">
                    ${((quote.askPrice || 0) - (quote.bidPrice || 0)).toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between text-sm mt-1">
                  <span className="text-gray-500">Volume</span>
                  <span className="font-medium">{quote.volume?.toLocaleString()}</span>
                </div>
                <div className="flex justify-between text-sm mt-1">
                  <span className="text-gray-500">Day Range</span>
                  <span className="font-medium text-xs">
                    ${quote.lowPrice?.toFixed(2)} - ${quote.highPrice?.toFixed(2)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}