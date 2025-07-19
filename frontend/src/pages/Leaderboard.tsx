import { useState } from 'react'
import { 
  TrophyIcon, 
  FireIcon,
  SparklesIcon,
  ArrowUpIcon,
  ArrowDownIcon,
  ClockIcon,
  UserGroupIcon,
  ChartBarIcon,
  ArrowPathIcon
} from '@heroicons/react/24/outline'

// Mock data for leaderboard
const mockLeaderboardData = [
  {
    rank: 1,
    previousRank: 2,
    username: 'AlphaTrader',
    totalPnL: 45623.50,
    pnlPercent: 45.62,
    winRate: 78.5,
    totalTrades: 342,
    sharpeRatio: 2.45,
    streak: 12,
    avatar: '🚀'
  },
  {
    rank: 2,
    previousRank: 1,
    username: 'QuantMaster',
    totalPnL: 38912.30,
    pnlPercent: 38.91,
    winRate: 72.3,
    totalTrades: 289,
    sharpeRatio: 2.12,
    streak: 8,
    avatar: '🧠'
  },
  {
    rank: 3,
    previousRank: 5,
    username: 'BullRunner',
    totalPnL: 28456.80,
    pnlPercent: 28.46,
    winRate: 69.8,
    totalTrades: 198,
    sharpeRatio: 1.89,
    streak: 5,
    avatar: '🐂'
  },
  {
    rank: 4,
    previousRank: 3,
    username: 'SwingKing',
    totalPnL: 24789.20,
    pnlPercent: 24.79,
    winRate: 65.2,
    totalTrades: 156,
    sharpeRatio: 1.76,
    streak: 3,
    avatar: '👑'
  },
  {
    rank: 5,
    previousRank: 7,
    username: 'DayTrader99',
    totalPnL: 19234.60,
    pnlPercent: 19.23,
    winRate: 61.5,
    totalTrades: 523,
    sharpeRatio: 1.54,
    streak: 7,
    avatar: '⚡'
  },
]

const timeframes = [
  { value: 'daily', label: 'Today' },
  { value: 'weekly', label: 'This Week' },
  { value: 'monthly', label: 'This Month' },
  { value: 'all-time', label: 'All Time' },
]

const competitions = [
  { value: 'global', label: 'Global Competition' },
  { value: 'stocks', label: 'Stock Masters' },
  { value: 'beginners', label: 'Beginner League' },
  { value: 'pro', label: 'Pro Traders' },
]

