'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'
import { Users, Server, FileText, Activity } from 'lucide-react'

interface DashboardStats {
  userCount: number
  supplierCount: number
  todayLogs: number
  systemStatus: string
}

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats>({
    userCount: 0,
    supplierCount: 0,
    todayLogs: 0,
    systemStatus: 'healthy'
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        // 这里可以调用多个API获取统计数据
        // 暂时使用模拟数据
        setTimeout(() => {
          setStats({
            userCount: 12,
            supplierCount: 8,
            todayLogs: 1234,
            systemStatus: 'healthy'
          })
          setLoading(false)
        }, 1000)
      } catch (error) {
        console.error('获取仪表板数据失败:', error)
        setLoading(false)
      }
    }

    fetchDashboardData()
  }, [])

  return (
    <MainLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">系统概览</h1>
          <p className="text-gray-600 mt-1">欢迎使用 HeyTrip 供应商集成管理系统</p>
        </div>

        {/* 统计卡片 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">系统用户</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{loading ? '-' : stats.userCount}</div>
              <p className="text-xs text-muted-foreground">
                当前活跃用户数量
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">供应商数量</CardTitle>
              <Server className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{loading ? '-' : stats.supplierCount}</div>
              <p className="text-xs text-muted-foreground">
                已集成的供应商
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">今日日志</CardTitle>
              <FileText className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{loading ? '-' : stats.todayLogs.toLocaleString()}</div>
              <p className="text-xs text-muted-foreground">
                今日API调用次数
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">系统状态</CardTitle>
              <Activity className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-600">
                {loading ? '-' : stats.systemStatus === 'healthy' ? '正常' : '异常'}
              </div>
              <p className="text-xs text-muted-foreground">
                所有服务运行正常
              </p>
            </CardContent>
          </Card>
        </div>

        {/* 快速操作 */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>快速操作</CardTitle>
              <CardDescription>常用的系统管理操作</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">创建新用户</span>
                <button className="text-blue-600 hover:text-blue-800 text-sm">前往 →</button>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">查看系统日志</span>
                <button className="text-blue-600 hover:text-blue-800 text-sm">前往 →</button>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">供应商健康检查</span>
                <button className="text-blue-600 hover:text-blue-800 text-sm">前往 →</button>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>系统信息</CardTitle>
              <CardDescription>当前系统运行状态</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">后端服务</span>
                <span className="text-sm text-green-600">运行中 (9090)</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">前端服务</span>
                <span className="text-sm text-green-600">运行中 (9091)</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">数据库连接</span>
                <span className="text-sm text-green-600">正常</span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </MainLayout>
  )
}
