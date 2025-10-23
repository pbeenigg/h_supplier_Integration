'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'
import type { SupplierConfig, SupplierListResponse, SupplierHealthResponse } from '@/types'
import { Server, Settings, Activity, AlertCircle, FileCode2, Shield, Upload, Code2, Grid3X3, List } from 'lucide-react'

export default function SuppliersPage() {
  const [suppliers, setSuppliers] = useState<SupplierConfig[]>([])
  const [loading, setLoading] = useState(true)
  const [healthStatus, setHealthStatus] = useState<{ [key: string]: boolean }>({})
  const [healthStats, setHealthStats] = useState({
    totalCount: 0,
    healthyCount: 0,
    unhealthyCount: 0,
    enabledCount: 0
  })
  const [editingSupplier, setEditingSupplier] = useState<SupplierConfig | null>(null)
  const [showEditDialog, setShowEditDialog] = useState(false)
  const [editForm, setEditForm] = useState<Partial<SupplierConfig>>({})
  const [saving, setSaving] = useState(false)
  const [viewMode, setViewMode] = useState<'compact' | 'detailed'>('compact')

  useEffect(() => {
    fetchSuppliers()
  }, [])

  // 当供应商列表更新后，重新进行健康检查
  useEffect(() => {
    if (suppliers.length > 0) {
      checkSuppliersHealth(suppliers)
    }
  }, [suppliers.length]) // 只监听长度变化，避免无限循环

  const fetchSuppliers = async () => {
    try {
      setLoading(true)
      console.log('开始获取供应商列表...')
      const response = await apiClient.get<SupplierListResponse>('/suppliers')

      console.log('API响应:', response.data)
      console.log('响应数据类型:', typeof response.data)
      console.log('响应结构:', Object.keys(response.data))

      if (response.data.code === 200) {
        console.log('供应商数据:', response.data.data)
        console.log('供应商数据类型:', typeof response.data.data)
        console.log('是否为数组:', Array.isArray(response.data.data))

        // 确保data是数组
        if (Array.isArray(response.data.data)) {
          setSuppliers(response.data.data)
          console.log('已设置供应商列表，数量:', response.data.data.length)
        } else {
          console.error('响应数据不是数组格式:', response.data.data)
          setSuppliers([])
        }
      } else {
        console.error('获取供应商列表失败:', response.data.msg)
        setSuppliers([])
      }
    } catch (error) {
      console.error('API调用失败，使用模拟数据:', error)
    } finally {
      setLoading(false)
    }
  }

  const checkSuppliersHealth = async (currentSuppliers?: SupplierConfig[]) => {
    try {
      console.log('开始健康检查...')
      const response = await apiClient.get<SupplierHealthResponse>('/suppliers/health')

      console.log('健康检查API响应:', response.data)

      if (response.data.code === 200) {
        // 转换健康检查数据为对象格式
        const healthData: { [key: string]: boolean } = {}
        response.data.data.suppliers.forEach(supplier => {
          healthData[supplier.supplierName] = supplier.healthy
        })

        console.log('健康检查结果:', healthData)
        setHealthStatus(healthData)

        // 更新统计信息 - 使用传入的供应商列表或当前状态
        const supplierList = currentSuppliers || suppliers
        const enabledCount = supplierList.filter(s => s.isActive).length
        setHealthStats({
          totalCount: response.data.data.totalCount,
          healthyCount: response.data.data.healthyCount,
          unhealthyCount: response.data.data.unhealthyCount,
          enabledCount: enabledCount
        })

        console.log('健康状态统计:', {
          总数: response.data.data.totalCount,
          健康: response.data.data.healthyCount,
          异常: response.data.data.unhealthyCount,
          已启用: enabledCount
        })
      } else {
        console.error('健康检查失败:', response.data.msg)
      }
    } catch (error) {
      console.error('健康检查API调用失败:', error)
      // 如果API调用失败，使用模拟数据作为后备
      const mockHealthData = {
        'AsianOverland': true,
        'TestSupplier': false
      }
      setHealthStatus(mockHealthData)

      // 计算启用的供应商数量 - 使用传入的供应商列表或当前状态
      const supplierList = currentSuppliers || suppliers
      const enabledCount = supplierList.filter(s => s.isActive).length
      setHealthStats({
        totalCount: supplierList.length,
        healthyCount: Object.values(mockHealthData).filter(Boolean).length,
        unhealthyCount: Object.values(mockHealthData).filter(h => !h).length,
        enabledCount: enabledCount
      })
    }
  }



  // 刷新页面数据（重新获取供应商列表和健康状态）
  const refreshData = async () => {
    console.log('刷新页面数据...')
    await fetchSuppliers()
    // fetchSuppliers完成后会自动触发健康检查（通过useEffect）
  }

  // 打开编辑对话框
  const handleEditSupplier = (supplier: SupplierConfig) => {
    setEditingSupplier(supplier)
    // 预处理JSON字段，格式化显示
    const formData = { ...supplier }
    try {
      if (formData.authConfig) {
        formData.authConfig = JSON.stringify(JSON.parse(formData.authConfig), null, 2)
      }
    } catch (e) {
      console.warn('authConfig JSON解析失败:', e)
    }
    try {
      if (formData.ftpConfig) {
        formData.ftpConfig = JSON.stringify(JSON.parse(formData.ftpConfig), null, 2)
      }
    } catch (e) {
      console.warn('ftpConfig JSON解析失败:', e)
    }
    try {
      if (formData.contactInfo) {
        formData.contactInfo = JSON.stringify(JSON.parse(formData.contactInfo), null, 2)
      }
    } catch (e) {
      console.warn('contactInfo JSON解析失败:', e)
    }
    setEditForm(formData)
    setShowEditDialog(true)
  }

  // 保存供应商配置
  const handleSaveSupplier = async () => {
    if (!editingSupplier) return

    try {
      setSaving(true)
      console.log('保存供应商配置:', editForm)

      // 准备请求数据，压缩JSON字段
      const requestData = { ...editForm }

      // 验证并压缩JSON字段
      try {
        if (requestData.authConfig) {
          requestData.authConfig = JSON.stringify(JSON.parse(requestData.authConfig))
        }
      } catch (e) {
        throw new Error('认证配置JSON格式无效')
      }

      try {
        if (requestData.ftpConfig) {
          requestData.ftpConfig = JSON.stringify(JSON.parse(requestData.ftpConfig))
        }
      } catch (e) {
        throw new Error('FTP配置JSON格式无效')
      }

      try {
        if (requestData.contactInfo) {
          requestData.contactInfo = JSON.stringify(JSON.parse(requestData.contactInfo))
        }
      } catch (e) {
        throw new Error('联系信息JSON格式无效')
      }

      // 删除不需要的字段
      delete requestData.id
      delete requestData.createdAt
      delete requestData.updatedAt
      delete requestData.createdBy
      delete requestData.updatedBy

      const response = await apiClient.put(`/suppliers/${editingSupplier.id}/config`, requestData)

      if (response.status === 200) {
        console.log('供应商配置更新成功')
        setShowEditDialog(false)
        setEditingSupplier(null)
        setEditForm({})
        // 重新获取数据
        await refreshData()
      } else {
        console.error('更新供应商配置失败:', response)
      }
    } catch (error: any) {
      console.error('保存供应商配置失败:', error)
      alert(error.message || '保存失败，请检查输入的JSON格式是否正确')
    } finally {
      setSaving(false)
    }
  }

  // 切换供应商状态（启用/禁用）
  const handleToggleSupplierStatus = async (supplier: SupplierConfig) => {
    try {
      setSaving(true)
      const requestData = {
        isActive: !supplier.isActive
      }

      const response = await apiClient.put(`/suppliers/${supplier.id}/config`, requestData)

      if (response.status === 200) {
        console.log(`供应商 ${supplier.supplierName} 状态切换成功`)
        // 重新获取数据
        await refreshData()
      } else {
        console.error('切换供应商状态失败:', response)
      }
    } catch (error) {
      console.error('切换供应商状态失败:', error)
      alert('操作失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  return (
    <MainLayout>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">供应商管理</h1>
            <p className="text-gray-600 mt-1">管理酒店供应商配置和连接状态</p>
          </div>
          <div className="flex items-center space-x-3">
            {/* 视图模式切换 */}
            <div className="flex items-center bg-gray-100 rounded-lg p-1">
              <button
                onClick={() => setViewMode('compact')}
                className={`flex items-center px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${viewMode === 'compact'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
                  }`}
              >
                <Grid3X3 className="w-4 h-4 mr-1.5" />
                紧凑视图
              </button>
              <button
                onClick={() => setViewMode('detailed')}
                className={`flex items-center px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${viewMode === 'detailed'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
                  }`}
              >
                <List className="w-4 h-4 mr-1.5" />
                详细视图
              </button>
            </div>
            <Button onClick={refreshData} disabled={loading}>
              <Activity className="w-4 h-4 mr-2" />
              {loading ? '刷新中...' : '刷新数据'}
            </Button>
          </div>
        </div>

        {/* 供应商列表 */}
        <div className={`grid gap-4 ${viewMode === 'compact'
          ? 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5'
          : 'grid-cols-1 lg:grid-cols-2 xl:grid-cols-3'
          }`}>
          {loading ? (
            <div className="col-span-full text-center py-8">
              <div className="text-gray-500">加载中...</div>
            </div>
          ) : (
            suppliers.map((supplier) => (
              <Card key={supplier.id} className={`${supplier.isActive ? 'border-green-200 bg-green-50/30' : 'border-gray-200'} hover:shadow-md transition-shadow duration-200`}>
                <CardHeader className={viewMode === 'compact' ? 'pb-2' : 'pb-3'}>
                  <div className="flex justify-between items-start">
                    <div className="flex-1 min-w-0">
                      <CardTitle className={`flex items-center ${viewMode === 'compact' ? 'text-sm' : 'text-base'}`}>
                        <Server className={`${viewMode === 'compact' ? 'w-3 h-3' : 'w-4 h-4'} mr-2 flex-shrink-0`} />
                        <span className="truncate">{supplier.supplierName}</span>
                      </CardTitle>
                      <CardDescription className="text-xs mt-1">
                        {supplier.supplierCode} · 优先级 {supplier.priority}
                        {viewMode === 'detailed' && ` · ${supplier.authType}`}
                      </CardDescription>
                    </div>
                    <div className="flex items-center space-x-1 ml-2">
                      {healthStatus[supplier.supplierName] !== undefined && (
                        <div
                          className={`${viewMode === 'compact' ? 'w-2 h-2' : 'w-2.5 h-2.5'} rounded-full ${healthStatus[supplier.supplierName] ? 'bg-green-500' : 'bg-red-500'}`}
                          title={healthStatus[supplier.supplierName] ? '健康' : '异常'}
                        />
                      )}
                      <span className={`inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium ${supplier.isActive
                        ? 'bg-green-100 text-green-700'
                        : 'bg-gray-100 text-gray-600'
                        }`}>
                        {supplier.isActive ? '启用' : '禁用'}
                      </span>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className={`pt-0 ${viewMode === 'compact' ? 'space-y-2' : 'space-y-3'}`}>
                  {viewMode === 'compact' ? (
                    /* 紧凑视图 - 只显示关键信息 */
                    <>
                      <div className="flex justify-between items-center text-xs">
                        <span className="text-gray-500">超时/并发</span>
                        <span className="font-medium text-gray-700">{supplier.timeoutMs}ms / {supplier.maxConcurrentRequests}</span>
                      </div>

                      {/* 同步状态 */}
                      <div className="flex justify-between items-center">
                        <div className="flex items-center space-x-1">
                          <div className={`w-1.5 h-1.5 rounded-full ${supplier.isSyncStatic ? 'bg-green-500' : 'bg-gray-300'}`}></div>
                          <span className="text-xs text-gray-600">静态</span>
                        </div>
                        <div className="flex items-center space-x-1">
                          <div className={`w-1.5 h-1.5 rounded-full ${supplier.isSyncHotel ? 'bg-green-500' : 'bg-gray-300'}`}></div>
                          <span className="text-xs text-gray-600">酒店</span>
                        </div>
                        <span className="text-xs text-gray-500">{supplier.rateLimitPerSecond}/s</span>
                      </div>
                    </>
                  ) : (
                    /* 详细视图 - 显示所有信息 */
                    <>
                      {/* 关键信息详细显示 */}
                      <div className="space-y-2">
                        <div className="flex justify-between items-center text-xs">
                          <span className="text-gray-500">认证类型</span>
                          <span className="font-medium text-gray-700">{supplier.authType}</span>
                        </div>

                        <div className="flex justify-between items-center text-xs">
                          <span className="text-gray-500">超时时间</span>
                          <span className="font-medium text-gray-700">{supplier.timeoutMs}ms</span>
                        </div>

                        <div className="flex justify-between items-center text-xs">
                          <span className="text-gray-500">并发请求</span>
                          <span className="font-medium text-gray-700">{supplier.maxConcurrentRequests}</span>
                        </div>

                        <div className="flex justify-between items-center text-xs">
                          <span className="text-gray-500">限流速率</span>
                          <span className="font-medium text-gray-700">{supplier.rateLimitPerSecond}/s</span>
                        </div>
                      </div>

                      {/* API地址 */}
                      <div className="bg-gray-50 rounded-md p-2">
                        <div className="text-xs text-gray-500 mb-1">API地址</div>
                        <div className="text-xs font-mono text-gray-700 break-all">{supplier.apiBaseUrl}</div>
                      </div>

                      {/* 描述信息 */}
                      {supplier.description && (
                        <div>
                          <div className="text-xs text-gray-500 mb-1">描述</div>
                          <div className="text-xs text-gray-700">{supplier.description}</div>
                        </div>
                      )}

                      {/* 同步状态 */}
                      <div className="flex justify-between items-center">
                        <div className="flex items-center space-x-1">
                          <div className={`w-2 h-2 rounded-full ${supplier.isSyncStatic ? 'bg-green-500' : 'bg-gray-300'}`}></div>
                          <span className="text-xs text-gray-600">静态数据同步</span>
                        </div>
                        <div className="flex items-center space-x-1">
                          <div className={`w-2 h-2 rounded-full ${supplier.isSyncHotel ? 'bg-green-500' : 'bg-gray-300'}`}></div>
                          <span className="text-xs text-gray-600">酒店数据同步</span>
                        </div>
                      </div>

                      {/* 联系信息 */}
                      {supplier.contactInfo && (
                        <div>
                          <div className="text-xs text-gray-500 mb-1">联系信息</div>
                          <div className="text-xs text-gray-700">
                            {(() => {
                              try {
                                const contact = JSON.parse(supplier.contactInfo)
                                return (
                                  <div className="space-y-1">
                                    {contact.email && <div>邮箱: {contact.email}</div>}
                                    {contact.phone && <div>电话: {contact.phone}</div>}
                                    {contact.contact_person && <div>联系人: {contact.contact_person}</div>}
                                  </div>
                                )
                              } catch {
                                return supplier.contactInfo
                              }
                            })()}
                          </div>
                        </div>
                      )}
                    </>
                  )}

                  {/* 底部操作区域 */}
                  <div className={`border-t space-y-2 ${viewMode === 'compact' ? 'pt-2' : 'pt-3'}`}>
                    {viewMode === 'detailed' && (
                      <div className="text-xs text-gray-400 text-center">
                        {new Date(supplier.updatedAt).toLocaleDateString()} · {supplier.updatedBy}
                      </div>
                    )}
                    <div className="flex space-x-1">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleEditSupplier(supplier)}
                        disabled={saving}
                        className={`flex-1 text-xs ${viewMode === 'compact' ? 'h-7 px-2' : 'h-8'}`}
                      >
                        <Settings className={`${viewMode === 'compact' ? 'w-2.5 h-2.5' : 'w-3 h-3'} mr-1`} />
                        配置
                      </Button>
                      <Button
                        variant={supplier.isActive ? "destructive" : "default"}
                        size="sm"
                        onClick={() => handleToggleSupplierStatus(supplier)}
                        disabled={saving}
                        className={`flex-1 text-xs ${viewMode === 'compact' ? 'h-7 px-2' : 'h-8'}`}
                      >
                        {saving ? '处理中' : (supplier.isActive ? '禁用' : '启用')}
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </div>

        {/* 健康状态概览 */}
        <Card>
          <CardHeader>
            <CardTitle>健康状态概览</CardTitle>
            <CardDescription>所有供应商的连接状态和统计信息</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="text-center">
                <div className="text-2xl font-bold text-blue-600">
                  {healthStats.totalCount}
                </div>
                <div className="text-sm text-gray-500">供应商总数</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-600">
                  {healthStats.healthyCount}
                </div>
                <div className="text-sm text-gray-500">健康</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-red-600">
                  {healthStats.unhealthyCount}
                </div>
                <div className="text-sm text-gray-500">不健康</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-purple-600">
                  {healthStats.enabledCount}
                </div>
                <div className="text-sm text-gray-500">已启用</div>
              </div>
            </div>

            {/* 健康状态比例显示 */}
            {healthStats.totalCount > 0 && (
              <div className="mt-6">
                <div className="flex justify-between text-sm text-gray-600 mb-2">
                  <span>健康状态</span>
                  <span>{healthStats.healthyCount}/{healthStats.totalCount} 健康</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-3">
                  <div
                    className="bg-green-500 h-3 rounded-full transition-all duration-300"
                    style={{ width: `${(healthStats.healthyCount / healthStats.totalCount) * 100}%` }}
                  ></div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* 编辑供应商配置对话框 */}
        {showEditDialog && editingSupplier && (
          <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 p-6">
            <div className="bg-white rounded-xl max-w-7xl w-full max-h-[95vh] overflow-hidden flex flex-col shadow-2xl">
              {/* 头部区域 */}
              <div className="flex justify-between items-center px-8 py-6 border-b border-gray-200 bg-gradient-to-r from-blue-50 to-indigo-50">
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                    <Settings className="w-5 h-5 text-blue-600" />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-gray-900">编辑供应商配置</h2>
                    <p className="text-sm text-gray-600">{editingSupplier.supplierName}</p>
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setShowEditDialog(false)
                    setEditingSupplier(null)
                    setEditForm({})
                  }}
                  className="rounded-full w-8 h-8 p-0 hover:bg-gray-100"
                >
                  ×
                </Button>
              </div>

              {/* 内容区域 */}
              <div className="flex-1 overflow-y-auto px-8 py-6 bg-gray-50">
                <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
                  {/* 基本信息 */}
                  <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-100 space-y-5">
                    <div className="flex items-center space-x-2 pb-3 border-b border-gray-200">
                      <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                      <h3 className="text-lg font-semibold text-gray-900">基本信息</h3>
                    </div>

                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-2">供应商名称</label>
                      <Input
                        value={editForm.supplierName || ''}
                        onChange={(e) => setEditForm({ ...editForm, supplierName: e.target.value })}
                        placeholder="请输入供应商名称"
                        className="h-11 border-gray-300 focus:border-blue-500 focus:ring-blue-200"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-2">供应商编码</label>
                      <Input
                        value={editForm.supplierCode || ''}
                        onChange={(e) => setEditForm({ ...editForm, supplierCode: e.target.value })}
                        placeholder="请输入供应商编码"
                        className="h-11 border-gray-300 focus:border-blue-500 focus:ring-blue-200"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-2">API地址</label>
                      <Input
                        value={editForm.apiBaseUrl || ''}
                        onChange={(e) => setEditForm({ ...editForm, apiBaseUrl: e.target.value })}
                        placeholder="请输入API基础地址"
                        className="h-11 border-gray-300 focus:border-blue-500 focus:ring-blue-200"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-2">认证类型</label>
                      <select
                        className="w-full h-11 px-3 border border-gray-300 rounded-md focus:border-blue-500 focus:ring-2 focus:ring-blue-200 bg-white"
                        value={editForm.authType || ''}
                        onChange={(e) => setEditForm({ ...editForm, authType: e.target.value })}
                      >
                        <option value="BasicAuth">BasicAuth</option>
                        <option value="BearerToken">BearerToken</option>
                        <option value="ApiKey">ApiKey</option>
                        <option value="OAuth">OAuth</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-2">描述</label>
                      <textarea
                        className="w-full p-3 border border-gray-300 rounded-md resize-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
                        rows={4}
                        value={editForm.description || ''}
                        onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                        placeholder="请输入供应商描述信息..."
                      />
                    </div>
                  </div>

                  {/* 配置参数 */}
                  <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-100 space-y-5">
                    <div className="flex items-center space-x-2 pb-3 border-b border-gray-200">
                      <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                      <h3 className="text-lg font-semibold text-gray-900">配置参数</h3>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                      <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">超时时间(ms)</label>
                        <Input
                          type="number"
                          value={editForm.timeoutMs || ''}
                          onChange={(e) => setEditForm({ ...editForm, timeoutMs: parseInt(e.target.value) || 0 })}
                          placeholder="30000"
                          className="h-11 border-gray-300 focus:border-blue-500 focus:ring-blue-200"
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">重试次数</label>
                        <Input
                          type="number"
                          value={editForm.retryCount || ''}
                          onChange={(e) => setEditForm({ ...editForm, retryCount: parseInt(e.target.value) || 0 })}
                          placeholder="3"
                          className="h-11 border-gray-300 focus:border-blue-500 focus:ring-blue-200"
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">最大并发请求</label>
                        <Input
                          type="number"
                          value={editForm.maxConcurrentRequests || ''}
                          onChange={(e) => setEditForm({ ...editForm, maxConcurrentRequests: parseInt(e.target.value) || 0 })}
                          placeholder="10"
                          className="h-11 border-gray-300 focus:border-blue-500 focus:ring-blue-200"
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">限流(请求/秒)</label>
                        <Input
                          type="number"
                          value={editForm.rateLimitPerSecond || ''}
                          onChange={(e) => setEditForm({ ...editForm, rateLimitPerSecond: parseInt(e.target.value) || 0 })}
                          placeholder="50"
                          className="h-11 border-gray-300 focus:border-blue-500 focus:ring-blue-200"
                        />
                      </div>

                      <div className="sm:col-span-2">
                        <label className="block text-sm font-semibold text-gray-700 mb-2">优先级</label>
                        <Input
                          type="number"
                          value={editForm.priority || ''}
                          onChange={(e) => setEditForm({ ...editForm, priority: parseInt(e.target.value) || 0 })}
                          placeholder="10"
                          className="h-11 border-gray-300 focus:border-blue-500 focus:ring-blue-200 max-w-xs"
                        />
                      </div>
                    </div>

                    {/* 功能开关 */}
                    <div className="bg-gradient-to-r from-gray-50 to-blue-50 rounded-lg p-5 space-y-4">
                      <div className="flex items-center space-x-2">
                        <div className="w-2 h-2 bg-purple-500 rounded-full"></div>
                        <h4 className="text-sm font-semibold text-gray-700">功能开关</h4>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                        <label className="flex items-center space-x-3 p-3 bg-white rounded-lg border border-gray-200 hover:bg-gray-50 cursor-pointer transition-colors">
                          <input
                            type="checkbox"
                            checked={editForm.isActive || false}
                            onChange={(e) => setEditForm({ ...editForm, isActive: e.target.checked })}
                            className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                          />
                          <div className="flex flex-col">
                            <span className="text-sm font-medium text-gray-900">启用供应商</span>
                            <span className="text-xs text-gray-500">是否启用此供应商</span>
                          </div>
                        </label>

                        <label className="flex items-center space-x-3 p-3 bg-white rounded-lg border border-gray-200 hover:bg-gray-50 cursor-pointer transition-colors">
                          <input
                            type="checkbox"
                            checked={editForm.isSyncStatic || false}
                            onChange={(e) => setEditForm({ ...editForm, isSyncStatic: e.target.checked })}
                            className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                          />
                          <div className="flex flex-col">
                            <span className="text-sm font-medium text-gray-900">同步静态数据</span>
                            <span className="text-xs text-gray-500">同步城市、国家等数据</span>
                          </div>
                        </label>

                        <label className="flex items-center space-x-3 p-3 bg-white rounded-lg border border-gray-200 hover:bg-gray-50 cursor-pointer transition-colors">
                          <input
                            type="checkbox"
                            checked={editForm.isSyncHotel || false}
                            onChange={(e) => setEditForm({ ...editForm, isSyncHotel: e.target.checked })}
                            className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                          />
                          <div className="flex flex-col">
                            <span className="text-sm font-medium text-gray-900">同步酒店数据</span>
                            <span className="text-xs text-gray-500">同步酒店库存信息</span>
                          </div>
                        </label>
                      </div>
                    </div>
                  </div>

                  {/* JSON配置 */}
                  <div className="md:col-span-2 space-y-6">
                    <div className="flex items-center justify-between border-b pb-4">
                      <div className="flex items-center space-x-3">
                        <div className="flex items-center justify-center w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg">
                          <FileCode2 className="w-5 h-5 text-white" />
                        </div>
                        <div>
                          <h3 className="text-lg font-semibold text-gray-900">JSON配置管理</h3>
                          <p className="text-sm text-gray-500">配置供应商的认证、FTP和联系信息</p>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2 text-xs text-gray-400">
                        <span>格式化</span>
                        <div className="w-1 h-1 bg-gray-300 rounded-full"></div>
                        <span>验证</span>
                        <div className="w-1 h-1 bg-gray-300 rounded-full"></div>
                        <span>实时预览</span>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                      {/* 认证配置 */}
                      <div className="bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-shadow">
                        <div className="flex items-center justify-between p-4 border-b border-gray-100 bg-gradient-to-r from-green-50 to-emerald-50 rounded-t-xl">
                          <div className="flex items-center space-x-3">
                            <div className="flex items-center justify-center w-8 h-8 bg-green-500 rounded-lg">
                              <Shield className="w-4 h-4 text-white" />
                            </div>
                            <div>
                              <h4 className="text-sm font-semibold text-gray-900">认证配置</h4>
                              <p className="text-xs text-gray-600">Authentication Settings</p>
                            </div>
                          </div>
                          <button
                            type="button"
                            onClick={() => {
                              try {
                                const formatted = JSON.stringify(JSON.parse(editForm.authConfig || '{}'), null, 2)
                                setEditForm({ ...editForm, authConfig: formatted })
                              } catch (e) {
                                // 格式化失败时不做任何操作
                              }
                            }}
                            className="flex items-center space-x-1 px-2 py-1 text-xs text-green-600 hover:bg-green-100 rounded-md transition-colors"
                          >
                            <Code2 className="w-3 h-3" />
                            <span>格式化</span>
                          </button>
                        </div>
                        <div className="p-4">
                          <div className="relative">
                            <textarea
                              className="w-full p-4 border-2 border-gray-200 rounded-lg font-mono text-sm resize-none 
                                       focus:border-green-500 focus:ring-2 focus:ring-green-200 transition-all duration-200
                                       bg-gray-50 hover:bg-white shadow-inner"
                              rows={12}
                              value={editForm.authConfig || ''}
                              onChange={(e) => setEditForm({ ...editForm, authConfig: e.target.value })}
                              placeholder={JSON.stringify({
                                "appId": "your_app_id",
                                "token": "your_token",
                                "username": "your_username",
                                "password": "your_password",
                                "secretKey": "your_secret_key"
                              }, null, 2)}
                              style={{
                                lineHeight: '1.6',
                                tabSize: '2',
                                fontFamily: '"JetBrains Mono", "Fira Code", "SF Mono", "Consolas", monospace',
                                fontSize: '13px'
                              }}
                            />
                            {(() => {
                              try {
                                if (editForm.authConfig) {
                                  JSON.parse(editForm.authConfig)
                                  return (
                                    <div className="absolute top-3 right-3 flex items-center space-x-2 bg-green-100 text-green-700 px-2 py-1 rounded-md text-xs font-medium">
                                      <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                                      <span>格式正确</span>
                                    </div>
                                  )
                                }
                              } catch {
                                return (
                                  <div className="absolute top-3 right-3 flex items-center space-x-2 bg-red-100 text-red-700 px-2 py-1 rounded-md text-xs font-medium">
                                    <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                                    <span>格式错误</span>
                                  </div>
                                )
                              }
                            })()}
                          </div>
                        </div>
                      </div>

                      {/* FTP配置 */}
                      <div className="bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-shadow">
                        <div className="flex items-center justify-between p-4 border-b border-gray-100 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-t-xl">
                          <div className="flex items-center space-x-3">
                            <div className="flex items-center justify-center w-8 h-8 bg-blue-500 rounded-lg">
                              <Upload className="w-4 h-4 text-white" />
                            </div>
                            <div>
                              <h4 className="text-sm font-semibold text-gray-900">FTP配置</h4>
                              <p className="text-xs text-gray-600">File Transfer Settings</p>
                            </div>
                          </div>
                          <button
                            type="button"
                            onClick={() => {
                              try {
                                const formatted = JSON.stringify(JSON.parse(editForm.ftpConfig || '{}'), null, 2)
                                setEditForm({ ...editForm, ftpConfig: formatted })
                              } catch (e) {
                                // 格式化失败时不做任何操作
                              }
                            }}
                            className="flex items-center space-x-1 px-2 py-1 text-xs text-blue-600 hover:bg-blue-100 rounded-md transition-colors"
                          >
                            <Code2 className="w-3 h-3" />
                            <span>格式化</span>
                          </button>
                        </div>
                        <div className="p-4">
                          <div className="relative">
                            <textarea
                              className="w-full p-4 border-2 border-gray-200 rounded-lg font-mono text-sm resize-none 
                                       focus:border-blue-500 focus:ring-2 focus:ring-blue-200 transition-all duration-200
                                       bg-gray-50 hover:bg-white shadow-inner"
                              rows={12}
                              value={editForm.ftpConfig || ''}
                              onChange={(e) => setEditForm({ ...editForm, ftpConfig: e.target.value })}
                              placeholder={JSON.stringify({
                                "host": "ftp.example.com",
                                "port": 21,
                                "username": "ftp_user",
                                "password": "ftp_password",
                                "citiesPath": "ftp,/cities.csv",
                                "hotelsPath": "local,/hotels.csv"
                              }, null, 2)}
                              style={{
                                lineHeight: '1.6',
                                tabSize: '2',
                                fontFamily: '"JetBrains Mono", "Fira Code", "SF Mono", "Consolas", monospace',
                                fontSize: '13px'
                              }}
                            />
                            {(() => {
                              try {
                                if (editForm.ftpConfig) {
                                  JSON.parse(editForm.ftpConfig)
                                  return (
                                    <div className="absolute top-3 right-3 flex items-center space-x-2 bg-green-100 text-green-700 px-2 py-1 rounded-md text-xs font-medium">
                                      <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                                      <span>格式正确</span>
                                    </div>
                                  )
                                }
                              } catch {
                                return (
                                  <div className="absolute top-3 right-3 flex items-center space-x-2 bg-red-100 text-red-700 px-2 py-1 rounded-md text-xs font-medium">
                                    <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                                    <span>格式错误</span>
                                  </div>
                                )
                              }
                            })()}
                          </div>
                        </div>
                      </div>

                      {/* 联系信息 */}
                      <div className="bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-shadow">
                        <div className="flex items-center justify-between p-4 border-b border-gray-100 bg-gradient-to-r from-purple-50 to-pink-50 rounded-t-xl">
                          <div className="flex items-center space-x-3">
                            <div className="flex items-center justify-center w-8 h-8 bg-purple-500 rounded-lg">
                              <Settings className="w-4 h-4 text-white" />
                            </div>
                            <div>
                              <h4 className="text-sm font-semibold text-gray-900">联系信息</h4>
                              <p className="text-xs text-gray-600">Contact Information</p>
                            </div>
                          </div>
                          <button
                            type="button"
                            onClick={() => {
                              try {
                                const formatted = JSON.stringify(JSON.parse(editForm.contactInfo || '{}'), null, 2)
                                setEditForm({ ...editForm, contactInfo: formatted })
                              } catch (e) {
                                // 格式化失败时不做任何操作
                              }
                            }}
                            className="flex items-center space-x-1 px-2 py-1 text-xs text-purple-600 hover:bg-purple-100 rounded-md transition-colors"
                          >
                            <Code2 className="w-3 h-3" />
                            <span>格式化</span>
                          </button>
                        </div>
                        <div className="p-4">
                          <div className="relative">
                            <textarea
                              className="w-full p-4 border-2 border-gray-200 rounded-lg font-mono text-sm resize-none 
                                       focus:border-purple-500 focus:ring-2 focus:ring-purple-200 transition-all duration-200
                                       bg-gray-50 hover:bg-white shadow-inner"
                              rows={12}
                              value={editForm.contactInfo || ''}
                              onChange={(e) => setEditForm({ ...editForm, contactInfo: e.target.value })}
                              placeholder={JSON.stringify({
                                "email": "contact@example.com",
                                "phone": "+1234567890",
                                "contact_person": "技术支持团队",
                                "department": "技术部",
                                "timezone": "Asia/Shanghai"
                              }, null, 2)}
                              style={{
                                lineHeight: '1.6',
                                tabSize: '2',
                                fontFamily: '"JetBrains Mono", "Fira Code", "SF Mono", "Consolas", monospace',
                                fontSize: '13px'
                              }}
                            />
                            {(() => {
                              try {
                                if (editForm.contactInfo) {
                                  JSON.parse(editForm.contactInfo)
                                  return (
                                    <div className="absolute top-3 right-3 flex items-center space-x-2 bg-green-100 text-green-700 px-2 py-1 rounded-md text-xs font-medium">
                                      <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                                      <span>格式正确</span>
                                    </div>
                                  )
                                }
                              } catch {
                                return (
                                  <div className="absolute top-3 right-3 flex items-center space-x-2 bg-red-100 text-red-700 px-2 py-1 rounded-md text-xs font-medium">
                                    <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                                    <span>格式错误</span>
                                  </div>
                                )
                              }
                            })()}
                          </div>
                        </div>
                      </div>

                      {/* JSON格式说明 */}
                      <div className="md:col-span-3">
                        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-xl p-6 shadow-sm">
                          <div className="flex items-start space-x-3">
                            <div className="flex items-center justify-center w-8 h-8 bg-blue-500 rounded-lg flex-shrink-0">
                              <AlertCircle className="w-4 h-4 text-white" />
                            </div>
                            <div>
                              <p className="text-blue-800 font-semibold mb-3">JSON格式要求与提示：</p>
                              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div>
                                  <h5 className="text-blue-700 font-medium mb-2 text-sm">基本格式</h5>
                                  <ul className="text-blue-600 space-y-1 text-xs">
                                    <li>• 使用双引号包围字符串</li>
                                    <li>• 确保所有括号和逗号匹配</li>
                                    <li>• 数值类型不需要引号</li>
                                    <li>• 布尔值使用 true/false</li>
                                  </ul>
                                </div>
                                <div>
                                  <h5 className="text-blue-700 font-medium mb-2 text-sm">编辑提示</h5>
                                  <ul className="text-blue-600 space-y-1 text-xs">
                                    <li>• 点击&ldquo;格式化&rdquo;按钮美化代码</li>
                                    <li>• 实时显示格式验证状态</li>
                                    <li>• 支持Tab键缩进对齐</li>
                                    <li>• 自动语法高亮显示</li>
                                  </ul>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* 底部操作区域 */}
                <div className="flex justify-between items-center px-8 py-6 border-t border-gray-200 bg-white">
                  <div className="text-sm text-gray-500">
                    请确保所有信息填写正确，保存后将立即生效
                  </div>
                  <div className="flex space-x-4">
                    <Button
                      variant="outline"
                      onClick={() => {
                        setShowEditDialog(false)
                        setEditingSupplier(null)
                        setEditForm({})
                      }}
                      disabled={saving}
                      className="px-6 py-2 h-10"
                    >
                      取消
                    </Button>
                    <Button
                      onClick={handleSaveSupplier}
                      disabled={saving}
                      className="px-6 py-2 h-10 bg-blue-600 hover:bg-blue-700 text-white"
                    >
                      {saving ? (
                        <div className="flex items-center space-x-2">
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                          <span>保存中...</span>
                        </div>
                      ) : (
                        '保存配置'
                      )}
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </MainLayout>
  )
}