export default function Leaderboard() {
  const [selectedTimeframe, setSelectedTimeframe] = useState('monthly')
  const [selectedCompetition, setSelectedCompetition] = useState('global')
  const [sortBy, setSortBy] = useState<'pnl' | 'winRate' | 'trades' | 'sharpe'>('pnl')

  const getRankChange = (current: number, previous: number) => {
    const change = previous - current
    if (change > 0) {
      return <span className="flex items-center text-green-500 text-xs">
        <ArrowUpIcon className="h-3 w-3" />
        {change}
      </span>
    } else if (change < 0) {
      return <span className="flex items-center text-red-500 text-xs">
        <ArrowDownIcon className="h-3 w-3" />
        {Math.abs(change)}
      </span>
    }
    return <span className="text-gray-400 text-xs">-</span>
  }

  const getRankBadge = (rank: number) => {
    switch (rank) {
      case 1:
        return <div className="flex items-center justify-center w-8 h-8 bg-yellow-500 rounded-full">
          <TrophyIcon className="h-5 w-5 text-white" />
        </div>
      case 2:
        return <div className="flex items-center justify-center w-8 h-8 bg-gray-400 rounded-full">
          <TrophyIcon className="h-5 w-5 text-white" />
        </div>
      case 3:
        return <div className="flex items-center justify-center w-8 h-8 bg-orange-600 rounded-full">
          <TrophyIcon className="h-5 w-5 text-white" />
        </div>
      default:
        return <div className="flex items-center justify-center w-8 h-8 text-lg font-bold text-gray-600">
          {rank}
        </div>
    }
  }

  return (
    <div>
      <div className="mb-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Trading Leaderboard
          </h1>
          <button className="btn-secondary flex items-center">
            <ArrowPathIcon className="h-4 w-4 mr-2" />
            Refresh
          </button>
        </div>

        {/* Filters */}
        <div className="card mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Competition
              </label>
              <select
                value={selectedCompetition}
                onChange={(e) => setSelectedCompetition(e.target.value)}
                className="input w-full"
              >
                {competitions.map(comp => (
                  <option key={comp.value} value={comp.value}>{comp.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Timeframe
              </label>
              <select
                value={selectedTimeframe}
                onChange={(e) => setSelectedTimeframe(e.target.value)}
                className="input w-full"
              >
                {timeframes.map(tf => (
                  <option key={tf.value} value={tf.value}>{tf.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Sort By
              </label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                className="input w-full"
              >
                <option value="pnl">Total P&L</option>
                <option value="winRate">Win Rate</option>
                <option value="trades">Total Trades</option>
                <option value="sharpe">Sharpe Ratio</option>
              </select>
            </div>
          </div>
        </div>

        {/* Competition Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <div className="card bg-gradient-to-r from-purple-50 to-purple-100 dark:from-purple-900/20 dark:to-purple-800/20">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Total Participants</p>
                <p className="text-2xl font-bold text-purple-600 dark:text-purple-400">1,234</p>
              </div>
              <UserGroupIcon className="h-8 w-8 text-purple-600 dark:text-purple-400" />
            </div>
          </div>
          <div className="card bg-gradient-to-r from-green-50 to-green-100 dark:from-green-900/20 dark:to-green-800/20">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Prize Pool</p>
                <p className="text-2xl font-bold text-green-600 dark:text-green-400">$50,000</p>
              </div>
              <TrophyIcon className="h-8 w-8 text-green-600 dark:text-green-400" />
            </div>
          </div>
          <div className="card bg-gradient-to-r from-orange-50 to-orange-100 dark:from-orange-900/20 dark:to-orange-800/20">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Time Remaining</p>
                <p className="text-2xl font-bold text-orange-600 dark:text-orange-400">7d 14h</p>
              </div>
              <ClockIcon className="h-8 w-8 text-orange-600 dark:text-orange-400" />
            </div>
          </div>
          <div className="card bg-gradient-to-r from-blue-50 to-blue-100 dark:from-blue-900/20 dark:to-blue-800/20">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Your Rank</p>
                <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">#42</p>
              </div>
              <ChartBarIcon className="h-8 w-8 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
        </div>
      </div>

      {/* Leaderboard Table */}
      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-800">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Rank
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Trader
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Total P&L
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Win Rate
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Trades
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Sharpe
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Streak
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
              {mockLeaderboardData.map((trader) => (
                <tr key={trader.rank} className={`hover:bg-gray-50 dark:hover:bg-gray-800 ${
                  trader.rank <= 3 ? 'bg-gradient-to-r from-yellow-50/50 to-transparent dark:from-yellow-900/10' : ''
                }`}>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center space-x-2">
                      {getRankBadge(trader.rank)}
                      {getRankChange(trader.rank, trader.previousRank)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="text-2xl mr-3">{trader.avatar}</div>
                      <div>
                        <div className="text-sm font-medium text-gray-900 dark:text-gray-100">
                          {trader.username}
                        </div>
                        <div className="text-sm text-gray-500">
                          Level {Math.floor(trader.totalTrades / 50)}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className={`font-medium ${trader.totalPnL >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      ${trader.totalPnL.toLocaleString()}
                    </div>
                    <div className={`text-sm ${trader.totalPnL >= 0 ? 'text-green-500' : 'text-red-500'}`}>
                      {trader.totalPnL >= 0 ? '+' : ''}{trader.pnlPercent}%
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="text-sm font-medium">{trader.winRate}%</div>
                      <div className="ml-2 w-16 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                        <div 
                          className="bg-green-500 h-2 rounded-full" 
                          style={{ width: `${trader.winRate}%` }}
                        />
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-gray-100">
                    {trader.totalTrades}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className={`text-sm font-medium ${
                      trader.sharpeRatio >= 2 ? 'text-green-600' :
                      trader.sharpeRatio >= 1 ? 'text-yellow-600' :
                      'text-red-600'
                    }`}>
                      {trader.sharpeRatio}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <FireIcon className={`h-4 w-4 mr-1 ${
                        trader.streak >= 10 ? 'text-red-500' :
                        trader.streak >= 5 ? 'text-orange-500' :
                        'text-gray-400'
                      }`} />
                      <span className="text-sm font-medium">{trader.streak}</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Your Stats Card */}
      <div className="mt-6 card bg-gradient-to-r from-blue-50 to-indigo-100 dark:from-blue-900/20 dark:to-indigo-900/20 border-blue-200 dark:border-blue-800">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-blue-900 dark:text-blue-100">
            Your Performance
          </h3>
          <SparklesIcon className="h-6 w-6 text-blue-600 dark:text-blue-400" />
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <p className="text-sm text-blue-700 dark:text-blue-300">Current Rank</p>
            <p className="text-2xl font-bold text-blue-900 dark:text-blue-100">#42</p>
            <p className="text-xs text-blue-600 dark:text-blue-400">Top 3.4%</p>
          </div>
          <div>
            <p className="text-sm text-blue-700 dark:text-blue-300">Total P&L</p>
            <p className="text-2xl font-bold text-green-600">+$12,345</p>
            <p className="text-xs text-green-500">+12.35%</p>
          </div>
          <div>
            <p className="text-sm text-blue-700 dark:text-blue-300">Win Rate</p>
            <p className="text-2xl font-bold text-blue-900 dark:text-blue-100">58.2%</p>
            <p className="text-xs text-blue-600 dark:text-blue-400">143 trades</p>
          </div>
          <div>
            <p className="text-sm text-blue-700 dark:text-blue-300">Current Streak</p>
            <p className="text-2xl font-bold text-orange-600">4 🔥</p>
            <p className="text-xs text-blue-600 dark:text-blue-400">Best: 7</p>
          </div>
        </div>
      </div>
    </div>
  )
}