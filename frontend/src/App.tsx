import { Routes, Route, Navigate } from 'react-router-dom'
import { Suspense, lazy } from 'react'

import Layout from '@/components/Layout'
import LoadingSpinner from '@/components/common/LoadingSpinner'

// Lazy load pages for code splitting
const Dashboard = lazy(() => import('@/pages/Dashboard'))
const Trading = lazy(() => import('@/pages/Trading'))
const OrderBook = lazy(() => import('@/pages/OrderBook'))
const Orders = lazy(() => import('@/pages/Orders'))
const Positions = lazy(() => import('@/pages/Positions'))
const Account = lazy(() => import('@/pages/Account'))
const Settings = lazy(() => import('@/pages/Settings'))
const Charts = lazy(() => import('@/pages/Charts'))
const Leaderboard = lazy(() => import('@/pages/Leaderboard'))

function App() {
  return (
    <Layout>
      <Suspense fallback={<LoadingSpinner />}>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/trading/:symbol?" element={<Trading />} />
          <Route path="/orderbook/:symbol?" element={<OrderBook />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/positions" element={<Positions />} />
          <Route path="/charts" element={<Charts />} />
          <Route path="/leaderboard" element={<Leaderboard />} />
          <Route path="/account" element={<Account />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Suspense>
    </Layout>
  )
}

export default App