'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'
import type { App, AppListResponse, AppUpdateRequest, AppInfoResponse } from '@/types'
import { Search, Edit, Trash2, Key, Settings, Eye, EyeOff, RefreshCw, Info, Shield } from 'lucide-react'

export default function AppsPage() {
  const [apps, setApps] = useState<App[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [showEditForm, setShowEditForm] = useState(false)
  const [showDetailDialog, setShowDetailDialog] = useState(false)
  const [editingApp, setEditingApp] = useState<App | null>(null)
  const [viewingApp, setViewingApp] = useState<App | null>(null)
  const [showSecrets, setShowSecrets] = useState<{ [key: string]: boolean }>({})
  const [editForm, setEditForm] = useState<AppUpdateRequest>({ appId: '' })
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    fetchApps()
  }, [])

  const fetchApps = async () => {
    try {
      setLoading(true)
      const response = await apiClient.get<AppListResponse>('/admin/app/list')

      if (response.data && response.data.code === 200) {
        setApps(response.data.data)
      } else {
        throw new Error(response.data?.msg || '获取应用列表失败')
      }
    } catch (error) {
      console.error('获取应用列表失败:', error)
      alert('获取应用列表失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  // 处理编辑应用
  const handleEditApp = (app: App) => {
    setEditingApp(app)
    setEditForm({
      appId: app.appId,
      secretKey: app.secretKey,
      encryptionKey: app.encryptionKey,
      rateLimit: app.rateLimit,
      timeout: app.timeout
    })
    setShowEditForm(true)
  }

  // 处理取消编辑
  const handleCancelEdit = () => {
    setShowEditForm(false)
    setEditingApp(null)
    setEditForm({ appId: '' })
  }

  // 处理查看应用详情
  const handleViewApp = async (appId: string) => {
    try {
      setLoading(true)
      const response = await apiClient.get<AppInfoResponse>(`/app/info/${appId}`)

      if (response.data && response.data.code === 200) {
        setViewingApp(response.data.data)
        setShowDetailDialog(true)
      } else {
        throw new Error(response.data?.msg || '获取应用详情失败')
      }
    } catch (error) {
      console.error('获取应用详情失败:', error)
      alert('获取应用详情失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  // 处理删除应用
  const handleDeleteApp = async (appId: string) => {
    if (!confirm('确定要删除这个应用吗？此操作不可恢复。')) {
      return
    }

    try {
      setSaving(true)
      const response = await apiClient.delete(`/app/delete/${appId}`)

      if (response.data && response.data.code === 200) {
        alert('删除应用成功')
        await fetchApps()
      } else {
        throw new Error(response.data?.msg || '删除应用失败')
      }
    } catch (error) {
      console.error('删除应用失败:', error)
      alert('删除应用失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  // 处理更新应用
  const handleUpdateApp = async () => {
    try {
      setSaving(true)
      const response = await apiClient.put<AppInfoResponse>('/admin/app/update', editForm)

      if (response.data && response.data.code === 200) {
        alert('更新应用成功')
        setShowEditForm(false)
        setEditingApp(null)
        await fetchApps()
      } else {
        throw new Error(response.data?.msg || '更新应用失败')
      }
    } catch (error) {
      console.error('更新应用失败:', error)
      alert('更新应用失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  // 切换密钥可见性
  const toggleSecretVisibility = (appId: string) => {
    setShowSecrets(prev => ({
      ...prev,
      [appId]: !prev[appId]
    }))
  }

  // 生成随机密钥
  const generateRandomKey = () => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    let result = ''
    for (let i = 0; i < 32; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    return result
  }

  // 脱敏显示密钥
  const maskSecret = (secret: string) => {
    if (!secret) return ''
    return secret.substring(0, 8) + '****' + secret.substring(secret.length - 4)
  }

  // 格式化超时时间
  const formatTimeout = (timeout: number) => {
    if (timeout === -1) return '永久有效'
    return `${timeout}小时`
  }

  // 统计数据
  const stats = {
    total: apps.length,
    active: apps.filter(app => !app.expired).length,
    expired: apps.filter(app => app.expired).length
  }

  // 过滤应用列表
  const filteredApps = apps.filter(app =>
    app.appId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    app.createBy.toLowerCase().includes(searchTerm.toLowerCase())
  )

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* 页面标题 */}
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">应用管理</h1>
            <p className="text-gray-500 mt-1">管理系统中的所有应用及其配置信息</p>
          </div>
          <div className="flex space-x-2">
            <Button onClick={fetchApps} disabled={loading} variant="outline">
              <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
              刷新
            </Button>
          </div>
        </div>

        {/* 统计卡片 */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">总应用数</CardTitle>
              <Key className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stats.total}</div>
              <p className="text-xs text-muted-foreground">系统中注册的应用总数</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">正常应用</CardTitle>
              <Shield className="h-4 w-4 text-green-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-600">{stats.active}</div>
              <p className="text-xs text-muted-foreground">当前可用的应用数量</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">已过期应用</CardTitle>
              <Settings className="h-4 w-4 text-red-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-red-600">{stats.expired}</div>
              <p className="text-xs text-muted-foreground">需要处理的过期应用</p>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和筛选 */}
        <Card>
          <CardHeader>
            <CardTitle>搜索应用</CardTitle>
            <CardDescription>根据应用ID或创建者搜索应用</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex space-x-2">
              <div className="relative flex-1">
                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="搜索应用ID或创建者..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 编辑应用表单 */}
        {showEditForm && editingApp && (
          <Card className="border-blue-200 bg-blue-50/50">
            <CardHeader>
              <div className="flex justify-between items-center">
                <div>
                  <CardTitle className="text-blue-900">编辑应用</CardTitle>
                  <CardDescription className="text-blue-700">
                    修改应用 {editingApp.appId} 的配置信息
                  </CardDescription>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleCancelEdit}
                  disabled={saving}
                  className="text-gray-500 hover:text-gray-700"
                >
                  ✕
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    应用ID
                  </label>
                  <Input
                    value={editForm.appId}
                    disabled
                    className="bg-gray-100 text-gray-600"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    状态
                  </label>
                  <div className="pt-2">
                    <Badge variant={editingApp.expired ? 'destructive' : 'default'}>
                      {editingApp.expired ? '已过期' : '正常'}
                    </Badge>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    密钥
                  </label>
                  <div className="flex space-x-2">
                    <Input
                      value={editForm.secretKey || ''}
                      onChange={(e) => setEditForm(prev => ({ ...prev, secretKey: e.target.value }))}
                      placeholder="输入密钥"
                      className="font-mono"
                    />
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setEditForm(prev => ({ ...prev, secretKey: generateRandomKey() }))}
                      disabled={saving}
                    >
                      生成
                    </Button>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    加密密钥
                  </label>
                  <div className="flex space-x-2">
                    <Input
                      value={editForm.encryptionKey || ''}
                      onChange={(e) => setEditForm(prev => ({ ...prev, encryptionKey: e.target.value }))}
                      placeholder="输入加密密钥"
                      className="font-mono"
                    />
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setEditForm(prev => ({ ...prev, encryptionKey: generateRandomKey() }))}
                      disabled={saving}
                    >
                      生成
                    </Button>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    速率限制 (请求/分钟)
                  </label>
                  <Input
                    type="number"
                    value={editForm.rateLimit || ''}
                    onChange={(e) => setEditForm(prev => ({ ...prev, rateLimit: parseInt(e.target.value) || 0 }))}
                    placeholder="例如：100"
                    disabled={saving}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    超时时间 (小时，-1表示无限制)
                  </label>
                  <Input
                    type="number"
                    value={editForm.timeout !== undefined ? editForm.timeout : ''}
                    onChange={(e) => {
                      const value = e.target.value
                      if (value === '' || value === '-') {
                        setEditForm(prev => ({ ...prev, timeout: undefined }))
                      } else {
                        const numValue = parseInt(value)
                        setEditForm(prev => ({ ...prev, timeout: isNaN(numValue) ? 0 : numValue }))
                      }
                    }}
                    placeholder="例如：24 或 -1"
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="flex justify-end space-x-2 pt-4 border-t border-blue-200">
                <Button
                  variant="outline"
                  onClick={handleCancelEdit}
                  disabled={saving}
                >
                  取消
                </Button>
                <Button
                  onClick={handleUpdateApp}
                  disabled={saving}
                  className="bg-blue-600 hover:bg-blue-700"
                >
                  {saving ? '保存中...' : '保存更改'}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* 应用列表 */}
        <Card>
          <CardHeader>
            <CardTitle>应用列表</CardTitle>
            <CardDescription>
              系统中所有应用的详细信息和操作管理
            </CardDescription>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="text-center py-8">
                <div className="text-gray-500">加载中...</div>
              </div>
            ) : filteredApps.length === 0 ? (
              <div className="text-center py-8">
                <div className="text-gray-500">
                  {searchTerm ? '没有找到匹配的应用' : '暂无应用数据'}
                </div>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b bg-gray-50">
                      <th className="text-left py-3 px-4 font-medium text-gray-900">应用ID</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">密钥</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">加密密钥</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">速率限制</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">超时时间</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">状态</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">创建时间</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredApps.map((app) => (
                      <tr
                        key={app.appId}
                        className={`border-b hover:bg-gray-50 ${editingApp?.appId === app.appId ? 'bg-blue-50 border-blue-200' : ''
                          }`}
                      >
                        <td className="py-3 px-4 font-mono text-sm">{app.appId}</td>
                        <td className="py-3 px-4">
                          <div className="flex items-center space-x-2">
                            <span className="font-mono text-sm">
                              {showSecrets[app.appId] ? app.secretKey : maskSecret(app.secretKey)}
                            </span>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => toggleSecretVisibility(app.appId)}
                            >
                              {showSecrets[app.appId] ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                            </Button>
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex items-center space-x-2">
                            <span className="font-mono text-sm">
                              {showSecrets[app.appId] ? app.encryptionKey : maskSecret(app.encryptionKey)}
                            </span>
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <Badge variant="secondary">
                            {app.rateLimit}/分钟
                          </Badge>
                        </td>
                        <td className="py-3 px-4">
                          <Badge variant={app.timeout === -1 ? 'default' : 'secondary'}>
                            {formatTimeout(app.timeout)}
                          </Badge>
                        </td>
                        <td className="py-3 px-4">
                          <Badge variant={app.expired ? 'destructive' : 'default'}>
                            {app.expired ? '已过期' : '正常'}
                          </Badge>
                        </td>
                        <td className="py-3 px-4 text-sm text-gray-500">
                          {new Date(app.createAt).toLocaleDateString()}
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex space-x-1">

                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleEditApp(app)}
                              disabled={saving}
                              title="编辑应用"
                            >
                              <Edit className="w-4 h-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDeleteApp(app.appId)}
                              disabled={saving}
                              title="删除应用"
                              className="text-red-600 hover:text-red-800"
                            >
                              <Trash2 className="w-4 h-4" />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>



        {/* 应用详情对话框 */}
        {showDetailDialog && viewingApp && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <Card className="w-full max-w-2xl mx-4 max-h-[80vh] overflow-y-auto">
              <CardHeader>
                <CardTitle>应用详情</CardTitle>
                <CardDescription>应用 {viewingApp.appId} 的完整信息</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      应用ID
                    </label>
                    <div className="p-2 bg-gray-50 rounded font-mono text-sm">
                      {viewingApp.appId}
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      状态
                    </label>
                    <div className="p-2">
                      <Badge variant={viewingApp.expired ? 'destructive' : 'default'}>
                        {viewingApp.expired ? '已过期' : '正常'}
                      </Badge>
                    </div>
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      密钥
                    </label>
                    <div className="flex items-center space-x-2">
                      <div className="p-2 bg-gray-50 rounded font-mono text-sm flex-1">
                        {showSecrets[viewingApp.appId] ? viewingApp.secretKey : maskSecret(viewingApp.secretKey)}
                      </div>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => toggleSecretVisibility(viewingApp.appId)}
                      >
                        {showSecrets[viewingApp.appId] ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </Button>
                    </div>
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      加密密钥
                    </label>
                    <div className="p-2 bg-gray-50 rounded font-mono text-sm">
                      {showSecrets[viewingApp.appId] ? viewingApp.encryptionKey : maskSecret(viewingApp.encryptionKey)}
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      速率限制
                    </label>
                    <div className="p-2">
                      <Badge variant="secondary">{viewingApp.rateLimit}/分钟</Badge>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      超时时间
                    </label>
                    <div className="p-2">
                      <Badge variant={viewingApp.timeout === -1 ? 'default' : 'secondary'}>
                        {formatTimeout(viewingApp.timeout)}
                      </Badge>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      创建者
                    </label>
                    <div className="p-2 bg-gray-50 rounded">
                      {viewingApp.createBy}
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      创建时间
                    </label>
                    <div className="p-2 bg-gray-50 rounded">
                      {new Date(viewingApp.createAt).toLocaleString()}
                    </div>
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      最后更新时间
                    </label>
                    <div className="p-2 bg-gray-50 rounded">
                      {new Date(viewingApp.updateAt).toLocaleString()}
                    </div>
                  </div>
                </div>
              </CardContent>
              <div className="flex justify-end p-6 pt-0">
                <Button
                  variant="outline"
                  onClick={() => {
                    setShowDetailDialog(false)
                    setViewingApp(null)
                  }}
                >
                  关闭
                </Button>
              </div>
            </Card>
          </div>
        )}
      </div>
    </MainLayout>
  )
}
