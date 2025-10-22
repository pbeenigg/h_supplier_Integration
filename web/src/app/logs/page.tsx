'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'
import type {
  LogQueryParams,
  DistributionOrdersLogQueryParams,
  DistributionCallLogQueryParams,
  ApiCallLogQueryParams,
  SyncLogQueryParams,
  PageResponse,
  ApiCallLog,
  DistributionCallLog,
  DistributionOrdersLog,
  SyncLog
} from '@/types'
import { FileText, Search, Download, Filter, Calendar, X, Eye, FileCode2, Maximize2, ShoppingCart, Phone, Code, RotateCcw, Loader2 } from 'lucide-react'
import * as pako from 'pako'

type LogType = 'supplier' | 'distribution-call' | 'distribution-orders' | 'sync'

export default function LogsPage() {
  const [activeTab, setActiveTab] = useState<LogType>('distribution-orders')
  const [logs, setLogs] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [queryParams, setQueryParams] = useState<LogQueryParams>({
    page: 0,
    size: 20
  })
  const [pagination, setPagination] = useState({
    totalElements: 0,
    totalPages: 0,
    currentPage: 0,
    hasNext: false,
    hasPrevious: false
  })
  const [selectedLog, setSelectedLog] = useState<any>(null)
  const [showDetailDialog, setShowDetailDialog] = useState(false)
  const [decompressedRequest, setDecompressedRequest] = useState<string | null>(null)
  const [decompressedResponse, setDecompressedResponse] = useState<string | null>(null)
  const [requestDecompressed, setRequestDecompressed] = useState(false)
  const [responseDecompressed, setResponseDecompressed] = useState(false)
  const [decompressingRequest, setDecompressingRequest] = useState(false)
  const [decompressingResponse, setDecompressingResponse] = useState(false)

  // 获取每种日志类型的可用筛选字段
  const getAvailableFilterFields = (logType: LogType) => {
    const commonFields = ['appId', 'supplierId', 'supplierCode', 'isSuccess', 'startTime', 'endTime']

    switch (logType) {
      case 'distribution-orders':
        return [...commonFields, 'traceId', 'businessType', 'hotelKey', 'distributionOrdersKey', 'checkInKey', 'checkOutKey', 'supplierBookingKey']
      case 'distribution-call':
        return [...commonFields, 'traceId', 'businessType', 'hotelKey', 'checkInKey', 'checkOutKey']
      case 'supplier':
        return [...commonFields, 'traceId', 'hotelKey']
      case 'sync':
        return [...commonFields, 'businessType']
      default:
        return commonFields
    }
  }

  // 获取每种日志类型对应的业务类型选项
  const getBusinessTypeOptions = (logType: LogType) => {
    switch (logType) {
      case 'distribution-call':
        return ['queryOrder', 'createOrder', 'cancelOrder', 'getPrice', 'getPrices', 'getHotel', 'orderCheck', 'modifyOrder']
      case 'distribution-orders':
        return ['createOrder', 'cancelOrder', 'orderCheck', 'modifyOrder']
      case 'supplier':
        return ['httpClient']
      case 'sync':
        return ['countries', 'cities', 'hotels', 'nationality', 'giata']
      default:
        return []
    }
  }  // 当切换标签页时，重置查询参数
  const handleTabChange = (newTab: LogType) => {
    setActiveTab(newTab)
    setQueryParams({
      page: 0,
      size: 20
    })
  }

  useEffect(() => {
    fetchLogs()
  }, [activeTab, queryParams])

  const fetchLogs = async () => {
    try {
      setLoading(true)

      // 根据不同的日志类型调用不同的API
      let endpoint = ''
      switch (activeTab) {
        case 'distribution-orders':
          endpoint = '/logs/distribution/orders'
          break
        case 'distribution-call':
          endpoint = '/logs/distribution/call'
          break
        case 'supplier':
          endpoint = '/logs/supplier/call'
          break
        case 'sync':
          endpoint = '/logs/sync'
      }

      // 构建查询参数
      const params = new URLSearchParams()

      // 通用参数
      if (queryParams.appId) params.append('appId', queryParams.appId)
      if (queryParams.supplierId) params.append('supplierId', queryParams.supplierId)
      if (queryParams.supplierCode) params.append('supplierCode', queryParams.supplierCode)
      if (queryParams.isSuccess !== undefined) params.append('isSuccess', String(queryParams.isSuccess))
      if (queryParams.startTime) params.append('startTime', queryParams.startTime)
      if (queryParams.endTime) params.append('endTime', queryParams.endTime)

      // 根据日志类型添加特定参数
      switch (activeTab) {
        case 'distribution-orders':
          // 分销商订单日志参数
          if (queryParams.traceId) params.append('traceId', queryParams.traceId)
          if (queryParams.businessType) params.append('businessType', queryParams.businessType)
          if (queryParams.hotelKey) params.append('hotelKey', queryParams.hotelKey)
          if (queryParams.distributionOrdersKey) params.append('distributionOrdersKey', queryParams.distributionOrdersKey)
          if (queryParams.checkInKey) params.append('checkInKey', queryParams.checkInKey)
          if (queryParams.checkOutKey) params.append('checkOutKey', queryParams.checkOutKey)
          if (queryParams.supplierBookingKey) params.append('supplierBookingKey', queryParams.supplierBookingKey)
          break
        case 'distribution-call':
          // 分销商调用日志参数
          if (queryParams.traceId) params.append('traceId', queryParams.traceId)
          if (queryParams.businessType) params.append('businessType', queryParams.businessType)
          if (queryParams.hotelKey) params.append('hotelKey', queryParams.hotelKey)
          if (queryParams.checkInKey) params.append('checkInKey', queryParams.checkInKey)
          if (queryParams.checkOutKey) params.append('checkOutKey', queryParams.checkOutKey)
          break
        case 'supplier':
          // API调用日志参数
          if (queryParams.traceId) params.append('traceId', queryParams.traceId)
          if (queryParams.hotelKey) params.append('hotelKey', queryParams.hotelKey)
          break
        case 'sync':
          // 同步日志参数 - 无额外参数
          break
      }

      params.append('page', String(queryParams.page || 0))
      params.append('size', String(queryParams.size || 20))

      try {
        // 尝试调用真实API
        const response = await apiClient.get(`${endpoint}?${params.toString()}`)

        if (response.data.data) {
          setLogs(response.data.data.content || [])
          setPagination({
            totalElements: response.data.data.totalElements || 0,
            totalPages: response.data.data.totalPages || 0,
            currentPage: response.data.data.currentPage || 0,
            hasNext: response.data.data.hasNext || false,
            hasPrevious: response.data.data.hasPrevious || false
          })
        }
      } catch (apiError) {
        console.warn('API调用失败，使用模拟数据:', apiError)

      }

      setLoading(false)
    } catch (error) {
      console.error('获取日志失败:', error)
      setLoading(false)
    }
  }



  const handleSearch = () => {
    setQueryParams({ ...queryParams, page: 0 })
  }

  const handlePageChange = (newPage: number) => {
    setQueryParams({ ...queryParams, page: newPage })
  }

  const getStatusBadge = (isSuccess: boolean) => (
    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${isSuccess ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
      }`}>
      {isSuccess ? '成功' : '失败'}
    </span>
  )

  // 全局 businessType 颜色设置函数
  const getBusinessTypeStyles = (businessType: string) => {
    const styles: Record<string, string> = {
      // 分销商调用日志：queryOrder、createOrder、cancelOrder、getPrice、getPrices、getHotel、orderCheck、modifyOrder
      // 分销商订单日志：createOrder、cancelOrder、orderCheck、modifyOrder
      // API调用日志：httpClient
      // 同步日志：countries, cities, hotels, nationality, giata

      // 分销商调用日志|分销商订单日志 业务类型
      'queryOrder': 'bg-teal-100 text-teal-800',        // 青色 - 查询订单
      'getPrice': 'bg-sky-100 text-sky-800',            // 天蓝色 - 获取价格
      'getPrices': 'bg-indigo-100 text-indigo-800',     // 靛蓝色 - 获取多个价格
      'createOrder': 'bg-emerald-100 text-emerald-800', // 翠绿色 - 创建订单
      'modifyOrder': 'bg-amber-100 text-amber-800',     // 琥珀色 - 修改订单
      'cancelOrder': 'bg-red-100 text-red-800',         // 红色 - 取消订单
      'orderCheck': 'bg-violet-100 text-violet-800',    // 紫罗兰色 - 订单检查
      'getHotel': 'bg-lime-100 text-lime-800',          // 青柠色 - 获取酒店

      // API调用日志 业务类型
      'httpClient': 'bg-slate-100 text-slate-800',     // 石板色 - HTTP客户端

      // 同步日志 业务类型
      'countries': 'bg-pink-100 text-pink-800',         // 粉色 - 国家同步
      'cities': 'bg-orange-100 text-orange-800',        // 橙色 - 城市同步
      'hotels': 'bg-green-100 text-green-800',          // 绿色 - 酒店同步
      'nationality': 'bg-cyan-100 text-cyan-800',       // 青色 - 国籍同步
      'giata': 'bg-purple-100 text-purple-800',         // 紫色 - Giata同步

      // 默认样式
      'default': 'bg-gray-100 text-gray-800'            // 灰色 - 默认
    }

    return styles[businessType] || styles.default
  }

  // 全局预订状态颜色设置函数
  const getBookingStatusStyles = (status: string) => {
    const styles: Record<string, string> = {
      'success': 'bg-green-100 text-green-800',
      'pending': 'bg-yellow-100 text-yellow-800',
      'failed': 'bg-gray-100 text-gray-800',
      'cancelled': 'bg-red-100 text-red-800',
      'default': 'bg-gray-100 text-gray-800'
    }

    return styles[status] || styles.default
  }

  // 全局HTTP状态码颜色设置函数
  const getHttpStatusStyles = (status: number) => {
    if (status >= 200 && status < 300) {
      return 'bg-green-100 text-green-800'
    } else if (status >= 400 && status < 500) {
      return 'bg-yellow-100 text-yellow-800'
    } else if (status >= 500) {
      return 'bg-red-100 text-red-800'
    } else {
      return 'bg-gray-100 text-gray-800'
    }
  }

  // 全局HTTP方法颜色设置函数
  const getHttpMethodStyles = (method: string) => {
    const styles: Record<string, string> = {
      'GET': 'bg-blue-100 text-blue-800',
      'POST': 'bg-green-100 text-green-800',
      'PUT': 'bg-yellow-100 text-yellow-800',
      'DELETE': 'bg-red-100 text-red-800',
      'PATCH': 'bg-purple-100 text-purple-800',
      'default': 'bg-gray-100 text-gray-800'
    }

    return styles[method] || styles.default
  }

  // 检测内容类型
  const detectContentType = (content: string): string => {
    if (!content || typeof content !== 'string') return 'TEXT'

    const trimmed = content.trim()

    if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
      try {
        JSON.parse(trimmed)
        return 'JSON'
      } catch {
        return 'TEXT'
      }
    }

    if (trimmed.startsWith('<') && trimmed.includes('>')) {
      return 'XML'
    }

    return 'TEXT'
  }

  // 格式化内容
  const formatContent = (content: string, type: string): string => {
    if (!content) return ''

    try {
      switch (type) {
        case 'JSON':
          return JSON.stringify(JSON.parse(content), null, 2)
        case 'XML':
          // 简单的XML格式化
          return content
            .replace(/></g, '>\n<')
            .replace(/^\s*\n/gm, '')
        default:
          return content
      }
    } catch {
      return content
    }
  }

  // 格式化响应时间显示（毫秒+秒双显）
  const formatResponseTime = (timeMs: number) => {
    if (timeMs < 1000) {
      return `${timeMs}ms`
    } else {
      const seconds = (timeMs / 1000).toFixed(2)
      return `${timeMs}ms (${seconds}s)`
    }
  }

  // 真实的解压缩功能
  const decompressContent = async (compressedContent: string): Promise<string> => {
    try {
      let binaryData: Uint8Array

      // 检查是否是 base64 编码的数据
      if (/^[A-Za-z0-9+/]+=*$/.test(compressedContent)) {
        // 是 base64，尝试解码
        try {
          const binaryString = atob(compressedContent)
          binaryData = new Uint8Array(binaryString.length)
          for (let i = 0; i < binaryString.length; i++) {
            binaryData[i] = binaryString.charCodeAt(i)
          }
        } catch {
          throw new Error('Base64 解码失败')
        }
      } else {
        // 不是 base64，可能是其他格式或未压缩
        throw new Error('不是有效的压缩数据格式')
      }

      // 使用 pako 进行 gzip 解压缩
      const decompressed = pako.inflate(binaryData, { to: 'string' })
      return decompressed
    } catch (error) {
      console.error('解压缩失败:', error)
      // 如果解压缩失败，返回错误信息和原始数据
      return `解压缩失败 (${error instanceof Error ? error.message : '未知错误'}):\n\n原始数据:\n${compressedContent}`
    }
  }  // 处理请求解压缩
  const handleDecompressRequest = async () => {
    if (selectedLog?.originalRequest) {
      try {
        setDecompressingRequest(true)
        const decompressed = await decompressContent(selectedLog.originalRequest)
        setDecompressedRequest(decompressed)
        setRequestDecompressed(true)
      } catch (error) {
        console.error('解压缩失败:', error)
      } finally {
        setDecompressingRequest(false)
      }
    }
  }

  // 处理响应解压缩
  const handleDecompressResponse = async () => {
    if (!selectedLog) return

    try {
      setDecompressingResponse(true)

      let contentToDecompress = ''

      // 根据不同的日志类型获取压缩内容
      if (activeTab === 'distribution-call' || activeTab === 'supplier' || activeTab === 'distribution-orders') {
        // 分销商调用日志
        if (selectedLog.responseBody && selectedLog.responseBody.startsWith('GZIP:')) {
          contentToDecompress = selectedLog.responseBody.substring(5) // 移除 "GZIP:" 前缀
        } else {
          console.warn('响应体没有压缩或格式不正确')
          setDecompressingResponse(false)
          return
        }
      } else {
        // 其他日志类型
        contentToDecompress = selectedLog.originalResponse || selectedLog.responseBody || ''
      }

      if (contentToDecompress) {
        const decompressed = await decompressContent(contentToDecompress)
        setDecompressedResponse(decompressed)
        setResponseDecompressed(true)
      }
    } catch (error) {
      console.error('解压缩失败:', error)
    } finally {
      setDecompressingResponse(false)
    }
  }

  // 重置解压缩状态
  const resetDecompressionState = () => {
    setDecompressedRequest(null)
    setDecompressedResponse(null)
    setRequestDecompressed(false)
    setResponseDecompressed(false)
    setDecompressingRequest(false)
    setDecompressingResponse(false)
  }

  return (
    <MainLayout>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">日志管理</h1>
            <p className="text-gray-600 mt-1">查看和分析系统日志</p>
          </div>
          <Button variant="outline">
            <Download className="w-4 h-4 mr-2" />
            导出日志
          </Button>
        </div>

        {/* 日志类型选项卡 */}
        <Card className="bg-gradient-to-r from-blue-50 to-indigo-50 border-blue-200">
          <CardHeader className="pb-4">
            <div className="flex justify-left">
              <div className="inline-flex bg-white p-1 rounded-xl shadow-sm border border-gray-200">
                {[
                  {
                    key: 'distribution-orders',
                    label: '分销商订单日志',
                    icon: ShoppingCart,
                    color: 'emerald',
                    description: '订单管理日志'
                  },
                  {
                    key: 'distribution-call',
                    label: '分销商调用日志',
                    icon: Phone,
                    color: 'blue',
                    description: '接口调用日志'
                  },
                  {
                    key: 'supplier',
                    label: 'API调用日志',
                    icon: Code,
                    color: 'purple',
                    description: '供应商API日志'
                  },
                  {
                    key: 'sync',
                    label: '数据同步日志',
                    icon: RotateCcw,
                    color: 'orange',
                    description: '数据同步记录'
                  }
                ].map((tab) => {
                  const Icon = tab.icon
                  const isActive = activeTab === tab.key
                  const colorConfig = {
                    emerald: {
                      active: 'bg-emerald-100 text-emerald-700 border-emerald-200 shadow-md',
                      inactive: 'hover:bg-gray-50 text-gray-600 hover:text-emerald-600',
                      icon: isActive ? 'text-emerald-600' : 'text-gray-400'
                    },
                    blue: {
                      active: 'bg-blue-100 text-blue-700 border-blue-200 shadow-md',
                      inactive: 'hover:bg-gray-50 text-gray-600 hover:text-blue-600',
                      icon: isActive ? 'text-blue-600' : 'text-gray-400'
                    },
                    purple: {
                      active: 'bg-purple-100 text-purple-700 border-purple-200 shadow-md',
                      inactive: 'hover:bg-gray-50 text-gray-600 hover:text-purple-600',
                      icon: isActive ? 'text-purple-600' : 'text-gray-400'
                    },
                    orange: {
                      active: 'bg-orange-100 text-orange-700 border-orange-200 shadow-md',
                      inactive: 'hover:bg-gray-50 text-gray-600 hover:text-orange-600',
                      icon: isActive ? 'text-orange-600' : 'text-gray-400'
                    }
                  }

                  return (
                    <button
                      key={tab.key}
                      className={`
                        relative px-6 py-4 rounded-lg border transition-all duration-200 ease-in-out
                        flex flex-col items-center space-y-2 min-w-[160px] group
                        ${isActive
                          ? colorConfig[tab.color as keyof typeof colorConfig].active
                          : `${colorConfig[tab.color as keyof typeof colorConfig].inactive} border-transparent`
                        }
                      `}
                      onClick={() => handleTabChange(tab.key as LogType)}
                    >
                      {/* 图标 */}
                      <Icon
                        className={`
                          w-6 h-6 transition-colors duration-200
                          ${colorConfig[tab.color as keyof typeof colorConfig].icon}
                        `}
                      />

                      {/* 标签 */}
                      <div className="text-center">
                        <div className={`
                          text-sm font-medium transition-colors duration-200
                          ${isActive ? '' : 'group-hover:text-gray-900'}
                        `}>
                          {tab.label}
                        </div>
                        <div className={`
                          text-xs transition-colors duration-200 mt-1
                          ${isActive
                            ? 'opacity-70'
                            : 'text-gray-400 group-hover:text-gray-500'
                          }
                        `}>
                          {tab.description}
                        </div>
                      </div>

                      {/* 活动指示器 */}
                      {isActive && (
                        <div className={`
                          absolute -bottom-0.5 left-1/2 transform -translate-x-1/2 
                          w-8 h-0.5 bg-gradient-to-r 
                          ${tab.color === 'emerald' ? 'from-emerald-400 to-emerald-600' : ''}
                          ${tab.color === 'blue' ? 'from-blue-400 to-blue-600' : ''}
                          ${tab.color === 'purple' ? 'from-purple-400 to-purple-600' : ''}
                          ${tab.color === 'orange' ? 'from-orange-400 to-orange-600' : ''}
                          rounded-full
                        `} />
                      )}
                    </button>
                  )
                })}
              </div>
            </div>
          </CardHeader>
        </Card>

        {/* 搜索和过滤 */}
        <Card>
          <CardHeader>
            <CardTitle>搜索条件</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              {/* 通用字段 - 所有日志类型都有 */}
              {getAvailableFilterFields(activeTab).includes('appId') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">应用ID</label>
                  <Input
                    placeholder="输入应用ID..."
                    value={queryParams.appId || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, appId: e.target.value })}
                  />
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('supplierId') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">供应商ID</label>
                  <Input
                    placeholder="输入供应商ID..."
                    value={queryParams.supplierId || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, supplierId: e.target.value })}
                  />
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('supplierCode') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">供应商编码</label>
                  <Input
                    placeholder="输入供应商编码..."
                    value={queryParams.supplierCode || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, supplierCode: e.target.value })}
                  />
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('traceId') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">追踪ID</label>
                  <Input
                    placeholder="输入追踪ID..."
                    value={queryParams.traceId || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, traceId: e.target.value })}
                  />
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('businessType') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">业务类型</label>
                  <select
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    value={queryParams.businessType || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, businessType: e.target.value || undefined })}
                  >
                    <option value="">全部</option>
                    {getBusinessTypeOptions(activeTab).map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('hotelKey') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">酒店标识</label>
                  <Input
                    placeholder="输入酒店标识..."
                    value={queryParams.hotelKey || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, hotelKey: e.target.value })}
                  />
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('distributionOrdersKey') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">分销商订单号</label>
                  <Input
                    placeholder="输入分销商订单号..."
                    value={queryParams.distributionOrdersKey || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, distributionOrdersKey: e.target.value })}
                  />
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('supplierBookingKey') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">供应商订单号</label>
                  <Input
                    placeholder="输入供应商订单号..."
                    value={queryParams.supplierBookingKey || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, supplierBookingKey: e.target.value })}
                  />
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('checkInKey') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">入住日期</label>
                  <Input
                    placeholder="输入入住日期..."
                    value={queryParams.checkInKey || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, checkInKey: e.target.value })}
                  />
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('checkOutKey') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">离店日期</label>
                  <Input
                    placeholder="输入离店日期..."
                    value={queryParams.checkOutKey || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, checkOutKey: e.target.value })}
                  />
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('isSuccess') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">状态</label>
                  <select
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    value={queryParams.isSuccess === undefined ? '' : String(queryParams.isSuccess)}
                    onChange={(e) => setQueryParams({
                      ...queryParams,
                      isSuccess: e.target.value === '' ? undefined : e.target.value === 'true'
                    })}
                  >
                    <option value="">全部</option>
                    <option value="true">成功</option>
                    <option value="false">失败</option>
                  </select>
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('startTime') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">开始时间</label>
                  <Input
                    type="datetime-local"
                    value={queryParams.startTime || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, startTime: e.target.value })}
                  />
                </div>
              )}

              {getAvailableFilterFields(activeTab).includes('endTime') && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">结束时间</label>
                  <Input
                    type="datetime-local"
                    value={queryParams.endTime || ''}
                    onChange={(e) => setQueryParams({ ...queryParams, endTime: e.target.value })}
                  />
                </div>
              )}
            </div>

            {/* 筛选字段提示 */}
            <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
              <p className="text-sm text-blue-700">
                <strong>当前日志类型可用筛选字段：</strong>
                {(() => {
                  const fields = getAvailableFilterFields(activeTab)
                  const fieldLabels: Record<string, string> = {
                    appId: '应用ID',
                    supplierId: '供应商ID',
                    supplierCode: '供应商编码',
                    traceId: '追踪ID',
                    businessType: '业务类型',
                    isSuccess: '状态',
                    hotelKey: '酒店标识',
                    distributionOrdersKey: '分销商订单号',
                    checkInKey: '入住日期',
                    checkOutKey: '离店日期',
                    supplierBookingKey: '供应商订单号',
                    startTime: '开始时间',
                    endTime: '结束时间'
                  }
                  return fields.map(field => fieldLabels[field] || field).join('、')
                })()}
              </p>
              {getAvailableFilterFields(activeTab).includes('businessType') && (
                <p className="text-sm text-blue-600 mt-2">
                  <strong>业务类型选项：</strong>
                  {getBusinessTypeOptions(activeTab).join('、')}
                </p>
              )}
            </div>

            <div className="mt-4 flex justify-end space-x-2">
              <Button
                variant="outline"
                onClick={() => {
                  setQueryParams({ page: 0, size: 20 })
                }}
              >
                重置
              </Button>
              <Button onClick={handleSearch}>
                <Search className="w-4 h-4 mr-2" />
                搜索
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* 日志列表 */}
        <Card>
          <CardHeader>
            <CardTitle>日志列表</CardTitle>
            <CardDescription>
              共 {pagination.totalElements} 条记录，第 {pagination.currentPage + 1} / {pagination.totalPages} 页
            </CardDescription>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="text-center py-8">
                <div className="text-gray-500">加载中...</div>
              </div>
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b">
                        <th className="text-left py-3 px-4 font-medium text-gray-900">ID</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-900">供应商编码</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-1200">追踪ID</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-600">状态</th>

                        {/* 供应商调用日志列 */}
                        {activeTab === 'supplier' && (
                          <>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">API端点</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">HTTP方法</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">业务类型</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">通道</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">响应状态</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">响应时间</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">重试次数</th>
                          </>
                        )}

                        {/* 分销调用日志列 */}
                        {activeTab === 'distribution-call' && (
                          <>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">API端点</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">业务类型</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">酒店标识</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">响应状态</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">响应时间</th>
                          </>
                        )}

                        {/* 分销商订单日志列 */}
                        {activeTab === 'distribution-orders' && (
                          <>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">业务类型</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">酒店标识</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">入住/离店</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">分销商订单号</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">供应商订单号</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">总金额</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">预订状态</th>
                          </>
                        )}

                        {/* 同步日志列 */}
                        {activeTab === 'sync' && (
                          <>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">业务类型</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">文件名</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">开始时间</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">结束时间</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">总数</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">成功数</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">跳过数</th>
                            <th className="text-left py-3 px-4 font-medium text-gray-900">错误数</th>
                          </>
                        )}

                        <th className="text-left py-3 px-4 font-medium text-gray-900">创建时间</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-900">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {logs.map((log) => (
                        <tr key={log.id} className="border-b hover:bg-gray-50">
                          <td className="py-3 px-4">{log.id}</td>
                          <td className="py-3 px-4">{log.supplierCode}</td>
                          <td className="py-3 px-4 font-mono text-sm min-w-[100px] max-w-[180px] truncate" title={log.traceId || '-'}>
                            {log.traceId || '-'}
                          </td>
                          <td className="py-3 px-4">{getStatusBadge(log.isSuccess)}</td>

                          {/* 供应商调用日志数据 */}
                          {activeTab === 'supplier' && (
                            <>
                              <td className="py-3 px-4 font-mono text-xs max-w-[150px] truncate" title={log.apiEndpoint}>
                                {log.apiEndpoint}
                              </td>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getHttpMethodStyles(log.httpMethod)}`}>
                                  {log.httpMethod}
                                </span>
                              </td>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getBusinessTypeStyles(log.businessType)}`}>
                                  {log.businessType}
                                </span>
                              </td>
                              <td className="py-3 px-4">
                                <span className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-800">
                                  {log.channel}
                                </span>
                              </td>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getHttpStatusStyles(log.responseStatus)}`}>
                                  {log.responseStatus}
                                </span>
                              </td>
                              <td className="py-3 px-4">{formatResponseTime(log.responseTimeMs)}</td>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${log.retryCount > 0 ? 'bg-yellow-100 text-yellow-800' : 'bg-green-100 text-green-800'
                                  }`}>
                                  {log.retryCount}
                                </span>
                              </td>
                            </>
                          )}

                          {/* 分销调用日志数据 */}
                          {activeTab === 'distribution-call' && (
                            <>
                              <td className="py-3 px-4 font-mono text-xs max-w-[150px] truncate" title={log.apiEndpoint}>
                                {log.apiEndpoint}
                              </td>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getBusinessTypeStyles(log.businessType)}`}>
                                  {log.businessType}
                                </span>
                              </td>
                              <td className="py-3 px-4 font-mono text-sm">{log.hotelKey}</td>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getHttpStatusStyles(log.responseStatus)}`}>
                                  {log.responseStatus}
                                </span>
                              </td>
                              <td className="py-3 px-4">{formatResponseTime(log.responseTimeMs)}</td>
                            </>
                          )}

                          {/* 分销商订单日志数据 */}
                          {activeTab === 'distribution-orders' && (
                            <>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getBusinessTypeStyles(log.businessType)}`}>
                                  {log.businessType}
                                </span>
                              </td>
                              <td className="py-3 px-4 font-mono text-sm">{log.hotelKey}</td>
                              <td className="py-3 px-4 text-sm">
                                <div>{log.checkInKey}</div>
                                <div className="text-gray-500">{log.checkOutKey}</div>
                              </td>
                              <td className="py-3 px-4 font-mono text-sm max-w-[120px] truncate" title={log.distributionOrdersKey}>
                                {log.distributionOrdersKey}
                              </td>
                              <td className="py-3 px-4 font-mono text-sm max-w-[120px] truncate" title={log.supplierBookingKey}>
                                {log.supplierBookingKey}
                              </td>
                              <td className="py-3 px-4 font-semibold">
                                {log.totalAmount} {log.currency}
                              </td>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getBookingStatusStyles(log.bookingStatus)}`}>
                                  {log.bookingStatus}
                                </span>
                              </td>
                            </>
                          )}

                          {/* 同步日志数据 */}
                          {activeTab === 'sync' && (
                            <>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getBusinessTypeStyles(log.businessType)}`}>
                                  {log.businessType}
                                </span>
                              </td>
                              <td className="py-3 px-4 font-mono text-xs max-w-[200px] truncate" title={log.fileName}>
                                {log.fileName}
                              </td>
                              <td className="py-3 px-4 text-sm">
                                {new Date(log.startTime).toLocaleString()}
                              </td>
                              <td className="py-3 px-4 text-sm">
                                {new Date(log.endTime).toLocaleString()}
                              </td>
                              <td className="py-3 px-4">
                                <span className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-blue-100 text-blue-800">
                                  {log.totalCount}
                                </span>
                              </td>
                              <td className="py-3 px-4">
                                <span className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-green-100 text-green-800">
                                  {log.successCount}
                                </span>
                              </td>
                              <td className="py-3 px-4">
                                <span className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-800">
                                  {log.skipCount}
                                </span>
                              </td>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${log.errorCount > 0 ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'}`}>
                                  {log.errorCount}
                                </span>
                              </td>
                            </>
                          )}

                          <td className="py-3 px-4 text-sm text-gray-500">
                            {new Date(log.createdAt).toLocaleString()}
                          </td>
                          <td className="py-3 px-4">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setSelectedLog(log)
                                setShowDetailDialog(true)
                                resetDecompressionState()
                              }}
                            >
                              <Eye className="w-4 h-4 mr-1" />
                              详情
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* 分页控件 */}
                <div className="flex justify-between items-center mt-6">
                  <div className="text-sm text-gray-500">
                    显示第 {pagination.currentPage * 20 + 1} - {Math.min((pagination.currentPage + 1) * 20, pagination.totalElements)} 条，共 {pagination.totalElements} 条记录
                  </div>
                  <div className="flex space-x-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={!pagination.hasPrevious}
                      onClick={() => handlePageChange(pagination.currentPage - 1)}
                    >
                      上一页
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={!pagination.hasNext}
                      onClick={() => handlePageChange(pagination.currentPage + 1)}
                    >
                      下一页
                    </Button>
                  </div>
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {/* 详情对话框 */}
        {showDetailDialog && selectedLog && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-6xl max-h-[90vh] flex flex-col">
              {/* 对话框头部 */}
              <div className="flex justify-between items-center p-6 border-b flex-shrink-0">
                <div>
                  <h2 className="text-xl font-semibold">日志详情</h2>
                  <p className="text-sm text-gray-500 mt-1">ID: {selectedLog.id} | 追踪ID: {selectedLog.traceId || '-'}</p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowDetailDialog(false)}
                >
                  <X className="w-4 h-4" />
                </Button>
              </div>

              {/* 对话框内容 */}
              <div className="p-6 overflow-y-auto flex-1">
                <div className="space-y-6">
                  {/* 基本信息 */}
                  <div>
                    <h3 className="text-lg font-medium mb-3">基本信息</h3>
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700">应用ID</label>
                        <p className="mt-1 text-sm text-gray-900">{selectedLog.appId}</p>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700">供应商</label>
                        <p className="mt-1 text-sm text-gray-900">{selectedLog.supplierId} - {selectedLog.supplierCode}</p>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700">状态</label>
                        <p className="mt-1">{getStatusBadge(selectedLog.isSuccess)}</p>
                      </div>
                      {selectedLog.businessType && (
                        <div>
                          <label className="block text-sm font-medium text-gray-700">业务类型</label>
                          <p className="mt-1 text-sm text-gray-900">{selectedLog.businessType}</p>
                        </div>
                      )}
                      {selectedLog.hotelKey && (
                        <div>
                          <label className="block text-sm font-medium text-gray-700">酒店标识</label>
                          <p className="mt-1 text-sm text-gray-900 font-mono">{selectedLog.hotelKey}</p>
                        </div>
                      )}
                      <div>
                        <label className="block text-sm font-medium text-gray-700">创建时间</label>
                        <p className="mt-1 text-sm text-gray-900">{new Date(selectedLog.createdAt).toLocaleString()}</p>
                      </div>
                    </div>
                  </div>

                  {/* 分销商订单日志特有字段 */}
                  {activeTab === 'distribution-orders' && (
                    <>
                      {/* 预订信息 */}
                      <div>
                        <h3 className="text-lg font-medium mb-3">预订信息</h3>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                          <div>
                            <label className="block text-sm font-medium text-gray-700">入住日期</label>
                            <p className="mt-1 text-sm text-gray-900">{selectedLog.checkInKey}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">离店日期</label>
                            <p className="mt-1 text-sm text-gray-900">{selectedLog.checkOutKey}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">房间数</label>
                            <p className="mt-1 text-sm text-gray-900">{selectedLog.rooms}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">晚数</label>
                            <p className="mt-1 text-sm text-gray-900">{selectedLog.nights}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">客人数</label>
                            <p className="mt-1 text-sm text-gray-900">{selectedLog.guests}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">入住人数</label>
                            <p className="mt-1 text-sm text-gray-900">{selectedLog.occupancy}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">总金额</label>
                            <p className="mt-1 text-sm text-gray-900 font-semibold">{selectedLog.totalAmount} {selectedLog.currency}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">预订状态</label>
                            <p className="mt-1">
                              <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getBookingStatusStyles(selectedLog.bookingStatus)}`}>
                                {selectedLog.bookingStatus}
                              </span>
                            </p>
                          </div>
                        </div>
                      </div>

                      {/* 订单键值 */}
                      <div>
                        <h3 className="text-lg font-medium mb-3">订单键值</h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            <label className="block text-sm font-medium text-gray-700">分销商订单号</label>
                            <p className="mt-1 text-sm text-gray-900 font-mono break-all">{selectedLog.distributionOrdersKey}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">供应商订单号</label>
                            <p className="mt-1 text-sm text-gray-900 font-mono break-all">{selectedLog.supplierBookingKey}</p>
                          </div>
                          {selectedLog.roomKey && (
                            <div>
                              <label className="block text-sm font-medium text-gray-700">房型Key</label>
                              <p className="mt-1 text-sm text-gray-900 font-mono">{selectedLog.roomKey}</p>
                            </div>
                          )}
                          {selectedLog.rateKey && (
                            <div>
                              <label className="block text-sm font-medium text-gray-700">房价Key</label>
                              <p className="mt-1 text-sm text-gray-900 font-mono">{selectedLog.rateKey}</p>
                            </div>
                          )}
                        </div>
                      </div>

                      {/* 错误信息 */}
                      {(selectedLog.errorCode || selectedLog.errorMessage) && (
                        <div>
                          <h3 className="text-lg font-medium mb-3 text-red-600">错误信息</h3>
                          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                            {selectedLog.errorCode && (
                              <div className="mb-2">
                                <label className="block text-sm font-medium text-red-700">错误代码</label>
                                <p className="mt-1 text-sm text-red-900 font-mono">{selectedLog.errorCode}</p>
                              </div>
                            )}
                            {selectedLog.errorMessage && (
                              <div>
                                <label className="block text-sm font-medium text-red-700">错误消息</label>
                                <p className="mt-1 text-sm text-red-900">{selectedLog.errorMessage}</p>
                              </div>
                            )}
                          </div>
                        </div>
                      )}

                      {/* 原始请求和响应 */}
                      <div>
                        <h3 className="text-lg font-medium mb-3">原始数据</h3>
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                          {selectedLog.originalRequest && (
                            <div>
                              <div className="flex justify-between items-center mb-2">
                                <label className="block text-sm font-medium text-gray-700">
                                  原始请求
                                  {selectedLog.requestBodyCompressed && !requestDecompressed && (
                                    <span className="text-xs text-orange-600 ml-2">(已压缩)</span>
                                  )}
                                  {requestDecompressed && (
                                    <span className="text-xs text-green-600 ml-2">(已解压缩)</span>
                                  )}
                                </label>
                                <div className="flex space-x-2">
                                  {selectedLog.requestBodyCompressed && !requestDecompressed && (
                                    <Button
                                      variant="outline"
                                      size="sm"
                                      onClick={handleDecompressRequest}
                                      disabled={decompressingRequest}
                                      className="text-xs"
                                    >
                                      <FileCode2 className="w-3 h-3 mr-1" />
                                      {decompressingRequest ? '解压中...' : '解压缩'}
                                    </Button>
                                  )}
                                </div>
                              </div>
                              <div className="bg-gray-50 border rounded-lg p-3 max-h-96 overflow-y-auto">
                                {(() => {
                                  let content = selectedLog.originalRequest
                                  if (requestDecompressed && decompressedRequest) {
                                    content = decompressedRequest
                                  }

                                  const contentType = detectContentType(content)
                                  const formattedContent = formatContent(content, contentType)

                                  return (
                                    <div>
                                      <div className="flex justify-between items-center mb-2">
                                        <span className="text-xs text-gray-500">
                                          格式: {contentType}
                                        </span>
                                      </div>
                                      <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                        {formattedContent}
                                      </pre>
                                    </div>
                                  )
                                })()}
                              </div>
                            </div>
                          )}
                          {selectedLog.originalResponse && (
                            <div>
                              <div className="flex justify-between items-center mb-2">
                                <label className="block text-sm font-medium text-gray-700">
                                  原始响应
                                  {selectedLog.responseBodyCompressed && !responseDecompressed && (
                                    <span className="text-xs text-orange-600 ml-2">(已压缩)</span>
                                  )}
                                  {responseDecompressed && (
                                    <span className="text-xs text-green-600 ml-2">(已解压缩)</span>
                                  )}
                                </label>
                                <div className="flex space-x-2">
                                  {selectedLog.responseBodyCompressed && !responseDecompressed && (
                                    <Button
                                      variant="outline"
                                      size="sm"
                                      onClick={handleDecompressResponse}
                                      disabled={decompressingResponse}
                                      className="text-xs"
                                    >
                                      <FileCode2 className="w-3 h-3 mr-1" />
                                      {decompressingResponse ? '解压中...' : '解压缩'}
                                    </Button>
                                  )}
                                </div>
                              </div>
                              <div className="bg-gray-50 border rounded-lg p-3 max-h-96 overflow-y-auto">
                                {(() => {
                                  let content = selectedLog.originalResponse
                                  if (responseDecompressed && decompressedResponse) {
                                    content = decompressedResponse
                                  }

                                  const contentType = detectContentType(content)
                                  const formattedContent = formatContent(content, contentType)

                                  return (
                                    <div>
                                      <div className="flex justify-between items-center mb-2">
                                        <span className="text-xs text-gray-500">
                                          格式: {contentType}
                                        </span>
                                      </div>
                                      <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                        {formattedContent}
                                      </pre>
                                    </div>
                                  )
                                })()}
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    </>
                  )}

                  {/* 其他日志类型的详情信息 */}
                  {activeTab === 'supplier' && (
                    <>
                      {/* 供应商调用基本信息 */}
                      <div>
                        <h3 className="text-lg font-medium mb-3">供应商调用信息</h3>
                        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                          <div>
                            <label className="block text-sm font-medium text-gray-700">API端点</label>
                            <p className="mt-1 text-sm text-gray-900 font-mono break-all">{selectedLog.apiEndpoint}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">HTTP方法</label>
                            <p className="mt-1">
                              <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getHttpMethodStyles(selectedLog.httpMethod)}`}>
                                {selectedLog.httpMethod}
                              </span>
                            </p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">业务类型</label>
                            <p className="mt-1">
                              <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getBusinessTypeStyles(selectedLog.businessType)}`}>
                                {selectedLog.businessType}
                              </span>
                            </p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">通道</label>
                            <p className="mt-1">
                              <span className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-800">
                                {selectedLog.channel}
                              </span>
                            </p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">响应状态</label>
                            <p className="mt-1">
                              <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getHttpStatusStyles(selectedLog.responseStatus)}`}>
                                {selectedLog.responseStatus}
                              </span>
                            </p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">响应时间</label>
                            <p className="mt-1 text-sm text-gray-900">{formatResponseTime(selectedLog.responseTimeMs)}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">重试次数</label>
                            <p className="mt-1">
                              <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${selectedLog.retryCount > 0 ? 'bg-yellow-100 text-yellow-800' : 'bg-green-100 text-green-800'
                                }`}>
                                {selectedLog.retryCount}
                              </span>
                            </p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">请求大小</label>
                            <p className="mt-1 text-sm text-gray-900">{selectedLog.requestSizeBytes} bytes</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">响应大小</label>
                            <p className="mt-1 text-sm text-gray-900">{selectedLog.responseSizeBytes} bytes</p>
                          </div>
                          {selectedLog.clientIp && (
                            <div>
                              <label className="block text-sm font-medium text-gray-700">客户端IP</label>
                              <p className="mt-1 text-sm text-gray-900 font-mono">{selectedLog.clientIp}</p>
                            </div>
                          )}
                          {selectedLog.userAgent && (
                            <div>
                              <label className="block text-sm font-medium text-gray-700">用户代理</label>
                              <p className="mt-1 text-sm text-gray-900 truncate" title={selectedLog.userAgent}>
                                {selectedLog.userAgent}
                              </p>
                            </div>
                          )}
                        </div>
                      </div>

                      {/* 错误信息 */}
                      {(selectedLog.errorCode || selectedLog.errorMessage) && (
                        <div>
                          <h3 className="text-lg font-medium mb-3 text-red-600">错误信息</h3>
                          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                            {selectedLog.errorCode && (
                              <div className="mb-2">
                                <label className="block text-sm font-medium text-red-700">错误代码</label>
                                <p className="mt-1 text-sm text-red-900 font-mono">{selectedLog.errorCode}</p>
                              </div>
                            )}
                            {selectedLog.errorMessage && (
                              <div>
                                <label className="block text-sm font-medium text-red-700">错误消息</label>
                                <p className="mt-1 text-sm text-red-900">{selectedLog.errorMessage}</p>
                              </div>
                            )}
                          </div>
                        </div>
                      )}

                      {/* 原始请求和响应数据 */}
                      <div>
                        <h3 className="text-lg font-medium mb-3">原始数据</h3>
                        <div className="space-y-4">
                          {/* 请求头 */}
                          {selectedLog.requestHeaders && (
                            <div>
                              <label className="block text-sm font-medium text-gray-700 mb-2">请求头</label>
                              <div className="bg-gray-50 border rounded-lg p-3 max-h-32 overflow-y-auto">
                                <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                  {formatContent(selectedLog.requestHeaders, 'JSON')}
                                </pre>
                              </div>
                            </div>
                          )}

                          {/* 请求参数 */}
                          {selectedLog.requestParams && (
                            <div>
                              <label className="block text-sm font-medium text-gray-700 mb-2">请求参数</label>
                              <div className="bg-gray-50 border rounded-lg p-3 max-h-32 overflow-y-auto">
                                <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                  {formatContent(selectedLog.requestParams, 'JSON')}
                                </pre>
                              </div>
                            </div>
                          )}

                          {/* 请求体 */}
                          {selectedLog.requestBody && (
                            <div>
                              <label className="block text-sm font-medium text-gray-700 mb-2">请求体</label>
                              <div className="bg-gray-50 border rounded-lg p-3 max-h-64 overflow-y-auto">
                                <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                  {formatContent(selectedLog.requestBody, detectContentType(selectedLog.requestBody))}
                                </pre>
                              </div>
                            </div>
                          )}

                          {/* 响应头 */}
                          {selectedLog.responseHeaders && (
                            <div>
                              <label className="block text-sm font-medium text-gray-700 mb-2">响应头</label>
                              <div className="bg-gray-50 border rounded-lg p-3 max-h-32 overflow-y-auto">
                                <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                  {formatContent(selectedLog.responseHeaders, 'JSON')}
                                </pre>
                              </div>
                            </div>
                          )}

                          {/* 响应体 */}
                          <div>
                            <div className="flex justify-between items-center mb-2">
                              <label className="block text-sm font-medium text-gray-700">
                                响应体
                                {(selectedLog.responseBodyCompressed || (selectedLog.responseBody && selectedLog.responseBody.startsWith('GZIP:'))) && !responseDecompressed && (
                                  <span className="text-xs text-orange-600 ml-2">(已压缩)</span>
                                )}
                                {responseDecompressed && (
                                  <span className="text-xs text-green-600 ml-2">(已解压缩)</span>
                                )}
                              </label>
                              <div className="flex space-x-2">
                                {(selectedLog.responseBodyCompressed || (selectedLog.responseBody && selectedLog.responseBody.startsWith('GZIP:'))) && !responseDecompressed && (
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={handleDecompressResponse}
                                    disabled={decompressingResponse}
                                    className="text-xs"
                                  >
                                    <FileCode2 className="w-3 h-3 mr-1" />
                                    {decompressingResponse ? '解压中...' : '解压缩'}
                                  </Button>
                                )}
                              </div>
                            </div>
                            <div className="bg-gray-50 border rounded-lg p-3 max-h-64 overflow-y-auto">
                              {(() => {
                                let content = selectedLog.responseBody

                                // 处理GZIP压缩数据
                                if (content && content.startsWith('GZIP:')) {
                                  const base64Data = content.substring(5)
                                  if (responseDecompressed && decompressedResponse) {
                                    content = decompressedResponse
                                  } else {
                                    content = base64Data
                                  }
                                } else if (responseDecompressed && decompressedResponse) {
                                  content = decompressedResponse
                                }

                                const contentType = detectContentType(content)
                                const formattedContent = formatContent(content, contentType)

                                return (
                                  <div>
                                    <div className="flex justify-between items-center mb-2">
                                      <span className="text-xs text-gray-500">
                                        格式: {contentType}
                                      </span>
                                    </div>
                                    <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                      {formattedContent}
                                    </pre>
                                  </div>
                                )
                              })()}
                            </div>
                          </div>
                        </div>
                      </div>
                    </>
                  )}

                  {/* 分销商调用日志详情信息 */}
                  {activeTab === 'distribution-call' && (
                    <>
                      {/* 请求信息 */}
                      <div>
                        <h3 className="text-lg font-medium mb-3">请求信息</h3>
                        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                          <div>
                            <label className="block text-sm font-medium text-gray-700">API端点</label>
                            <p className="mt-1 text-sm text-gray-900 font-mono break-all">{selectedLog.apiEndpoint}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">HTTP方法</label>
                            <p className="mt-1">
                              <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getHttpMethodStyles(selectedLog.httpMethod)}`}>
                                {selectedLog.httpMethod}
                              </span>
                            </p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">业务类型</label>
                            <p className="mt-1 text-sm text-gray-900">{selectedLog.businessType}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">客户端IP</label>
                            <p className="mt-1 text-sm text-gray-900 font-mono">{selectedLog.clientIp}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">用户代理</label>
                            <p className="mt-1 text-sm text-gray-900 truncate" title={selectedLog.userAgent}>
                              {selectedLog.userAgent}
                            </p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">酒店标识</label>
                            <p className="mt-1 text-sm text-gray-900 font-mono">{selectedLog.hotelKey}</p>
                          </div>
                        </div>
                      </div>

                      {/* 响应信息 */}
                      <div>
                        <h3 className="text-lg font-medium mb-3">响应信息</h3>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                          <div>
                            <label className="block text-sm font-medium text-gray-700">响应状态</label>
                            <p className="mt-1">
                              <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${getHttpStatusStyles(selectedLog.responseStatus)}`}>
                                {selectedLog.responseStatus}
                              </span>
                            </p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">响应时间</label>
                            <p className="mt-1 text-sm text-gray-900">{formatResponseTime(selectedLog.responseTimeMs)}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">请求压缩</label>
                            <p className="mt-1">
                              <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${selectedLog.requestBodyCompressed ? 'bg-orange-100 text-orange-800' : 'bg-gray-100 text-gray-800'
                                }`}>
                                {selectedLog.requestBodyCompressed ? '已压缩' : '未压缩'}
                              </span>
                            </p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">响应压缩</label>
                            <p className="mt-1">
                              <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${selectedLog.responseBodyCompressed ? 'bg-orange-100 text-orange-800' : 'bg-gray-100 text-gray-800'
                                }`}>
                                {selectedLog.responseBodyCompressed ? '已压缩' : '未压缩'}
                              </span>
                            </p>
                          </div>
                        </div>
                      </div>

                      {/* 错误信息 */}
                      {(selectedLog.errorCode || selectedLog.errorMessage) && (
                        <div>
                          <h3 className="text-lg font-medium mb-3 text-red-600">错误信息</h3>
                          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                            {selectedLog.errorCode && (
                              <div className="mb-2">
                                <label className="block text-sm font-medium text-red-700">错误代码</label>
                                <p className="mt-1 text-sm text-red-900 font-mono">{selectedLog.errorCode}</p>
                              </div>
                            )}
                            {selectedLog.errorMessage && (
                              <div>
                                <label className="block text-sm font-medium text-red-700">错误消息</label>
                                <p className="mt-1 text-sm text-red-900">{selectedLog.errorMessage}</p>
                              </div>
                            )}
                          </div>
                        </div>
                      )}

                      {/* 原始请求和响应数据 */}
                      <div>
                        <h3 className="text-lg font-medium mb-3">原始数据</h3>
                        <div className="space-y-4">
                          {/* 请求头 */}
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">请求头</label>
                            <div className="bg-gray-50 border rounded-lg p-3 max-h-32 overflow-y-auto">
                              <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                {formatContent(selectedLog.requestHeaders, 'JSON')}
                              </pre>
                            </div>
                          </div>

                          {/* 请求参数 */}
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">请求参数</label>
                            <div className="bg-gray-50 border rounded-lg p-3 max-h-32 overflow-y-auto">
                              <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                {formatContent(selectedLog.requestParams, 'JSON')}
                              </pre>
                            </div>
                          </div>

                          {/* 请求体 */}
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                              请求体
                              {selectedLog.requestBodyCompressed && (
                                <span className="text-xs text-orange-600 ml-2">(已压缩)</span>
                              )}
                            </label>
                            <div className="bg-gray-50 border rounded-lg p-3 max-h-64 overflow-y-auto">
                              <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                {formatContent(selectedLog.requestBody, detectContentType(selectedLog.requestBody))}
                              </pre>
                            </div>
                          </div>

                          {/* 响应体 */}
                          <div>
                            <div className="flex justify-between items-center mb-2">
                              <label className="block text-sm font-medium text-gray-700">
                                响应体
                                {(selectedLog.responseBodyCompressed || (selectedLog.responseBody && selectedLog.responseBody.startsWith('GZIP:'))) && !responseDecompressed && (
                                  <span className="text-xs text-orange-600 ml-2">(已压缩)</span>
                                )}
                                {responseDecompressed && (
                                  <span className="text-xs text-green-600 ml-2">(已解压缩)</span>
                                )}
                              </label>
                              <div className="flex space-x-2">
                                {(selectedLog.responseBodyCompressed || (selectedLog.responseBody && selectedLog.responseBody.startsWith('GZIP:'))) && !responseDecompressed && (
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={handleDecompressResponse}
                                    disabled={decompressingResponse}
                                    className="text-xs"
                                  >
                                    <FileCode2 className="w-3 h-3 mr-1" />
                                    {decompressingResponse ? '解压中...' : '解压缩'}
                                  </Button>
                                )}
                              </div>
                            </div>
                            <div className="bg-gray-50 border rounded-lg p-3 max-h-64 overflow-y-auto">
                              {(() => {
                                let content = selectedLog.responseBody

                                // 处理GZIP压缩数据
                                if (content && content.startsWith('GZIP:')) {
                                  const base64Data = content.substring(5)
                                  if (responseDecompressed && decompressedResponse) {
                                    content = decompressedResponse
                                  } else {
                                    content = base64Data
                                  }
                                } else if (responseDecompressed && decompressedResponse) {
                                  content = decompressedResponse
                                }

                                const contentType = detectContentType(content)
                                const formattedContent = formatContent(content, contentType)

                                return (
                                  <div>
                                    <div className="flex justify-between items-center mb-2">
                                      <span className="text-xs text-gray-500">
                                        格式: {contentType}
                                      </span>
                                    </div>
                                    <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all">
                                      {formattedContent}
                                    </pre>
                                  </div>
                                )
                              })()}
                            </div>
                          </div>
                        </div>
                      </div>
                    </>
                  )}

                  {/* 同步日志详情信息 */}
                  {activeTab === 'sync' && (
                    <>
                      {/* 同步基本信息 */}
                      <div>
                        <h3 className="text-lg font-medium mb-3">同步信息</h3>
                        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                          <div>
                            <label className="block text-sm font-medium text-gray-700">文件名</label>
                            <p className="mt-1 text-sm text-gray-900 font-mono break-all">{selectedLog.fileName}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">开始时间</label>
                            <p className="mt-1 text-sm text-gray-900">{new Date(selectedLog.startTime).toLocaleString()}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">结束时间</label>
                            <p className="mt-1 text-sm text-gray-900">{new Date(selectedLog.endTime).toLocaleString()}</p>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700">执行时长</label>
                            <p className="mt-1 text-sm text-gray-900">
                              {(() => {
                                const start = new Date(selectedLog.startTime).getTime()
                                const end = new Date(selectedLog.endTime).getTime()
                                const duration = Math.max(0, end - start)
                                return duration < 1000 ? `${duration}ms` : `${(duration / 1000).toFixed(2)}s`
                              })()}
                            </p>
                          </div>
                        </div>
                      </div>

                      {/* 同步统计信息 */}
                      <div>
                        <h3 className="text-lg font-medium mb-3">统计信息</h3>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                            <div className="text-center">
                              <div className="text-2xl font-bold text-blue-600">{selectedLog.totalCount}</div>
                              <div className="text-sm text-blue-700">总数</div>
                            </div>
                          </div>
                          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                            <div className="text-center">
                              <div className="text-2xl font-bold text-green-600">{selectedLog.successCount}</div>
                              <div className="text-sm text-green-700">成功数</div>
                            </div>
                          </div>
                          <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                            <div className="text-center">
                              <div className="text-2xl font-bold text-gray-600">{selectedLog.skipCount}</div>
                              <div className="text-sm text-gray-700">跳过数</div>
                            </div>
                          </div>
                          <div className={`${selectedLog.errorCount > 0 ? 'bg-red-50 border-red-200' : 'bg-green-50 border-green-200'} border rounded-lg p-4`}>
                            <div className="text-center">
                              <div className={`text-2xl font-bold ${selectedLog.errorCount > 0 ? 'text-red-600' : 'text-green-600'}`}>
                                {selectedLog.errorCount}
                              </div>
                              <div className={`text-sm ${selectedLog.errorCount > 0 ? 'text-red-700' : 'text-green-700'}`}>错误数</div>
                            </div>
                          </div>
                        </div>

                        {/* 成功率显示 */}
                        <div className="mt-4">
                          <div className="flex justify-between items-center mb-2">
                            <span className="text-sm font-medium text-gray-700">成功率</span>
                            <span className="text-sm text-gray-900">
                              {selectedLog.totalCount > 0
                                ? `${((selectedLog.successCount / selectedLog.totalCount) * 100).toFixed(2)}%`
                                : '0%'
                              }
                            </span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div
                              className="bg-green-500 h-2 rounded-full transition-all duration-300"
                              style={{
                                width: selectedLog.totalCount > 0
                                  ? `${(selectedLog.successCount / selectedLog.totalCount) * 100}%`
                                  : '0%'
                              }}
                            ></div>
                          </div>
                        </div>
                      </div>

                      {/* 错误信息 */}
                      {selectedLog.message && (
                        <div>
                          <h3 className="text-lg font-medium mb-3 text-amber-600">消息信息</h3>
                          <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
                            <p className="text-sm text-amber-900">{selectedLog.message}</p>
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>

              {/* 对话框底部 */}
              <div className="flex justify-end p-6 border-t bg-gray-50 flex-shrink-0">
                <Button
                  onClick={() => setShowDetailDialog(false)}
                  className="min-w-[80px]"
                >
                  关闭
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </MainLayout>
  )
}
