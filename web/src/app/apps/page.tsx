'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'
import { Plus, Search, Edit, Trash2, Key, Clock, Settings, Eye, EyeOff } from 'lucide-react'

interface AppConfig {
  appId: string
  secretKey: string
  encryptionKey: string
  rateLimit: number
  timeout: number
  createAt: string
  updateAt: string
  createBy: string
  updateBy: string
}

interface CreateAppRequest {
  appId: string
  secretKey: string
  encryptionKey: string
  rateLimit: number
  timeout: number
}

export default function AppsPage() {
  const [apps, setApps] = useState<AppConfig[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [editingApp, setEditingApp] = useState<AppConfig | null>(null)
  const [showSecrets, setShowSecrets] = useState<{ [key: string]: boolean }>({})
  const [formData, setFormData] = useState<CreateAppRequest>({
    appId: '',
    secretKey: '',
    encryptionKey: '',
    rateLimit: 1000,
    timeout: 720
  })

  useEffect(() => {
    fetchApps()
  }, [])

  const fetchApps = async () => {
    try {
      setLoading(true)
      // 模拟API调用，实际使用时替换为真实的API
      setTimeout(() => {
        const mockApps: AppConfig[] = [
          {
            appId: 'heytrip_supplier_integration_pax',
            secretKey: 'HeyTrip@Pax#SupplierIntegration!2025',
            encryptionKey: '427ae41e4649b934ca495991b7852b855',
            rateLimit: 1000,
            timeout: 720,
            createAt: '2025-01-01 00:00:00',
            updateAt: '2025-01-01 00:00:00',
            createBy: 'system',
            updateBy: 'system'
          },
          {
            appId: 'test_app_001',
            secretKey: 'TestSecret123!@#',
            encryptionKey: 'abcd1234efgh5678ijkl9012mnop3456qrst',
            rateLimit: 500,
            timeout: 24,
            createAt: '2025-01-02 10:30:00',
            updateAt: '2025-01-05 14:20:00',
            createBy: 'admin',
            updateBy: 'admin'
          }
        ]
        setApps(mockApps)
        setLoading(false)
      }, 1000)
    } catch (error) {
      console.error('获取应用列表失败:', error)
      setLoading(false)
    }
  }

  const handleCreateApp = async () => {
    try {
      // 实际调用API创建应用
      const response = await apiClient.post('/admin/app/create', formData)
      console.log('应用创建成功:', response.data)
      
      // 重新获取应用列表
      await fetchApps()
      
      // 重置表单
      setFormData({
        appId: '',
        secretKey: '',
        encryptionKey: '',
        rateLimit: 1000,
        timeout: 720
      })
      setShowCreateForm(false)
    } catch (error) {
      console.error('创建应用失败:', error)
      alert('创建应用失败，请检查输入信息')
    }
  }

  const handleUpdateApp = async () => {
    if (!editingApp) return

    try {
      const response = await apiClient.put('/admin/app/update', {
        ...editingApp,
        ...formData
      })
      console.log('应用更新成功:', response.data)
      
      await fetchApps()
      setEditingApp(null)
      setShowCreateForm(false)
    } catch (error) {
      console.error('更新应用失败:', error)
      alert('更新应用失败，请检查输入信息')
    }
  }

  const handleDeleteApp = async (appId: string) => {
    if (!confirm(`确定要删除应用 ${appId} 吗？此操作不可撤销。`)) {
      return
    }

    try {
      await apiClient.delete(`/admin/app/delete/${appId}`)
      console.log('应用删除成功')
      await fetchApps()
    } catch (error) {
      console.error('删除应用失败:', error)
      alert('删除应用失败')
    }
  }

  const toggleSecretVisibility = (appId: string) => {
    setShowSecrets(prev => ({
      ...prev,
      [appId]: !prev[appId]
    }))
  }

  const generateRandomKey = (length: number = 32) => {
    const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
    let result = ''
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    return result
  }

  const filteredApps = apps.filter(app =>
    app.appId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    app.createBy.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const formatTimeout = (timeout: number) => {
    if (timeout === -1) return '永不过期'
    if (timeout < 24) return `${timeout} 小时`
    return `${Math.floor(timeout / 24)} 天`
  }

  const maskSecret = (secret: string) => {
    if (secret.length <= 8) return '****'
    return secret.substring(0, 4) + '****' + secret.substring(secret.length - 4)
  }

  if (loading) {
    return (
      <MainLayout>
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="text-lg">加载应用数据中...</div>
        </div>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <h1 className="text-3xl font-bold">应用管理</h1>
          <Button onClick={() => {
            setShowCreateForm(true)
            setEditingApp(null)
            setFormData({
              appId: '',
              secretKey: '',
              encryptionKey: '',
              rateLimit: 1000,
              timeout: 720
            })
          }}>
            <Plus className="w-4 h-4 mr-2" />
            创建应用
          </Button>
        </div>

        {/* 搜索栏 */}
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center space-x-4">
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                <Input
                  placeholder="搜索应用ID或创建者..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
              <Button variant="outline" onClick={fetchApps}>
                刷新
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* 创建/编辑表单 */}
        {showCreateForm && (
          <Card>
            <CardHeader>
              <CardTitle>{editingApp ? '编辑应用' : '创建新应用'}</CardTitle>
              <CardDescription>
                {editingApp ? '修改应用配置信息' : '填写新应用的配置信息'}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">应用ID *</label>
                  <Input
                    value={formData.appId}
                    onChange={(e) => setFormData({ ...formData, appId: e.target.value })}
                    placeholder="请输入应用ID"
                    disabled={!!editingApp}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">限流阈值 (请求/分钟) *</label>
                  <Input
                    type="number"
                    value={formData.rateLimit}
                    onChange={(e) => setFormData({ ...formData, rateLimit: parseInt(e.target.value) || 0 })}
                    placeholder="1000"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">密钥 *</label>
                  <div className="flex space-x-2">
                    <Input
                      value={formData.secretKey}
                      onChange={(e) => setFormData({ ...formData, secretKey: e.target.value })}
                      placeholder="请输入密钥"
                    />
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setFormData({ ...formData, secretKey: generateRandomKey(24) })}
                    >
                      生成
                    </Button>
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">超时时间 (小时，-1为永不过期) *</label>
                  <Input
                    type="number"
                    value={formData.timeout}
                    onChange={(e) => setFormData({ ...formData, timeout: parseInt(e.target.value) || 0 })}
                    placeholder="720"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">加密密钥 *</label>
                <div className="flex space-x-2">
                  <Input
                    value={formData.encryptionKey}
                    onChange={(e) => setFormData({ ...formData, encryptionKey: e.target.value })}
                    placeholder="请输入加密密钥（32位）"
                  />
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setFormData({ ...formData, encryptionKey: generateRandomKey(32) })}
                  >
                    生成
                  </Button>
                </div>
              </div>

              <div className="flex space-x-4">
                <Button onClick={editingApp ? handleUpdateApp : handleCreateApp}>
                  {editingApp ? '更新应用' : '创建应用'}
                </Button>
                <Button variant="outline" onClick={() => setShowCreateForm(false)}>
                  取消
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* 应用列表 */}
        <div className="grid gap-4">
          {filteredApps.map((app) => (
            <Card key={app.appId}>
              <CardHeader>
                <div className="flex justify-between items-start">
                  <div>
                    <CardTitle className="flex items-center gap-2">
                      <Key className="w-5 h-5" />
                      {app.appId}
                      <Badge variant={app.timeout === -1 ? 'default' : 'secondary'}>
                        {formatTimeout(app.timeout)}
                      </Badge>
                    </CardTitle>
                    <CardDescription>
                      创建者: {app.createBy} | 创建时间: {app.createAt} | 更新时间: {app.updateAt}
                    </CardDescription>
                  </div>
                  <div className="flex space-x-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setEditingApp(app)
                        setFormData({
                          appId: app.appId,
                          secretKey: app.secretKey,
                          encryptionKey: app.encryptionKey,
                          rateLimit: app.rateLimit,
                          timeout: app.timeout
                        })
                        setShowCreateForm(true)
                      }}
                    >
                      <Edit className="w-4 h-4" />
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleDeleteApp(app.appId)}
                      className="text-red-600 hover:text-red-800"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  <div>
                    <label className="text-sm font-medium text-gray-500">密钥</label>
                    <div className="flex items-center space-x-2">
                      <code className="text-sm bg-gray-100 px-2 py-1 rounded">
                        {showSecrets[app.appId] ? app.secretKey : maskSecret(app.secretKey)}
                      </code>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => toggleSecretVisibility(app.appId)}
                      >
                        {showSecrets[app.appId] ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </Button>
                    </div>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">加密密钥</label>
                    <div className="flex items-center space-x-2">
                      <code className="text-sm bg-gray-100 px-2 py-1 rounded">
                        {showSecrets[app.appId] ? app.encryptionKey : maskSecret(app.encryptionKey)}
                      </code>
                    </div>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">限流阈值</label>
                    <div className="flex items-center space-x-2">
                      <Badge variant="outline">{app.rateLimit} 请求/分钟</Badge>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {filteredApps.length === 0 && (
          <Card>
            <CardContent className="py-8 text-center">
              <div className="text-gray-500">
                {searchTerm ? '没有找到匹配的应用' : '暂无应用数据'}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </MainLayout>
  )
}