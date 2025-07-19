import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { orderApi } from '@/services/api'
import { Order } from '@/types'
import toast from 'react-hot-toast'
import { 
  DocumentTextIcon, 
  XMarkIcon, 
  CheckCircleIcon,
  ClockIcon,
  XCircleIcon,
  ArrowPathIcon,
  FunnelIcon,
  ArrowUpIcon,
  ArrowDownIcon
} from '@heroicons/react/24/outline'

export default function Orders() {
  const queryClient = useQueryClient()
  const [filterStatus, setFilterStatus] = useState<string>('ALL')
  const [filterSide, setFilterSide] = useState<string>('ALL')
  const [sortBy, setSortBy] = useState<'createdAt' | 'symbol' | 'status'>('createdAt')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc')

  // Fetch orders
  const { data: orders, isLoading, refetch } = useQuery({
    queryKey: ['orders'],
    queryFn: () => orderApi.getOrders(),
    refetchInterval: 2000, // Real-time updates
  })

  // Cancel order mutation
  const cancelOrderMutation = useMutation({
    mutationFn: (orderId: string) => orderApi.cancelOrder(orderId),
    onSuccess: () => {
      toast.success('Order cancelled successfully')
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to cancel order')
    },
  })

  // Filter and sort orders
  const filteredOrders = orders
    ?.filter(order => {
      if (filterStatus !== 'ALL' && order.status !== filterStatus) return false
      if (filterSide !== 'ALL' && order.side !== filterSide) return false
      return true
    })
    .sort((a, b) => {
      let comparison = 0
      switch (sortBy) {
        case 'createdAt':
          comparison = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
          break
        case 'symbol':
          comparison = a.symbol.localeCompare(b.symbol)
          break
        case 'status':
          comparison = a.status.localeCompare(b.status)
          break
      }
      return sortOrder === 'asc' ? comparison : -comparison
    })

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'NEW':
        return <ClockIcon className="h-5 w-5 text-blue-500" />
      case 'PARTIALLY_FILLED':
        return <ArrowPathIcon className="h-5 w-5 text-yellow-500" />
      case 'FILLED':
        return <CheckCircleIcon className="h-5 w-5 text-green-500" />
      case 'CANCELLED':
        return <XCircleIcon className="h-5 w-5 text-red-500" />
      case 'REJECTED':
        return <XMarkIcon className="h-5 w-5 text-red-600" />
      default:
        return <DocumentTextIcon className="h-5 w-5 text-gray-500" />
    }
  }

  const getStatusBadge = (status: string) => {
    const baseClasses = "px-2 py-1 text-xs rounded-full font-medium"
    switch (status) {
      case 'NEW':
        return `${baseClasses} bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-400`
      case 'PARTIALLY_FILLED':
        return `${baseClasses} bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-400`
      case 'FILLED':
        return `${baseClasses} bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400`
      case 'CANCELLED':
        return `${baseClasses} bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400`
      case 'REJECTED':
        return `${baseClasses} bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400`
      default:
        return `${baseClasses} bg-gray-100 text-gray-800 dark:bg-gray-900/20 dark:text-gray-400`
    }
  }

  const canCancelOrder = (order: Order) => {
    return order.status === 'NEW' || order.status === 'PARTIALLY_FILLED'
  }

  if (isLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="text-gray-500">Loading orders...</div>
      </div>
    )
  }

  return (
    <div>
      <div className="mb-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Order Management
          </h1>
          <button
            onClick={() => refetch()}
            className="btn-secondary flex items-center"
          >
            <ArrowPathIcon className="h-4 w-4 mr-2" />
            Refresh
          </button>
        </div>

        {/* Filters */}
        <div className="card mb-6">
          <div className="flex items-center mb-4">
            <FunnelIcon className="h-5 w-5 mr-2 text-gray-500" />
            <h2 className="text-lg font-semibold">Filters</h2>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Status Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Status
              </label>
              <select
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                className="input w-full"
              >
                <option value="ALL">All Statuses</option>
                <option value="NEW">New</option>
                <option value="PARTIALLY_FILLED">Partially Filled</option>
                <option value="FILLED">Filled</option>
                <option value="CANCELLED">Cancelled</option>
                <option value="REJECTED">Rejected</option>
              </select>
            </div>

            {/* Side Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Side
              </label>
              <select
                value={filterSide}
                onChange={(e) => setFilterSide(e.target.value)}
                className="input w-full"
              >
                <option value="ALL">All Sides</option>
                <option value="BUY">Buy</option>
                <option value="SELL">Sell</option>
              </select>
            </div>

            {/* Sort By */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Sort By
              </label>
              <div className="flex space-x-2">
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value as any)}
                  className="input flex-1"
                >
                  <option value="createdAt">Date</option>
                  <option value="symbol">Symbol</option>
                  <option value="status">Status</option>
                </select>
                <button
                  onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                  className="btn-secondary p-2"
                >
                  {sortOrder === 'asc' ? (
                    <ArrowUpIcon className="h-4 w-4" />
                  ) : (
                    <ArrowDownIcon className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Summary Stats */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="card bg-blue-50 dark:bg-blue-900/20">
            <p className="text-sm text-gray-600 dark:text-gray-400">Active Orders</p>
            <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">
              {orders?.filter(o => o.status === 'NEW' || o.status === 'PARTIALLY_FILLED').length || 0}
            </p>
          </div>
          <div className="card bg-green-50 dark:bg-green-900/20">
            <p className="text-sm text-gray-600 dark:text-gray-400">Filled Today</p>
            <p className="text-2xl font-bold text-green-600 dark:text-green-400">
              {orders?.filter(o => o.status === 'FILLED').length || 0}
            </p>
          </div>
          <div className="card bg-yellow-50 dark:bg-yellow-900/20">
            <p className="text-sm text-gray-600 dark:text-gray-400">Partial Fills</p>
            <p className="text-2xl font-bold text-yellow-600 dark:text-yellow-400">
              {orders?.filter(o => o.status === 'PARTIALLY_FILLED').length || 0}
            </p>
          </div>
          <div className="card bg-red-50 dark:bg-red-900/20">
            <p className="text-sm text-gray-600 dark:text-gray-400">Cancelled</p>
            <p className="text-2xl font-bold text-red-600 dark:text-red-400">
              {orders?.filter(o => o.status === 'CANCELLED').length || 0}
            </p>
          </div>
        </div>
      </div>

      {/* Orders Table */}
      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-800">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Order ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Time
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Symbol
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Side
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Type
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Price
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Quantity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Filled
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
              {filteredOrders?.length === 0 ? (
                <tr>
                  <td colSpan={10} className="px-6 py-4 text-center text-gray-500">
                    No orders found
                  </td>
                </tr>
              ) : (
                filteredOrders?.map((order) => (
                  <tr key={order.orderId} className="hover:bg-gray-50 dark:hover:bg-gray-800">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">
                      {order.orderId.slice(0, 8)}...
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {new Date(order.createdAt).toLocaleTimeString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      {order.symbol}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span className={`font-medium ${
                        order.side === 'BUY' ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {order.side}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {order.type}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-gray-100">
                      ${order.price?.toFixed(2) || 'Market'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {order.quantity.toLocaleString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {order.filledQuantity.toLocaleString()} ({Math.round((order.filledQuantity / order.quantity) * 100)}%)
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        {getStatusIcon(order.status)}
                        <span className={`ml-2 ${getStatusBadge(order.status)}`}>
                          {order.status}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      {canCancelOrder(order) ? (
                        <button
                          onClick={() => cancelOrderMutation.mutate(order.orderId)}
                          disabled={cancelOrderMutation.isPending}
                          className="text-red-600 hover:text-red-700 font-medium"
                        >
                          Cancel
                        </button>
                      ) : (
                        <span className="text-gray-400">-</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Order Details Modal would go here */}
    </div>
  )
}