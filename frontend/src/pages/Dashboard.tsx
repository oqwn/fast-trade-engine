import { useQuery } from '@tanstack/react-query'
import { marketDataApi } from '@/services/api'

export default function Dashboard() {
  const { data: quotes, isLoading } = useQuery({
    queryKey: ['market-quotes'],
    queryFn: marketDataApi.getAllQuotes,
    refetchInterval: 5000, // Refresh every 5 seconds
  })

  if (isLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="text-gray-500">Loading market data...</div>
      </div>
    )
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
        Market Dashboard
      </h1>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {quotes?.map((quote) => (
          <div key={quote.symbol} className="card">
            <div className="flex justify-between items-start mb-4">
              <h3 className="text-lg font-semibold">{quote.symbol}</h3>
              <span className={`text-sm ${quote.change && quote.change > 0 ? 'price-up' : 'price-down'}`}>
                {quote.changePercent?.toFixed(2)}%
              </span>
            </div>
            
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-gray-500">Last Price</span>
                <span className="font-medium">${quote.lastPrice?.toFixed(2)}</span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-gray-500">Bid/Ask</span>
                <span className="text-sm">
                  ${quote.bidPrice?.toFixed(2)} / ${quote.askPrice?.toFixed(2)}
                </span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-gray-500">Volume</span>
                <span className="text-sm">{quote.volume?.toLocaleString()}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}