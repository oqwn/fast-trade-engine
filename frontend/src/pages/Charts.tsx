import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { marketDataApi } from '@/services/api'
import { Line } from 'react-chartjs-2'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ChartOptions,
} from 'chart.js'
import { 
  ChartBarIcon, 
  ArrowPathIcon,
  ClockIcon,
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon,
  CurrencyDollarIcon
} from '@heroicons/react/24/outline'

// Register ChartJS components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
)

export default function Charts() {
  const [selectedSymbol, setSelectedSymbol] = useState<string>('')
  const [timeframe, setTimeframe] = useState<string>('1m')
  const [periods, setPeriods] = useState<number>(50)
  const [showVolume, setShowVolume] = useState(true)
  const [showMA, setShowMA] = useState(true)

  // Fetch available symbols
  const { data: symbols } = useQuery({
    queryKey: ['available-symbols'],
    queryFn: marketDataApi.getAvailableSymbols,
  })

  // Fetch OHLC data
  const { data: ohlcData, isLoading, refetch } = useQuery({
    queryKey: ['ohlc', selectedSymbol, timeframe, periods],
    queryFn: () => marketDataApi.getOHLC(selectedSymbol, timeframe, periods),
    enabled: !!selectedSymbol,
    refetchInterval: 5000, // Update every 5 seconds
  })

  // Fetch current market data
  const { data: marketData } = useQuery({
    queryKey: ['market-quote', selectedSymbol],
    queryFn: () => marketDataApi.getQuote(selectedSymbol),
    enabled: !!selectedSymbol,
    refetchInterval: 2000,
  })

  // Calculate moving average
  const calculateMA = (data: number[], period: number) => {
    const ma = []
    for (let i = 0; i < data.length; i++) {
      if (i < period - 1) {
        ma.push(null)
      } else {
        const sum = data.slice(i - period + 1, i + 1).reduce((a, b) => a + b, 0)
        ma.push(sum / period)
      }
    }
    return ma
  }

  // Prepare chart data
  const prepareChartData = () => {
    if (!ohlcData?.data || ohlcData.data.length === 0) {
      return null
    }

    const labels = ohlcData.data.map(candle => 
      new Date(candle.timestamp).toLocaleTimeString()
    )
    const prices = ohlcData.data.map(candle => candle.close)
    const volumes = ohlcData.data.map(candle => candle.volume)

    const datasets = [
      {
        label: `${selectedSymbol} Price`,
        data: prices,
        borderColor: 'rgb(59, 130, 246)',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        tension: 0.1,
        pointRadius: 0,
        pointHoverRadius: 5,
      }
    ]

    if (showMA) {
      const ma20 = calculateMA(prices, Math.min(20, prices.length))
      datasets.push({
        label: 'MA 20',
        data: ma20 as any,
        borderColor: 'rgb(234, 179, 8)',
        backgroundColor: 'transparent',
        tension: 0.1,
        pointRadius: 0,
        pointHoverRadius: 0,
        borderDash: [5, 5],
      } as any)
    }

    return {
      labels,
      datasets,
      volumes,
    }
  }

  const chartData = prepareChartData()

  const chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top' as const,
        labels: {
          color: '#9CA3AF',
        }
      },
      title: {
        display: false,
      },
      tooltip: {
        mode: 'index',
        intersect: false,
        callbacks: {
          label: (context) => {
            if (context.dataset.label?.includes('Price')) {
              return `${context.dataset.label}: $${context.parsed.y.toFixed(2)}`
            }
            return `${context.dataset.label}: ${context.parsed.y?.toFixed(2) || '-'}`
          }
        }
      },
    },
    scales: {
      x: {
        grid: {
          color: 'rgba(156, 163, 175, 0.1)',
        },
        ticks: {
          color: '#9CA3AF',
          maxRotation: 45,
          minRotation: 45,
        }
      },
      y: {
        position: 'right',
        grid: {
          color: 'rgba(156, 163, 175, 0.1)',
        },
        ticks: {
          color: '#9CA3AF',
          callback: function(value) {
            return '$' + value
          }
        }
      }
    },
    interaction: {
      mode: 'index' as const,
      intersect: false,
    },
  }

  // Calculate price statistics
  const calculateStats = () => {
    if (!ohlcData?.data || ohlcData.data.length === 0) return null

    const prices = ohlcData.data.map(c => c.close)
    const high = Math.max(...ohlcData.data.map(c => c.high))
    const low = Math.min(...ohlcData.data.map(c => c.low))
    const firstPrice = prices[0]
    const lastPrice = prices[prices.length - 1]
    const change = lastPrice - firstPrice
    const changePercent = (change / firstPrice) * 100
    const totalVolume = ohlcData.data.reduce((sum, c) => sum + c.volume, 0)

    return {
      high,
      low,
      change,
      changePercent,
      totalVolume,
      candleCount: ohlcData.data.length,
    }
  }

  const stats = calculateStats()

  return (
    <div>
      <div className="mb-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Price Charts
          </h1>
          <button
            onClick={() => refetch()}
            className="btn-secondary flex items-center"
          >
            <ArrowPathIcon className="h-4 w-4 mr-2" />
            Refresh
          </button>
        </div>

        {/* Controls */}
        <div className="card mb-6">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
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
                Timeframe
              </label>
              <select
                value={timeframe}
                onChange={(e) => setTimeframe(e.target.value)}
                className="input w-full"
              >
                <option value="1m">1 Minute</option>
                <option value="5m">5 Minutes</option>
                <option value="15m">15 Minutes</option>
                <option value="30m">30 Minutes</option>
                <option value="1h">1 Hour</option>
                <option value="4h">4 Hours</option>
                <option value="1d">1 Day</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Periods
              </label>
              <select
                value={periods}
                onChange={(e) => setPeriods(Number(e.target.value))}
                className="input w-full"
              >
                <option value={20}>20</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
                <option value={200}>200</option>
              </select>
            </div>

            <div className="flex items-end space-x-4">
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={showMA}
                  onChange={(e) => setShowMA(e.target.checked)}
                  className="mr-2 rounded border-gray-300 text-blue-500 focus:ring-blue-500"
                />
                <span className="text-sm text-gray-700 dark:text-gray-300">
                  Show MA
                </span>
              </label>
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={showVolume}
                  onChange={(e) => setShowVolume(e.target.checked)}
                  className="mr-2 rounded border-gray-300 text-blue-500 focus:ring-blue-500"
                />
                <span className="text-sm text-gray-700 dark:text-gray-300">
                  Volume
                </span>
              </label>
            </div>
          </div>
        </div>

        {/* Market Info */}
        {selectedSymbol && marketData && stats && (
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4 mb-6">
            <div className="card">
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs text-gray-600 dark:text-gray-400">Current Price</span>
                <CurrencyDollarIcon className="h-4 w-4 text-gray-400" />
              </div>
              <p className="text-lg font-bold">${marketData.lastPrice?.toFixed(2)}</p>
              <p className={`text-xs ${
                (marketData.change || 0) >= 0 ? 'text-green-500' : 'text-red-500'
              }`}>
                {(marketData.change || 0) >= 0 ? '+' : ''}{marketData.changePercent?.toFixed(2)}%
              </p>
            </div>

            <div className="card">
              <p className="text-xs text-gray-600 dark:text-gray-400 mb-1">Period High</p>
              <p className="text-lg font-bold">${stats.high.toFixed(2)}</p>
            </div>

            <div className="card">
              <p className="text-xs text-gray-600 dark:text-gray-400 mb-1">Period Low</p>
              <p className="text-lg font-bold">${stats.low.toFixed(2)}</p>
            </div>

            <div className="card">
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs text-gray-600 dark:text-gray-400">Period Change</span>
                {stats.change >= 0 ? (
                  <ArrowTrendingUpIcon className="h-4 w-4 text-green-500" />
                ) : (
                  <ArrowTrendingDownIcon className="h-4 w-4 text-red-500" />
                )}
              </div>
              <p className={`text-lg font-bold ${
                stats.change >= 0 ? 'text-green-600' : 'text-red-600'
              }`}>
                {stats.changePercent.toFixed(2)}%
              </p>
            </div>

            <div className="card">
              <p className="text-xs text-gray-600 dark:text-gray-400 mb-1">Period Volume</p>
              <p className="text-lg font-bold">{(stats.totalVolume / 1000000).toFixed(1)}M</p>
            </div>

            <div className="card">
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs text-gray-600 dark:text-gray-400">Data Points</span>
                <ClockIcon className="h-4 w-4 text-gray-400" />
              </div>
              <p className="text-lg font-bold">{stats.candleCount}</p>
            </div>
          </div>
        )}
      </div>

      {/* Chart Display */}
      {selectedSymbol ? (
        <div className="space-y-6">
          {/* Price Chart */}
          <div className="card">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <ChartBarIcon className="h-5 w-5 mr-2" />
              {selectedSymbol} - {timeframe} Chart
            </h2>
            
            {isLoading ? (
              <div className="h-96 flex items-center justify-center">
                <div className="text-gray-500">Loading chart data...</div>
              </div>
            ) : chartData ? (
              <div className="h-96">
                <Line data={{
                  labels: chartData.labels,
                  datasets: chartData.datasets
                }} options={chartOptions} />
              </div>
            ) : (
              <div className="h-96 flex items-center justify-center">
                <div className="text-gray-500">No data available</div>
              </div>
            )}
          </div>

          {/* Volume Chart */}
          {showVolume && chartData && (
            <div className="card">
              <h3 className="text-lg font-semibold mb-4">Volume</h3>
              <div className="h-48">
                <Line 
                  data={{
                    labels: chartData.labels,
                    datasets: [{
                      label: 'Volume',
                      data: chartData.volumes,
                      backgroundColor: 'rgba(59, 130, 246, 0.5)',
                      borderColor: 'rgb(59, 130, 246)',
                      borderWidth: 1,
                      type: 'bar' as any,
                    }]
                  }} 
                  options={{
                    ...chartOptions,
                    scales: {
                      ...chartOptions.scales,
                      y: {
                        ...chartOptions.scales?.y,
                        ticks: {
                          ...chartOptions.scales?.y?.ticks,
                          callback: function(value) {
                            return (Number(value) / 1000000).toFixed(1) + 'M'
                          }
                        }
                      }
                    }
                  }} 
                />
              </div>
            </div>
          )}

          {/* Technical Analysis Info */}
          <div className="card bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
            <h3 className="text-sm font-semibold text-blue-900 dark:text-blue-100 mb-2">
              Technical Indicators
            </h3>
            <div className="text-xs text-blue-800 dark:text-blue-200 space-y-1">
              <p>• MA 20: Moving Average over 20 periods - helps identify trends</p>
              <p>• Volume bars show trading activity for each period</p>
              <p>• Higher timeframes show longer-term trends</p>
              <p>• Use multiple timeframes for better analysis</p>
            </div>
          </div>
        </div>
      ) : (
        <div className="card text-center py-12">
          <ChartBarIcon className="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <p className="text-gray-500">Select a symbol to view price charts</p>
        </div>
      )}
    </div>
  )
}