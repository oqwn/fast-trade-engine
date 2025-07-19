import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { accountApi, marketDataApi } from '@/services/api'
import { Position, MarketData } from '@/types'
import { 
  ChartPieIcon, 
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon,
  CurrencyDollarIcon,
  ScaleIcon,
  ArrowPathIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon
} from '@heroicons/react/24/outline'

export default function Positions() {
  const [accountId, setAccountId] = useState<string>('')
  const [showClosedPositions, setShowClosedPositions] = useState(false)

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

  // Fetch positions
  const { data: positions, isLoading: positionsLoading, refetch } = useQuery({
    queryKey: ['positions', accountId],
    queryFn: () => accountApi.getPositions(accountId),
    refetchInterval: 5000, // Update every 5 seconds
  })

  // Fetch market data for all positions
  const { data: marketQuotes } = useQuery({
    queryKey: ['market-quotes'],
    queryFn: marketDataApi.getAllQuotes,
    refetchInterval: 2000,
  })

  // Calculate position metrics
  const calculateMetrics = (position: Position, marketData?: MarketData) => {
    const currentPrice = marketData?.lastPrice || position.currentPrice || position.averagePrice
    const marketValue = position.quantity * currentPrice
    const costBasis = position.quantity * position.averagePrice
    const unrealizedPnL = marketValue - costBasis
    const unrealizedPnLPercent = (unrealizedPnL / costBasis) * 100
    const totalPnL = unrealizedPnL + position.realizedPnL

    return {
      currentPrice,
      marketValue,
      costBasis,
      unrealizedPnL,
      unrealizedPnLPercent,
      totalPnL,
    }
  }

  // Calculate portfolio summary
  const portfolioSummary = positions?.reduce((acc, position) => {
    const marketData = marketQuotes?.find(q => q.symbol === position.symbol)
    const metrics = calculateMetrics(position, marketData)
    
    acc.totalMarketValue += metrics.marketValue
    acc.totalCostBasis += metrics.costBasis
    acc.totalUnrealizedPnL += metrics.unrealizedPnL
    acc.totalRealizedPnL += position.realizedPnL
    
    return acc
  }, {
    totalMarketValue: 0,
    totalCostBasis: 0,
    totalUnrealizedPnL: 0,
    totalRealizedPnL: 0,
  }) || {
    totalMarketValue: 0,
    totalCostBasis: 0,
    totalUnrealizedPnL: 0,
    totalRealizedPnL: 0,
  }

  const totalPnL = portfolioSummary.totalUnrealizedPnL + portfolioSummary.totalRealizedPnL
  const totalPnLPercent = portfolioSummary.totalCostBasis > 0 
    ? (totalPnL / portfolioSummary.totalCostBasis) * 100 
    : 0

  // Filter positions based on showClosedPositions
  const displayPositions = positions?.filter(p => 
    showClosedPositions || p.side !== 'FLAT'
  )

  if (positionsLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="text-gray-500">Loading positions...</div>
      </div>
    )
  }

  return (
    <div>
      <div className="mb-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Portfolio Positions
          </h1>
          <div className="flex items-center space-x-4">
            {/* Account Selector */}
            <div className="flex items-center space-x-2">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Account:
              </label>
              <select
                value={accountId}
                onChange={(e) => setAccountId(e.target.value)}
                className="input"
              >
                <option value="">Select account</option>
                {availableAccounts?.map((id) => (
                  <option key={id} value={id}>
                    {id}
                  </option>
                ))}
              </select>
            </div>
            <button
              onClick={() => refetch()}
              className="btn-secondary flex items-center"
            >
              <ArrowPathIcon className="h-4 w-4 mr-2" />
              Refresh
            </button>
          </div>
        </div>

        {/* Portfolio Summary */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <div className="card">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-gray-600 dark:text-gray-400">Market Value</span>
              <CurrencyDollarIcon className="h-5 w-5 text-gray-400" />
            </div>
            <p className="text-2xl font-bold">${portfolioSummary.totalMarketValue.toFixed(2)}</p>
          </div>

          <div className="card">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-gray-600 dark:text-gray-400">Cost Basis</span>
              <ScaleIcon className="h-5 w-5 text-gray-400" />
            </div>
            <p className="text-2xl font-bold">${portfolioSummary.totalCostBasis.toFixed(2)}</p>
          </div>

          <div className="card">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-gray-600 dark:text-gray-400">Unrealized P&L</span>
              {portfolioSummary.totalUnrealizedPnL >= 0 ? (
                <ArrowTrendingUpIcon className="h-5 w-5 text-green-500" />
              ) : (
                <ArrowTrendingDownIcon className="h-5 w-5 text-red-500" />
              )}
            </div>
            <p className={`text-2xl font-bold ${
              portfolioSummary.totalUnrealizedPnL >= 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              {portfolioSummary.totalUnrealizedPnL >= 0 ? '+' : ''}
              ${portfolioSummary.totalUnrealizedPnL.toFixed(2)}
            </p>
            <p className={`text-sm ${
              portfolioSummary.totalUnrealizedPnL >= 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              ({portfolioSummary.totalCostBasis > 0 
                ? ((portfolioSummary.totalUnrealizedPnL / portfolioSummary.totalCostBasis) * 100).toFixed(2) 
                : '0.00'}%)
            </p>
          </div>

          <div className="card">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-gray-600 dark:text-gray-400">Total P&L</span>
              <ChartPieIcon className="h-5 w-5 text-gray-400" />
            </div>
            <p className={`text-2xl font-bold ${
              totalPnL >= 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              {totalPnL >= 0 ? '+' : ''}${totalPnL.toFixed(2)}
            </p>
            <p className={`text-sm ${
              totalPnL >= 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              ({totalPnLPercent.toFixed(2)}%)
            </p>
          </div>
        </div>

        {/* Filter */}
        <div className="flex items-center mb-4">
          <label className="flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={showClosedPositions}
              onChange={(e) => setShowClosedPositions(e.target.checked)}
              className="mr-2 rounded border-gray-300 text-blue-500 focus:ring-blue-500"
            />
            <span className="text-sm text-gray-700 dark:text-gray-300">
              Show closed positions
            </span>
          </label>
        </div>
      </div>

      {/* Positions Table */}
      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-800">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Symbol
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Side
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Quantity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Avg Price
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Current Price
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Market Value
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Cost Basis
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Unrealized P&L
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Realized P&L
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Total P&L
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
              {displayPositions?.length === 0 ? (
                <tr>
                  <td colSpan={10} className="px-6 py-4 text-center text-gray-500">
                    No positions found
                  </td>
                </tr>
              ) : (
                displayPositions?.map((position) => {
                  const marketData = marketQuotes?.find(q => q.symbol === position.symbol)
                  const metrics = calculateMetrics(position, marketData)
                  
                  return (
                    <tr key={position.symbol} className="hover:bg-gray-50 dark:hover:bg-gray-800">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <span className="font-medium text-gray-900 dark:text-gray-100">
                            {position.symbol}
                          </span>
                          {position.side === 'FLAT' && (
                            <CheckCircleIcon className="h-4 w-4 ml-2 text-gray-400" />
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`font-medium ${
                          position.side === 'LONG' ? 'text-green-600' : 
                          position.side === 'SHORT' ? 'text-red-600' : 
                          'text-gray-500'
                        }`}>
                          {position.side}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-gray-100">
                        {position.quantity.toLocaleString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-gray-100">
                        ${position.averagePrice.toFixed(2)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        <div className="flex items-center">
                          <span className="text-gray-900 dark:text-gray-100">
                            ${metrics.currentPrice.toFixed(2)}
                          </span>
                          {marketData && (
                            <span className={`ml-2 text-xs ${
                              (marketData.change || 0) >= 0 ? 'text-green-500' : 'text-red-500'
                            }`}>
                              {(marketData.change || 0) >= 0 ? '+' : ''}{marketData.changePercent?.toFixed(2)}%
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-gray-100">
                        ${metrics.marketValue.toFixed(2)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-gray-100">
                        ${metrics.costBasis.toFixed(2)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        <div className={`${metrics.unrealizedPnL >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                          <span className="font-medium">
                            {metrics.unrealizedPnL >= 0 ? '+' : ''}${metrics.unrealizedPnL.toFixed(2)}
                          </span>
                          <span className="text-xs ml-1">
                            ({metrics.unrealizedPnLPercent.toFixed(2)}%)
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        <span className={`font-medium ${
                          position.realizedPnL >= 0 ? 'text-green-600' : 'text-red-600'
                        }`}>
                          {position.realizedPnL >= 0 ? '+' : ''}${position.realizedPnL.toFixed(2)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        <div className={`font-bold ${metrics.totalPnL >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                          {metrics.totalPnL >= 0 ? '+' : ''}${metrics.totalPnL.toFixed(2)}
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Risk Warning */}
      {portfolioSummary.totalUnrealizedPnL < -portfolioSummary.totalCostBasis * 0.1 && (
        <div className="mt-6 card bg-yellow-50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-800">
          <div className="flex items-start">
            <ExclamationTriangleIcon className="h-5 w-5 text-yellow-600 mt-0.5" />
            <div className="ml-3">
              <h3 className="text-sm font-semibold text-yellow-800 dark:text-yellow-200">
                Risk Alert
              </h3>
              <p className="text-sm text-yellow-700 dark:text-yellow-300 mt-1">
                Your portfolio is down more than 10%. Consider reviewing your positions and risk management strategy.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}