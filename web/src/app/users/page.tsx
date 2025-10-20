'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'
import type { User, ApiResponse } from '@/types'
import { Plus, Search, Edit, Trash2 } from 'lucide-react'

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')

  useEffect(() => {
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    try {
      setLoading(true)
      // 模拟API调用，实际使用时替换为真实的API
      setTimeout(() => {
        const mockUsers: User[] = [
          {
            userId: 1,
            userName: 'admin',
            userNick: '系统管理员',
            sex: 'M',
            timeout: -1,
            appId: 'admin_app_001',
            createAt: '2024-01-01T00:00:00',
            updateAt: '2024-01-01T00:00:00'
          },
          {
            userId: 2,
            userName: 'test_user',
            userNick: '测试用户',
            sex: 'F',
            timeout: 24,
            appId: 'test_app_002',
            createAt: '2024-01-15T10:30:00',
            updateAt: '2024-01-15T10:30:00'
          }
        ]
        setUsers(mockUsers)
        setLoading(false)
      }, 1000)
    } catch (error) {
      console.error('获取用户列表失败:', error)
      setLoading(false)
    }
  }

  const filteredUsers = users.filter(user =>
    user.userName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.userNick.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const handleDeleteUser = async (userId: number) => {
    if (confirm('确定要删除该用户吗？此操作不可恢复。')) {
      try {
        // 调用删除API
        // await apiClient.delete(`/admin/user/delete/${userId}`)
        // 刷新列表
        fetchUsers()
      } catch (error) {
        console.error('删除用户失败:', error)
      }
    }
  }

  return (
    <MainLayout>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">用户管理</h1>
            <p className="text-gray-600 mt-1">管理系统用户账户和权限</p>
          </div>
          <Button>
            <Plus className="w-4 h-4 mr-2" />
            新建用户
          </Button>
        </div>

        {/* 搜索和过滤 */}
        <Card>
          <CardHeader>
            <CardTitle>搜索用户</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                placeholder="搜索用户名或昵称..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
          </CardContent>
        </Card>

        {/* 用户列表 */}
        <Card>
          <CardHeader>
            <CardTitle>用户列表</CardTitle>
            <CardDescription>
              共 {filteredUsers.length} 个用户
            </CardDescription>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="text-center py-8">
                <div className="text-gray-500">加载中...</div>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-3 px-4 font-medium text-gray-900">用户ID</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">用户名</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">昵称</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">性别</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">应用ID</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">超时时间</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">创建时间</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.map((user) => (
                      <tr key={user.userId} className="border-b hover:bg-gray-50">
                        <td className="py-3 px-4">{user.userId}</td>
                        <td className="py-3 px-4 font-medium">{user.userName}</td>
                        <td className="py-3 px-4">{user.userNick}</td>
                        <td className="py-3 px-4">
                          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                            {user.sex === 'M' ? '男' : user.sex === 'F' ? '女' : '未知'}
                          </span>
                        </td>
                        <td className="py-3 px-4 font-mono text-sm">{user.appId}</td>
                        <td className="py-3 px-4">
                          {user.timeout === -1 ? (
                            <span className="text-green-600">永不过期</span>
                          ) : (
                            <span>{user.timeout}小时</span>
                          )}
                        </td>
                        <td className="py-3 px-4 text-sm text-gray-500">
                          {new Date(user.createAt).toLocaleDateString()}
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex space-x-2">
                            <Button variant="ghost" size="sm">
                              <Edit className="w-4 h-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDeleteUser(user.userId)}
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
      </div>
    </MainLayout>
  )
}
