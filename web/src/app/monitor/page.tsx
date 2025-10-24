'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'
import { 
  Activity, 
  Server, 
  Database, 
  Users, 
  Settings, 
  BarChart3, 
  Cpu, 
  HardDrive,
  Network,
  RefreshCw,
  CheckCircle,
  AlertCircle,
  Clock,
  Gauge,
  Zap,
  Globe,
  XCircle,
  TrendingUp,
  ArrowDown,
  ArrowUp
} from 'lucide-react'

// 系统就绪接口响应
interface ReadyResponse {
  message: string
  timestamp: string
  status: string
}

// 系统健康检查接口响应
interface HealthResponse {
  database: {
    syncLogCount: number
    status: string
    distributionOrdersLogCount: number
    supplierCount: number
    apiCallCount: number
    distributionCallLogCount: number
  }
  suppliers: {
    count: number
    healthChecks: number
    enabled: string[]
  }
  service: string
  systemConfig: {
    status: string
    activeConfigs: number
  }
  version: string
  status: string
  timestamp: string
}

// 系统性能指标接口响应
interface PerformanceResponse {
  jvm: {
    maxMemory: number
    freeMemory: number
    totalMemory: number
    usedMemory: number
    availableProcessors: number
  }
  averageResponseTime: number
  timestamp: string
}

// HTTP监控指标接口响应
interface HttpMetricsResponse {
  healthy: boolean
  metrics: string
  availablePermits: number
  timestamp: number
}

// HTTP健康检查接口响应
interface HttpHealthResponse {
  maxPermits: number
  utilizationRate: string
  message: string
  availablePermits: number
  status: string
  timestamp: number
}

// 统一API响应格式
interface ApiResponse<T> {
  code: number
  msg: string
  data: T
  timestamp: number
}

