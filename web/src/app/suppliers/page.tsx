'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'
import type { SupplierConfig } from '@/types'
import { Server, Settings, Activity, AlertCircle } from 'lucide-react'

export default function SuppliersPage() {
  const [suppliers, setSuppliers] = useState<SupplierConfig[]>([])
  const [loading, setLoading] = useState(true)
  const [healthStatus, setHealthStatus] = useState<{ [key: string]: boolean }>({})

  useEffect(() => {
    fetchSuppliers()
    checkSuppliersHealth()
  }, [])

  const fetchSuppliers = async () => {
    try {
      setLoading(true)
      // 模拟数据，实际使用时调用真实API
      setTimeout(() => {
        const mockSuppliers: SupplierConfig[] = [
          {
            id: 1,
            supplierName: 'Asianoverland',
            supplierCode: 'AOL',
            baseUrl: 'https://api.asianoverland.com',
            authConfig: '{"apiKey":"***","secret":"***"}',
            ftpConfig: '{"host":"ftp.example.com","username":"***"}',
            retryCount: 3,
            priority: 1,
            isActive: true,
            createdAt: '2024-01-01T00:00:00',
            updatedAt: '2024-01-01T00:00:00'
          },
          {
            id: 2,
            supplierName: 'TestSupplier',
            supplierCode: 'TEST',
            baseUrl: 'https://api.testsupplier.com',
            authConfig: '{"username":"test","password":"***"}',
            ftpConfig: '{}',
            retryCount: 2,
            priority: 2,
            isActive: false,
            createdAt: '2024-01-15T10:30:00',
            updatedAt: '2024-01-15T10:30:00'
          }
        ]
        setSuppliers(mockSuppliers)
        setLoading(false)
      }, 1000)
    } catch (error) {
      console.error('获取供应商列表失败:', error)
      setLoading(false)
    }
  }

  const checkSuppliersHealth = async () => {
    try {
      // 模拟健康检查结果
      setTimeout(() => {
        setHealthStatus({
          'Asianoverland': true,
          'TestSupplier': false
        })
      }, 1500)
    } catch (error) {
      console.error('健康检查失败:', error)
    }
  }

  const handleToggleSupplier = async (supplierName: string, isActive: boolean) => {
    try {
      // 调用API切换供应商状态
      // await apiClient.put('/suppliers', { supplierName, isActive: !isActive })
      console.log(`切换供应商 ${supplierName} 状态为: ${!isActive}`)
      fetchSuppliers()
    } catch (error) {
      console.error('切换供应商状态失败:', error)
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
          <Button onClick={checkSuppliersHealth}>
            <Activity className="w-4 h-4 mr-2" />
            健康检查
          </Button>
        </div>

        {/* 供应商列表 */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {loading ? (
            <div className="col-span-2 text-center py-8">
              <div className="text-gray-500">加载中...</div>
            </div>
          ) : (
            suppliers.map((supplier) => (
              <Card key={supplier.id} className={`${supplier.isActive ? 'border-green-200' : 'border-gray-200'}`}>
                <CardHeader>
                  <div className="flex justify-between items-center">
                    <CardTitle className="flex items-center">
                      <Server className="w-5 h-5 mr-2" />
                      {supplier.supplierName}
                    </CardTitle>
                    <div className="flex items-center space-x-2">
                      {healthStatus[supplier.supplierName] !== undefined && (
                        <div className={`w-3 h-3 rounded-full ${
                          healthStatus[supplier.supplierName] ? 'bg-green-500' : 'bg-red-500'
                        }`} title={healthStatus[supplier.supplierName] ? '健康' : '异常'} />
                      )}
                      <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                        supplier.isActive
                          ? 'bg-green-100 text-green-800'
                          : 'bg-gray-100 text-gray-800'
                      }`}>
                        {supplier.isActive ? '启用' : '禁用'}
                      </span>
                    </div>
                  </div>
                  <CardDescription>
                    供应商代码: {supplier.supplierCode} | 优先级: {supplier.priority}
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <label className="text-sm font-medium text-gray-700">API地址</label>
                    <p className="text-sm text-gray-600 font-mono">{supplier.baseUrl}</p>
                  </div>

                  <div>
                    <label className="text-sm font-medium text-gray-700">重试次数</label>
                    <p className="text-sm text-gray-600">{supplier.retryCount} 次</p>
                  </div>

                  <div>
                    <label className="text-sm font-medium text-gray-700">认证配置</label>
                    <p className="text-sm text-gray-600">已配置</p>
                  </div>

                  <div className="flex justify-between items-center pt-4 border-t">
                    <div className="text-xs text-gray-500">
                      更新时间: {new Date(supplier.updatedAt).toLocaleDateString()}
                    </div>
                    <div className="flex space-x-2">
                      <Button variant="outline" size="sm">
                        <Settings className="w-4 h-4 mr-1" />
                        配置
                      </Button>
                      <Button
                        variant={supplier.isActive ? "destructive" : "default"}
                        size="sm"
                        onClick={() => handleToggleSupplier(supplier.supplierName, supplier.isActive)}
                      >
                        {supplier.isActive ? '禁用' : '启用'}
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
            <CardDescription>所有供应商的连接状态</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="text-center">
                <div className="text-2xl font-bold text-green-600">
                  {Object.values(healthStatus).filter(Boolean).length}
                </div>
                <div className="text-sm text-gray-500">健康</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-red-600">
                  {Object.values(healthStatus).filter(status => !status).length}
                </div>
                <div className="text-sm text-gray-500">异常</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-gray-600">
                  {suppliers.filter(s => s.isActive).length}
                </div>
                <div className="text-sm text-gray-500">已启用</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </MainLayout>
  )
}
