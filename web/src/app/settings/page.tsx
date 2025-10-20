'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'
import { Settings, Save, RefreshCw, Database, Shield, Clock, Zap } from 'lucide-react'

interface SystemConfig {
  category: string
  key: string
  value: string
  description: string
  type: 'string' | 'number' | 'boolean' | 'json'
  editable: boolean
}

interface ConfigCategory {
  name: string
  icon: React.ReactNode
  configs: SystemConfig[]
}

export default function SettingsPage() {
  const [categories, setCategories] = useState<ConfigCategory[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [editedConfigs, setEditedConfigs] = useState<{ [key: string]: string }>({})

  useEffect(() => {
    fetchSystemConfigs()
  }, [])

  const fetchSystemConfigs = async () => {
    try {
      setLoading(true)
      // 模拟API调用，实际使用时替换为真实的API
      setTimeout(() => {
        const mockConfigs: ConfigCategory[] = [
          {
            name: '数据库配置',
            icon: <Database className="w-5 h-5" />,
            configs: [
              {
                category: 'database',
                key: 'connection.pool.size',
                value: '20',
                description: '数据库连接池大小',
                type: 'number',
                editable: true
              },
              {
                category: 'database',
                key: 'connection.timeout',
                value: '30000',
                description: '数据库连接超时时间（毫秒）',
                type: 'number',
                editable: true
              },
              {
                category: 'database',
                key: 'query.timeout',
                value: '60000',
                description: '查询超时时间（毫秒）',
                type: 'number',
                editable: true
              }
            ]
          },
          {
            name: '安全配置',
            icon: <Shield className="w-5 h-5" />,
            configs: [
              {
                category: 'security',
                key: 'auth.token.expiry',
                value: '7200',
                description: '认证token过期时间（秒）',
                type: 'number',
                editable: true
              },
              {
                category: 'security',
                key: 'auth.max.attempts',
                value: '5',
                description: '最大登录尝试次数',
                type: 'number',
                editable: true
              },
              {
                category: 'security',
                key: 'auth.lockout.duration',
                value: '1800',
                description: '账户锁定时间（秒）',
                type: 'number',
                editable: true
              },
              {
                category: 'security',
                key: 'signature.required',
                value: 'true',
                description: '是否启用API签名验证',
                type: 'boolean',
                editable: true
              }
            ]
          },
          {
            name: '缓存配置',
            icon: <Zap className="w-5 h-5" />,
            configs: [
              {
                category: 'cache',
                key: 'caffeine.maximum.size',
                value: '10000',
                description: 'Caffeine缓存最大条目数',
                type: 'number',
                editable: true
              },
              {
                category: 'cache',
                key: 'caffeine.expire.after.write',
                value: '3600',
                description: '写入后过期时间（秒）',
                type: 'number',
                editable: true
              },
              {
                category: 'cache',
                key: 'caffeine.expire.after.access',
                value: '1800',
                description: '访问后过期时间（秒）',
                type: 'number',
                editable: true
              },
              {
                category: 'cache',
                key: 'cache.static.data.enabled',
                value: 'true',
                description: '是否启用静态数据缓存',
                type: 'boolean',
                editable: true
              }
            ]
          },
          {
            name: '供应商配置',
            icon: <Clock className="w-5 h-5" />,
            configs: [
              {
                category: 'supplier',
                key: 'http.client.timeout',
                value: '30000',
                description: 'HTTP客户端超时时间（毫秒）',
                type: 'number',
                editable: true
              },
              {
                category: 'supplier',
                key: 'http.client.retry.max',
                value: '3',
                description: 'HTTP请求最大重试次数',
                type: 'number',
                editable: true
              },
              {
                category: 'supplier',
                key: 'supplier.health.check.interval',
                value: '300000',
                description: '供应商健康检查间隔（毫秒）',
                type: 'number',
                editable: true
              },
              {
                category: 'supplier',
                key: 'rate.limit.enabled',
                value: 'true',
                description: '是否启用供应商限流',
                type: 'boolean',
                editable: true
              }
            ]
          }
        ]
        setCategories(mockConfigs)
        setLoading(false)
      }, 1000)
    } catch (error) {
      console.error('获取系统配置失败:', error)
      setLoading(false)
    }
  }

  const handleConfigChange = (category: string, key: string, value: string) => {
    const configKey = `${category}.${key}`
    setEditedConfigs(prev => ({
      ...prev,
      [configKey]: value
    }))
  }

  const getDisplayValue = (category: string, key: string, originalValue: string) => {
    const configKey = `${category}.${key}`
    return editedConfigs[configKey] !== undefined ? editedConfigs[configKey] : originalValue
  }

  const hasChanges = () => {
    return Object.keys(editedConfigs).length > 0
  }

  const handleSaveAll = async () => {
    if (!hasChanges()) {
      alert('没有需要保存的更改')
      return
    }

    try {
      setSaving(true)
      
      // 实际调用API保存配置
      for (const [configKey, value] of Object.entries(editedConfigs)) {
        const [category, key] = configKey.split('.')
        await apiClient.put('/system/config/update', {
          category,
          key,
          value
        })
      }
      
      alert('配置保存成功')
      setEditedConfigs({})
      await fetchSystemConfigs()
    } catch (error) {
      console.error('保存配置失败:', error)
      alert('保存配置失败，请重试')
    } finally {
      setSaving(false)
    }
  }

  const handleResetAll = () => {
    if (!hasChanges()) {
      return
    }
    
    if (confirm('确定要重置所有未保存的更改吗？')) {
      setEditedConfigs({})
    }
  }

  const renderConfigValue = (config: SystemConfig) => {
    const currentValue = getDisplayValue(config.category, config.key, config.value)
    const configKey = `${config.category}.${config.key}`
    const hasEdit = editedConfigs[configKey] !== undefined

    if (!config.editable) {
      return (
        <div className="flex items-center space-x-2">
          <code className="text-sm bg-gray-100 px-2 py-1 rounded">{config.value}</code>
          <Badge variant="secondary">只读</Badge>
        </div>
      )
    }

    switch (config.type) {
      case 'boolean':
        return (
          <div className="flex items-center space-x-2">
            <select
              value={currentValue}
              onChange={(e) => handleConfigChange(config.category, config.key, e.target.value)}
              className="border rounded px-2 py-1 text-sm"
            >
              <option value="true">启用</option>
              <option value="false">禁用</option>
            </select>
            {hasEdit && <Badge variant="outline">已修改</Badge>}
          </div>
        )
      case 'number':
        return (
          <div className="flex items-center space-x-2">
            <Input
              type="number"
              value={currentValue}
              onChange={(e) => handleConfigChange(config.category, config.key, e.target.value)}
              className="w-32 text-sm"
            />
            {hasEdit && <Badge variant="outline">已修改</Badge>}
          </div>
        )
      default:
        return (
          <div className="flex items-center space-x-2">
            <Input
              value={currentValue}
              onChange={(e) => handleConfigChange(config.category, config.key, e.target.value)}
              className="w-48 text-sm"
            />
            {hasEdit && <Badge variant="outline">已修改</Badge>}
          </div>
        )
    }
  }

  if (loading) {
    return (
      <MainLayout>
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="text-lg">加载系统配置中...</div>
        </div>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <h1 className="text-3xl font-bold">系统设置</h1>
          <div className="flex space-x-2">
            <Button variant="outline" onClick={fetchSystemConfigs} disabled={loading}>
              <RefreshCw className="w-4 h-4 mr-2" />
              刷新配置
            </Button>
            {hasChanges() && (
              <>
                <Button variant="outline" onClick={handleResetAll}>
                  重置更改
                </Button>
                <Button onClick={handleSaveAll} disabled={saving}>
                  <Save className="w-4 h-4 mr-2" />
                  {saving ? '保存中...' : '保存所有更改'}
                </Button>
              </>
            )}
          </div>
        </div>

        {/* 配置变更提示 */}
        {hasChanges() && (
          <Card className="border-orange-200 bg-orange-50">
            <CardContent className="pt-6">
              <div className="flex items-center space-x-2">
                <Settings className="w-5 h-5 text-orange-600" />
                <span className="text-orange-800">
                  您有 {Object.keys(editedConfigs).length} 项未保存的配置更改
                </span>
              </div>
            </CardContent>
          </Card>
        )}

        {/* 配置分类 */}
        <div className="space-y-6">
          {categories.map((category) => (
            <Card key={category.name}>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  {category.icon}
                  {category.name}
                </CardTitle>
                <CardDescription>
                  管理 {category.name.toLowerCase()} 相关的系统参数
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {category.configs.map((config) => (
                    <div key={`${config.category}.${config.key}`} className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 border rounded-lg">
                      <div>
                        <div className="font-medium text-sm">{config.key}</div>
                        <div className="text-sm text-gray-500 mt-1">{config.description}</div>
                      </div>
                      <div className="flex items-center">
                        <Badge variant="outline" className="text-xs">
                          {config.type}
                        </Badge>
                      </div>
                      <div className="flex items-center justify-end">
                        {renderConfigValue(config)}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* 配置说明 */}
        <Card>
          <CardHeader>
            <CardTitle>配置说明</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <h4 className="font-medium mb-2">配置类型说明</h4>
                <ul className="text-sm text-gray-600 space-y-1">
                  <li><strong>string</strong>: 文本类型配置</li>
                  <li><strong>number</strong>: 数值类型配置</li>
                  <li><strong>boolean</strong>: 布尔类型配置（启用/禁用）</li>
                  <li><strong>json</strong>: JSON格式配置</li>
                </ul>
              </div>
              <div>
                <h4 className="font-medium mb-2">注意事项</h4>
                <ul className="text-sm text-gray-600 space-y-1">
                  <li>• 配置更改后需要重启服务才能生效</li>
                  <li>• 建议在维护窗口期间进行配置修改</li>
                  <li>• 修改前请备份当前配置</li>
                  <li>• 敏感配置请谨慎修改</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </MainLayout>
  )
}