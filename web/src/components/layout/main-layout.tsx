'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { logout, getRemainingLoginTime, clearCache } from '@/lib/api-client'
import {
  Users,
  Settings,
  FileText,
  Server,
  Monitor,
  LogOut,
  Home,
  Database,
  Shield,
  Clock
} from 'lucide-react'

const navigation = [
  { name: '概览', href: '/dashboard', icon: Home },
  { name: '用户', href: '/users', icon: Users },
  { name: '应用', href: '/apps', icon: Shield },
  { name: '供应商', href: '/suppliers', icon: Server },
  { name: '日志', href: '/logs', icon: FileText },
  { name: '监控', href: '/monitor', icon: Monitor },
  { name: '缓存', href: '/cache', icon: Database },
  { name: '系统', href: '/settings', icon: Settings },
]

interface MainLayoutProps {
  children: React.ReactNode
}

export default function MainLayout({ children }: MainLayoutProps) {
  const pathname = usePathname()
  const [remainingTime, setRemainingTime] = useState(0)
  const [userName, setUserName] = useState('')

  useEffect(() => {
    // 获取用户名
    try {
      const userInfo = localStorage.getItem('pax_user')
      if (userInfo) {
        const parsed = JSON.parse(userInfo)
        setUserName(parsed.userNick || parsed.userName || '用户')
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }

    // 更新剩余时间
    const updateRemainingTime = () => {
      setRemainingTime(getRemainingLoginTime())
    }

    updateRemainingTime() // 立即更新一次
    const interval = setInterval(updateRemainingTime, 60000) // 每分钟更新一次

    return () => clearInterval(interval)
  }, [])

  const handleLogout = async () => {
    console.log('用户点击退出按钮，开始执行登出流程...')

    try {
      // 主要的登出流程（调用后端接口）
      await logout()
    } catch (error) {
      console.error('登出过程发生错误:', error)

      // 备选方案：手动清理缓存并跳转
      console.log('使用备选方案清理缓存...')
      clearCache()
      window.location.href = '/login'
    }
  }

  // 格式化剩余时间显示
  const formatRemainingTime = (minutes: number) => {
    if (minutes === -1) return '永不过期'
    if (minutes <= 0) return '已过期'
    if (minutes < 60) return `${minutes}分钟`
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return `${hours}小时${mins > 0 ? mins + '分钟' : ''}`
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 顶部导航栏 */}
      <header className="bg-white shadow-sm border-b">
        <div className="mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-semibold text-gray-900">
                HeyTrip 供应商集成管理系统
              </h1>
            </div>

            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-3 text-sm text-gray-500">
                <span>欢迎，{userName}</span>
                <div className="flex items-center space-x-1">
                  <Clock className="w-4 h-4" />
                  <span className={cn(
                    'font-medium',
                    remainingTime === -1 ? 'text-blue-600' : // 永不过期 - 蓝色
                      remainingTime <= 0 ? 'text-gray-600' : // 已过期 - 灰色
                        remainingTime <= 30 ? 'text-red-600' : // 30分钟内 - 红色
                          remainingTime <= 60 ? 'text-orange-600' : // 1小时内 - 橙色
                            'text-green-600' // 超过1小时 - 绿色
                  )}>
                    剩余: {formatRemainingTime(remainingTime)}
                  </span>
                </div>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleLogout}
                className="text-gray-500 hover:text-gray-700"
              >
                <LogOut className="w-4 h-4 mr-2" />
                退出
              </Button>
            </div>
          </div>
        </div>

        {/* 主导航菜单 */}
        <div className="border-t border-gray-200">
          <div className="mx-auto px-4 sm:px-6 lg:px-8">
            <nav className="flex space-x-8">
              {navigation.map((item) => {
                const isActive = pathname === item.href || pathname.startsWith(item.href + '/')
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={cn(
                      'flex items-center px-3 py-4 text-sm font-medium border-b-2 transition-colors',
                      isActive
                        ? 'border-blue-500 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                    )}
                  >
                    <item.icon className="w-4 h-4 mr-2" />
                    {item.name}
                  </Link>
                )
              })}
            </nav>
          </div>
        </div>
      </header>

      {/* 主内容区域 */}
      <main className="py-6">
        <div className="mx-auto px-4 sm:px-6 lg:px-8">
          {children}
        </div>
      </main>
    </div>
  )
}
