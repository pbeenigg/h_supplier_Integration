'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'

interface HealthStatus {
  status: string
  details?: any
}

interface SystemStats {
  uptime: number
  memoryUsage: any
  cpuUsage: number
}

interface SupplierMetrics {
  totalSuppliers: number
  activeSuppliers: number
  healthySuppliers: number
}

interface PerformanceMetrics {
  responseTime: number
  throughput: number
  errorRate: number
}

interface HttpClientMetrics {
  totalRequests: number
  successfulRequests: number
  failedRequests: number
  averageResponseTime: number
}

export default function MonitorPage() {
  const [health, setHealth] = useState<HealthStatus | null>(null)
  const [stats, setStats] = useState<SystemStats | null>(null)
  const [supplierMetrics, setSupplierMetrics] = useState<SupplierMetrics | null>(null)
  const [performanceMetrics, setPerformanceMetrics] = useState<PerformanceMetrics | null>(null)
  const [httpClientMetrics, setHttpClientMetrics] = useState<HttpClientMetrics | null>(null)
  const [httpClientHealth, setHttpClientHealth] = useState<HealthStatus | null>(null)
  const [loading, setLoading] = useState(true)

  const fetchMonitorData = async () => {
    setLoading(true)
    try {
      const [
        healthRes,
        statsRes,
        supplierRes,
        performanceRes,
        httpMetricsRes,
        httpHealthRes
      ] = await Promise.all([
        apiClient.get('/monitor/health'),
        apiClient.get('/monitor/stats'),
        apiClient.get('/monitor/metrics/suppliers'),
        apiClient.get('/monitor/metrics/performance'),
        apiClient.get('/monitor/httpClient/metrics'),
        apiClient.get('/monitor/httpClient/health')
      ])

      setHealth(healthRes.data)
      setStats(statsRes.data)
      setSupplierMetrics(supplierRes.data)
      setPerformanceMetrics(performanceRes.data)
      setHttpClientMetrics(httpMetricsRes.data)
      setHttpClientHealth(httpHealthRes.data)
    } catch (error) {
      console.error('获取监控数据失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchMonitorData()
    // 每30秒刷新一次数据
    const interval = setInterval(fetchMonitorData, 30000)
    return () => clearInterval(interval)
  }, [])

  const getStatusColor = (status: string) => {
    switch (status?.toLowerCase()) {
      case 'up':
      case 'healthy':
      case 'ok':
        return 'bg-green-500'
      case 'down':
      case 'unhealthy':
      case 'error':
        return 'bg-red-500'
      default:
        return 'bg-yellow-500'
    }
  }

  const formatUptime = (seconds: number) => {
    const days = Math.floor(seconds / 86400)
    const hours = Math.floor((seconds % 86400) / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    return `${days}天 ${hours}小时 ${minutes}分钟`
  }

  if (loading) {
    return (
      <MainLayout>
        <div className="container mx-auto p-6">
          <div className="flex items-center justify-center min-h-[400px]">
            <div className="text-lg">加载监控数据中...</div>
          </div>
        </div>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <div className="container mx-auto p-6">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold">系统监控</h1>
          <Button onClick={fetchMonitorData} disabled={loading}>
            刷新数据
          </Button>
        </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* 系统健康状态 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              系统健康状态
              {health && (
                <Badge className={`${getStatusColor(health.status)} text-white`}>
                  {health.status}
                </Badge>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {health ? (
              <div className="space-y-2">
                <div>状态: {health.status}</div>
                {health.details && (
                  <pre className="text-sm bg-gray-100 p-2 rounded">
                    {JSON.stringify(health.details, null, 2)}
                  </pre>
                )}
              </div>
            ) : (
              <div>暂无数据</div>
            )}
          </CardContent>
        </Card>

        {/* 系统统计 */}
        <Card>
          <CardHeader>
            <CardTitle>系统统计</CardTitle>
          </CardHeader>
          <CardContent>
            {stats ? (
              <div className="space-y-2">
                <div>运行时间: {formatUptime(stats.uptime)}</div>
                <div>CPU 使用率: {stats.cpuUsage?.toFixed(2)}%</div>
                {stats.memoryUsage && (
                  <div>
                    内存使用: {(stats.memoryUsage.used / 1024 / 1024).toFixed(0)}MB / 
                    {(stats.memoryUsage.total / 1024 / 1024).toFixed(0)}MB
                  </div>
                )}
              </div>
            ) : (
              <div>暂无数据</div>
            )}
          </CardContent>
        </Card>

        {/* 供应商指标 */}
        <Card>
          <CardHeader>
            <CardTitle>供应商指标</CardTitle>
          </CardHeader>
          <CardContent>
            {supplierMetrics ? (
              <div className="space-y-2">
                <div>总供应商数: {supplierMetrics.totalSuppliers}</div>
                <div>活跃供应商: {supplierMetrics.activeSuppliers}</div>
                <div>健康供应商: {supplierMetrics.healthySuppliers}</div>
                <div>
                  健康率: {
                    supplierMetrics.totalSuppliers > 0 
                      ? ((supplierMetrics.healthySuppliers / supplierMetrics.totalSuppliers) * 100).toFixed(1)
                      : 0
                  }%
                </div>
              </div>
            ) : (
              <div>暂无数据</div>
            )}
          </CardContent>
        </Card>

        {/* 性能指标 */}
        <Card>
          <CardHeader>
            <CardTitle>性能指标</CardTitle>
          </CardHeader>
          <CardContent>
            {performanceMetrics ? (
              <div className="space-y-2">
                <div>平均响应时间: {performanceMetrics.responseTime}ms</div>
                <div>吞吐量: {performanceMetrics.throughput} req/s</div>
                <div>错误率: {(performanceMetrics.errorRate * 100).toFixed(2)}%</div>
              </div>
            ) : (
              <div>暂无数据</div>
            )}
          </CardContent>
        </Card>

        {/* HttpClient 指标 */}
        <Card>
          <CardHeader>
            <CardTitle>HttpClient 指标</CardTitle>
          </CardHeader>
          <CardContent>
            {httpClientMetrics ? (
              <div className="space-y-2">
                <div>总请求数: {httpClientMetrics.totalRequests}</div>
                <div>成功请求: {httpClientMetrics.successfulRequests}</div>
                <div>失败请求: {httpClientMetrics.failedRequests}</div>
                <div>平均响应时间: {httpClientMetrics.averageResponseTime}ms</div>
                <div>
                  成功率: {
                    httpClientMetrics.totalRequests > 0
                      ? ((httpClientMetrics.successfulRequests / httpClientMetrics.totalRequests) * 100).toFixed(1)
                      : 0
                  }%
                </div>
              </div>
            ) : (
              <div>暂无数据</div>
            )}
          </CardContent>
        </Card>

        {/* HttpClient 健康状态 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              HttpClient 健康状态
              {httpClientHealth && (
                <Badge className={`${getStatusColor(httpClientHealth.status)} text-white`}>
                  {httpClientHealth.status}
                </Badge>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {httpClientHealth ? (
              <div className="space-y-2">
                <div>状态: {httpClientHealth.status}</div>
                {httpClientHealth.details && (
                  <pre className="text-sm bg-gray-100 p-2 rounded max-h-32 overflow-y-auto">
                    {JSON.stringify(httpClientHealth.details, null, 2)}
                  </pre>
                )}
              </div>
            ) : (
              <div>暂无数据</div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
    </MainLayout>
  )
}