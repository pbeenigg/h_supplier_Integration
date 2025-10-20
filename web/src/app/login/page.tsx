'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { apiClient, saveLoginInfo } from '@/lib/api-client'
import type { LoginRequest, LoginResponse } from '@/types'

export default function LoginPage() {
  const [userName, setUserName] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const router = useRouter()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      const loginData: LoginRequest = { userName, password }
      const response = await apiClient.post<LoginResponse>('/auth/login', loginData)
      console.log('登录响应:', response)

      // 构建用户信息对象
      const userInfo = {
        userId: response.data.userId,
        userName: response.data.userName,
        userNick: response.data.userNick,
        appId: response.data.appId,
        secretKey: response.data.secretKey,
        loginTime: response.data.loginTime,
        timeout: response.data.timeout
      }

      // 使用新的保存函数，自动处理登录时间和过期时间
      const token = response.data.userName // 临时使用userName作为token
      const timeoutHours = response.data.timeout || 8 // 默认8小时，如果后端没有返回timeout字段

      saveLoginInfo(userInfo, token, timeoutHours)
      console.log('登录成功，用户信息已保存:', userInfo)

      // 跳转到主页
      router.push('/dashboard')
    } catch (err: any) {
      console.error('登录失败:', err)
      setError(err.message || '登录失败，请检查用户名和密码')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-bold">系统登录</CardTitle>
          <CardDescription>
            HeyTrip 供应商集成管理系统
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
                {error}
              </div>
            )}

            <div>
              <label htmlFor="userName" className="block text-sm font-medium text-gray-700 mb-1">
                用户名
              </label>
              <Input
                id="userName"
                type="text"
                value={userName}
                onChange={(e) => setUserName(e.target.value)}
                placeholder="请输入用户名"
                required
                disabled={loading}
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
                密码
              </label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="请输入密码"
                required
                disabled={loading}
              />
            </div>

            <Button
              type="submit"
              className="w-full"
              disabled={loading}
            >
              {loading ? '登录中...' : '登录'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