export default function MonitorPage() {
  const [ready, setReady] = useState<ReadyResponse | null>(null)
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [performance, setPerformance] = useState<PerformanceResponse | null>(null)
  const [httpMetrics, setHttpMetrics] = useState<HttpMetricsResponse | null>(null)
  const [httpHealth, setHttpHealth] = useState<HttpHealthResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date())

  const fetchMonitorData = async () => {
    setLoading(true)
    try {
      const [
        readyRes,
        healthRes,
        performanceRes,
        httpMetricsRes,
        httpHealthRes
      ] = await Promise.all([
        apiClient.get<ApiResponse<ReadyResponse>>('/monitor/ready'),
        apiClient.get<ApiResponse<HealthResponse>>('/monitor/health'),
        apiClient.get<ApiResponse<PerformanceResponse>>('/monitor/metrics/performance'),
        apiClient.get<ApiResponse<HttpMetricsResponse>>('/monitor/httpClient/metrics'),
        apiClient.get<ApiResponse<HttpHealthResponse>>('/monitor/httpClient/health')
      ])

      if (readyRes.data?.code === 200) setReady(readyRes.data.data)
      if (healthRes.data?.code === 200) setHealth(healthRes.data.data)
      if (performanceRes.data?.code === 200) setPerformance(performanceRes.data.data)
      if (httpMetricsRes.data?.code === 200) setHttpMetrics(httpMetricsRes.data.data)
      if (httpHealthRes.data?.code === 200) setHttpHealth(httpHealthRes.data.data)
      
      setLastUpdate(new Date())
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
      case 'ready':
        return 'text-green-600 bg-green-100'
      case 'down':
      case 'unhealthy':
      case 'error':
        return 'text-red-600 bg-red-100'
      default:
        return 'text-yellow-600 bg-yellow-100'
    }
  }

  const formatMemory = (bytes: number) => {
    return `${(bytes / 1024).toFixed(0)}GB`
  }

  const formatBytes = (bytes: number) => {
    return `${(bytes / 1024 / 1024).toFixed(0)}MB`
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
        <div className="flex justify-between items-center mb-8">
          <div className="flex items-center space-x-4">
            <div className="p-3 bg-gradient-to-r from-blue-500 to-purple-600 rounded-lg">
              <Activity className="w-8 h-8 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold bg-gradient-to-r from-gray-900 to-gray-600 bg-clip-text text-transparent">
                系统监控中心
              </h1>
              <p className="text-gray-500 mt-1 flex items-center">
                <Clock className="w-4 h-4 mr-1" />
                最后更新: {lastUpdate.toLocaleString()}
              </p>
            </div>
          </div>
          <Button 
            onClick={fetchMonitorData} 
            disabled={loading}
            className="bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white shadow-lg hover:shadow-xl transition-all duration-200"
          >
            <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            {loading ? '刷新中...' : '刷新数据'}
          </Button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* 系统就绪状态 */}
          <Card className="hover:shadow-lg transition-all duration-200 border-0 bg-gradient-to-br from-green-50 to-emerald-50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-3">
                <div className="p-2 bg-gradient-to-r from-green-500 to-emerald-600 rounded-lg">
                  <CheckCircle className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-800 font-semibold">系统就绪状态</span>
                    {ready && (
                      <Badge className={`${getStatusColor(ready.status)} font-medium`}>
                        {ready.status}
                      </Badge>
                    )}
                  </div>
                </div>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              {ready ? (
                <div className="space-y-3">
                  <div className="flex items-start space-x-2">
                    <Zap className="w-4 h-4 text-green-600 mt-0.5 flex-shrink-0" />
                    <div className="text-sm text-gray-700 font-medium">{ready.message}</div>
                  </div>
                  <div className="flex items-center space-x-2 text-xs text-gray-500">
                    <Clock className="w-3 h-3" />
                    <span>检查时间: {new Date(ready.timestamp).toLocaleString()}</span>
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-2 text-gray-500">
                  <AlertCircle className="w-4 h-4" />
                  <span>暂无数据</span>
                </div>
              )}
            </CardContent>
          </Card>

          {/* 系统健康状态 */}
          <Card className="hover:shadow-lg transition-all duration-200 border-0 bg-gradient-to-br from-blue-50 to-indigo-50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-3">
                <div className="p-2 bg-gradient-to-r from-blue-500 to-indigo-600 rounded-lg">
                  <Server className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-800 font-semibold">系统健康状态</span>
                    {health && (
                      <Badge className={`${getStatusColor(health.status)} font-medium`}>
                        {health.status}
                      </Badge>
                    )}
                  </div>
                </div>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              {health ? (
                <div className="space-y-3">
                  <div className="bg-white/60 backdrop-blur-sm rounded-lg p-3 border border-blue-100">
                    <div className="font-medium text-blue-900 mb-2">{health.service} v{health.version}</div>
                    <div className="grid grid-cols-1 gap-2">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-2">
                          <Database className="w-4 h-4 text-blue-600" />
                          <span className="text-sm text-gray-700">数据库</span>
                        </div>
                        <Badge className={`${getStatusColor(health.database.status)} text-xs px-2 py-1`}>
                          {health.database.status}
                        </Badge>
                      </div>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-2">
                          <Settings className="w-4 h-4 text-blue-600" />
                          <span className="text-sm text-gray-700">系统配置</span>
                        </div>
                        <Badge className={`${getStatusColor(health.systemConfig.status)} text-xs px-2 py-1`}>
                          {health.systemConfig.status}
                        </Badge>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-2 text-gray-500">
                  <AlertCircle className="w-4 h-4" />
                  <span>暂无数据</span>
                </div>
              )}
            </CardContent>
          </Card>

          {/* 数据库统计 */}
          <Card className="hover:shadow-lg transition-all duration-200 border-0 bg-gradient-to-br from-purple-50 to-pink-50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-3">
                <div className="p-2 bg-gradient-to-r from-purple-500 to-pink-600 rounded-lg">
                  <Database className="w-5 h-5 text-white" />
                </div>
                <span className="text-gray-800 font-semibold">数据库统计</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              {health?.database ? (
                <div className="grid grid-cols-1 gap-3">
                  <div className="bg-white/60 backdrop-blur-sm rounded-lg p-3 border border-purple-100">
                    <div className="grid grid-cols-2 gap-3">
                      <div className="flex items-center space-x-2">
                        <div className="w-2 h-2 bg-purple-500 rounded-full"></div>
                        <div>
                          <div className="text-xs text-gray-500">供应商数量</div>
                          <div className="font-semibold text-purple-700">{health.database.supplierCount}</div>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <div className="w-2 h-2 bg-pink-500 rounded-full"></div>
                        <div>
                          <div className="text-xs text-gray-500">API调用</div>
                          <div className="font-semibold text-pink-700">{health.database.apiCallCount?.toLocaleString()}</div>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <div className="w-2 h-2 bg-indigo-500 rounded-full"></div>
                        <div>
                          <div className="text-xs text-gray-500">同步日志</div>
                          <div className="font-semibold text-indigo-700">{health.database.syncLogCount}</div>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <div className="w-2 h-2 bg-emerald-500 rounded-full"></div>
                        <div>
                          <div className="text-xs text-gray-500">分销订单</div>
                          <div className="font-semibold text-emerald-700">{health.database.distributionOrdersLogCount}</div>
                        </div>
                      </div>
                    </div>
                    <div className="mt-3 pt-3 border-t border-purple-100">
                      <div className="flex items-center space-x-2">
                        <div className="w-2 h-2 bg-orange-500 rounded-full"></div>
                        <div>
                          <div className="text-xs text-gray-500">分销调用</div>
                          <div className="font-semibold text-orange-700">{health.database.distributionCallLogCount}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-2 text-gray-500">
                  <AlertCircle className="w-4 h-4" />
                  <span>暂无数据</span>
                </div>
              )}
            </CardContent>
          </Card>

          {/* 供应商状态 */}
          <Card className="hover:shadow-lg transition-all duration-200 border-0 bg-gradient-to-br from-emerald-50 to-teal-50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-3">
                <div className="p-2 bg-gradient-to-r from-emerald-500 to-teal-600 rounded-lg">
                  <Users className="w-5 h-5 text-white" />
                </div>
                <span className="text-gray-800 font-semibold">供应商状态</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              {health?.suppliers ? (
                <div className="space-y-4">
                  <div className="bg-white/60 backdrop-blur-sm rounded-lg p-3 border border-emerald-100">
                    <div className="grid grid-cols-2 gap-4 mb-4">
                      <div className="text-center">
                        <div className="text-2xl font-bold text-emerald-600">{health.suppliers.count}</div>
                        <div className="text-xs text-gray-500">总供应商数</div>
                      </div>
                      <div className="text-center">
                        <div className="text-2xl font-bold text-teal-600">{health.suppliers.healthChecks}</div>
                        <div className="text-xs text-gray-500">健康检查数</div>
                      </div>
                    </div>
                    <div>
                      <div className="text-sm font-medium text-gray-700 mb-2">启用的供应商:</div>
                      <div className="flex flex-wrap gap-1">
                        {health.suppliers.enabled.map((supplier, index) => (
                          <Badge 
                            key={index} 
                            className="bg-emerald-100 text-emerald-800 hover:bg-emerald-200 transition-colors"
                          >
                            {supplier}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-2 text-gray-500">
                  <AlertCircle className="w-4 h-4" />
                  <span>暂无数据</span>
                </div>
              )}
            </CardContent>
          </Card>

          {/* 系统配置 */}
          <Card className="hover:shadow-lg transition-all duration-200 border-0 bg-gradient-to-br from-amber-50 to-orange-50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-3">
                <div className="p-2 bg-gradient-to-r from-amber-500 to-orange-600 rounded-lg">
                  <Settings className="w-5 h-5 text-white" />
                </div>
                <span className="text-gray-800 font-semibold">系统配置</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              {health?.systemConfig ? (
                <div className="space-y-3">
                  <div className="bg-white/60 backdrop-blur-sm rounded-lg p-4 border border-amber-100">
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-sm font-medium text-gray-700">配置状态</span>
                      <Badge className={`${getStatusColor(health.systemConfig.status)} font-medium`}>
                        {health.systemConfig.status}
                      </Badge>
                    </div>
                    <div className="flex items-center space-x-3">
                      <div className="flex-1 text-center">
                        <div className="text-2xl font-bold text-amber-600">{health.systemConfig.activeConfigs}</div>
                        <div className="text-xs text-gray-500">活跃配置数</div>
                      </div>
                      <div className="w-px h-8 bg-amber-200"></div>
                      <div className="flex-1 text-center">
                        <div className="text-lg font-semibold text-orange-600">
                          {health.systemConfig.activeConfigs > 0 ? '正常' : '异常'}
                        </div>
                        <div className="text-xs text-gray-500">配置状态</div>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-2 text-gray-500">
                  <AlertCircle className="w-4 h-4" />
                  <span>暂无数据</span>
                </div>
              )}
            </CardContent>
          </Card>

          {/* JVM 性能指标 */}
          <Card className="hover:shadow-lg transition-all duration-200 border-0 bg-gradient-to-br from-cyan-50 to-blue-50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-3">
                <div className="p-2 bg-gradient-to-r from-cyan-500 to-blue-600 rounded-lg">
                  <Cpu className="w-5 h-5 text-white" />
                </div>
                <span className="text-gray-800 font-semibold">JVM 性能指标</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              {performance?.jvm ? (
                <div className="space-y-4">
                  <div className="bg-white/60 backdrop-blur-sm rounded-lg p-4 border border-cyan-100">
                    {/* 内存使用率进度条 */}
                    <div className="mb-4">
                      <div className="flex justify-between items-center mb-2">
                        <span className="text-sm font-medium text-gray-700">内存使用率</span>
                        <span className="text-sm font-bold text-cyan-600">
                          {((performance.jvm.usedMemory / performance.jvm.totalMemory) * 100).toFixed(1)}%
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div 
                          className="bg-gradient-to-r from-cyan-500 to-blue-500 h-2 rounded-full transition-all duration-300"
                          style={{ width: `${((performance.jvm.usedMemory / performance.jvm.totalMemory) * 100)}%` }}
                        ></div>
                      </div>
                    </div>
                    
                    {/* 内存详情 */}
                    <div className="grid grid-cols-2 gap-3 mb-4">
                      <div className="flex items-center space-x-2">
                        <HardDrive className="w-4 h-4 text-cyan-600" />
                        <div>
                          <div className="text-xs text-gray-500">最大内存</div>
                          <div className="font-semibold text-cyan-700">{formatMemory(performance.jvm.maxMemory)}</div>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Gauge className="w-4 h-4 text-blue-600" />
                        <div>
                          <div className="text-xs text-gray-500">总内存</div>
                          <div className="font-semibold text-blue-700">{formatMemory(performance.jvm.totalMemory)}</div>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <BarChart3 className="w-4 h-4 text-red-600" />
                        <div>
                          <div className="text-xs text-gray-500">已用内存</div>
                          <div className="font-semibold text-red-700">{formatMemory(performance.jvm.usedMemory)}</div>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Activity className="w-4 h-4 text-green-600" />
                        <div>
                          <div className="text-xs text-gray-500">可用内存</div>
                          <div className="font-semibold text-green-700">{formatMemory(performance.jvm.freeMemory)}</div>
                        </div>
                      </div>
                    </div>
                    
                    {/* 处理器信息 */}
                    <div className="pt-3 border-t border-cyan-100">
                      <div className="flex items-center justify-center space-x-2">
                        <Cpu className="w-4 h-4 text-cyan-600" />
                        <div className="text-center">
                          <div className="text-lg font-bold text-cyan-700">{performance.jvm.availableProcessors}</div>
                          <div className="text-xs text-gray-500">可用处理器</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-2 text-gray-500">
                  <AlertCircle className="w-4 h-4" />
                  <span>暂无数据</span>
                </div>
              )}
            </CardContent>
          </Card>

          {/* 响应时间指标 */}
          <Card className="hover:shadow-lg transition-all duration-200 border-0 bg-gradient-to-br from-pink-50 to-rose-50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-3">
                <div className="p-2 bg-gradient-to-r from-pink-500 to-rose-600 rounded-lg">
                  <Clock className="w-5 h-5 text-white" />
                </div>
                <span className="text-gray-800 font-semibold">响应时间指标</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              {performance ? (
                <div className="space-y-4">
                  <div className="bg-white/60 backdrop-blur-sm rounded-lg p-4 border border-pink-100">
                    {/* 平均响应时间 */}
                    <div className="text-center p-3 bg-gradient-to-r from-pink-100 to-rose-100 rounded-lg mb-4">
                      <div className="text-3xl font-bold text-pink-700">{performance.averageResponseTime.toFixed(2)}ms</div>
                      <div className="text-xs text-gray-600 flex items-center justify-center gap-1 mt-1">
                        <TrendingUp className="w-3 h-3" />
                        平均响应时间
                      </div>
                    </div>
                    
                    {/* 更新时间 */}
                    <div className="pt-3 border-t border-pink-100 text-center">
                      <div className="flex items-center justify-center space-x-2 text-xs text-gray-500">
                        <Clock className="w-3 h-3" />
                        <span>更新时间: {new Date(performance.timestamp).toLocaleString()}</span>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-2 text-gray-500">
                  <AlertCircle className="w-4 h-4" />
                  <span>暂无数据</span>
                </div>
              )}
            </CardContent>
          </Card>

          {/* HTTP客户端指标 */}
          {/* HTTP客户端指标 */}
          <Card className="hover:shadow-lg transition-all duration-200 border-0 bg-gradient-to-br from-indigo-50 to-purple-50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-3">
                <div className="p-2 bg-gradient-to-r from-indigo-500 to-purple-600 rounded-lg">
                  <Globe className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-800 font-semibold">HTTP客户端指标</span>
                    {httpMetrics && (
                      <Badge className={`${httpMetrics.healthy ? 'bg-green-100 text-green-700 border-green-200' : 'bg-red-100 text-red-700 border-red-200'} font-medium`}>
                        {httpMetrics.healthy ? 'HEALTHY' : 'UNHEALTHY'}
                      </Badge>
                    )}
                  </div>
                </div>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              {httpMetrics ? (
                <div className="space-y-4">
                  <div className="bg-white/60 backdrop-blur-sm rounded-lg p-4 border border-indigo-100">
                    {/* 健康状态 */}
                    <div className="text-center p-3 bg-gradient-to-r from-indigo-100 to-purple-100 rounded-lg mb-4">
                      <div className="text-2xl font-bold text-indigo-700">
                        {httpMetrics.healthy ? '✓' : '✗'}
                      </div>
                      <div className="text-xs text-gray-600 flex items-center justify-center gap-1">
                        <Globe className="w-3 h-3" />
                        客户端状态
                      </div>
                    </div>
                    
                    {/* 指标详情 */}
                    <div className="grid grid-cols-2 gap-3">
                      <div className="text-center p-3 bg-blue-50 rounded-lg">
                        <div className="flex items-center justify-center space-x-1 mb-1">
                          <Gauge className="w-4 h-4 text-blue-600" />
                        </div>
                        <div className="text-lg font-bold text-blue-700">
                          {httpMetrics.availablePermits}
                        </div>
                        <div className="text-xs text-gray-500">可用许可</div>
                      </div>
                      <div className="text-center p-3 bg-purple-50 rounded-lg">
                        <div className="flex items-center justify-center space-x-1 mb-1">
                          <Clock className="w-4 h-4 text-purple-600" />
                        </div>
                        <div className="text-lg font-bold text-purple-700">
                          {new Date(httpMetrics.timestamp).toLocaleTimeString()}
                        </div>
                        <div className="text-xs text-gray-500">最后更新</div>
                      </div>
                    </div>
                    
                    {/* 指标详情 */}
                    {httpMetrics.metrics && (
                      <div className="mt-3 p-3 bg-gray-50 rounded-lg">
                        <div className="text-xs text-gray-600 text-center">
                          {httpMetrics.metrics}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-2 text-gray-500">
                  <AlertCircle className="w-4 h-4" />
                  <span>暂无数据</span>
                </div>
              )}
            </CardContent>
          </Card>          {/* HTTP客户端健康状态 */}
          <Card className="hover:shadow-lg transition-all duration-200 border-0 bg-gradient-to-br from-teal-50 to-cyan-50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-3">
                <div className="p-2 bg-gradient-to-r from-teal-500 to-cyan-600 rounded-lg">
                  <Network className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-800 font-semibold">HTTP客户端健康状态</span>
                    {httpHealth && (
                      <Badge className={`${getStatusColor(httpHealth.status)} font-medium`}>
                        {httpHealth.status}
                      </Badge>
                    )}
                  </div>
                </div>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              {httpHealth ? (
                <div className="space-y-4">
                  <div className="bg-white/60 backdrop-blur-sm rounded-lg p-4 border border-teal-100">
                    {/* 状态消息 */}
                    <div className="text-center p-3 bg-gradient-to-r from-teal-100 to-cyan-100 rounded-lg mb-4">
                      <div className="text-sm font-medium text-teal-700">
                        {httpHealth.message}
                      </div>
                    </div>
                    
                    {/* 许可证信息 */}
                    <div className="grid grid-cols-2 gap-3 mb-4">
                      <div className="text-center p-3 bg-teal-50 rounded-lg">
                        <div className="flex items-center justify-center space-x-1 mb-1">
                          <Gauge className="w-4 h-4 text-teal-600" />
                        </div>
                        <div className="text-lg font-bold text-teal-700">
                          {httpHealth.maxPermits}
                        </div>
                        <div className="text-xs text-gray-500">最大许可证</div>
                      </div>
                      <div className="text-center p-3 bg-cyan-50 rounded-lg">
                        <div className="flex items-center justify-center space-x-1 mb-1">
                          <CheckCircle className="w-4 h-4 text-cyan-600" />
                        </div>
                        <div className="text-lg font-bold text-cyan-700">
                          {httpHealth.availablePermits}
                        </div>
                        <div className="text-xs text-gray-500">可用许可证</div>
                      </div>
                    </div>
                    
                    {/* 使用率 */}
                    <div className="pt-3 border-t border-teal-100 text-center">
                      <div className="flex items-center justify-center space-x-2 mb-2">
                        <BarChart3 className="w-4 h-4 text-teal-600" />
                        <span className="text-sm font-medium text-gray-700">使用率</span>
                      </div>
                      <div className="text-xl font-bold text-teal-700">
                        {httpHealth.utilizationRate}
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-2 text-gray-500">
                  <AlertCircle className="w-4 h-4" />
                  <span>暂无数据</span>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </MainLayout>
  )
}