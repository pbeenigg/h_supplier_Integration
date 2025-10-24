'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import MainLayout from '@/components/layout/main-layout'
import { apiClient } from '@/lib/api-client'
import type { User, UserListResponse, UserUpdateRequest, ResetPasswordRequest, UserCreateRequest, AppCreateRequest, UserAppBindRequest } from '@/types'
import { Search, Edit, Trash2, Key, RefreshCw, Users, UserCheck, UserX, Clock, X, Save, Plus, Link, Unlink } from 'lucide-react'

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [showEditDialog, setShowEditDialog] = useState(false)
  const [showPasswordDialog, setShowPasswordDialog] = useState(false)
  const [showCreateDialog, setShowCreateDialog] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [editForm, setEditForm] = useState<UserUpdateRequest>({ userId: 0 })
  const [newPassword, setNewPassword] = useState('')
  const [createForm, setCreateForm] = useState<UserCreateRequest>({
    userName: '',
    password: '',
    userNick: '',
    sex: 'U',
    timeout: 24
  })
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    try {
      setLoading(true)
      const response = await apiClient.get<UserListResponse>('/admin/user/list')

      if (response.data && response.data.code === 200) {
        setUsers(response.data.data)
      } else {
        throw new Error(response.data?.msg || '获取用户列表失败')
      }
    } catch (error) {
      console.error('获取用户列表失败:', error)
      alert('获取用户列表失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  const refreshData = () => {
    fetchUsers()
  }

  const filteredUsers = users.filter(user =>
    user.userName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.userNick.toLowerCase().includes(searchTerm.toLowerCase())
  )

  // 处理编辑用户
  const handleEditUser = (user: User) => {
    setEditingUser(user)
    setEditForm({
      userId: user.userId,
      userNick: user.userNick,
      sex: user.sex,
      timeout: user.timeout
    })
    setShowEditDialog(true)
  }

  // 保存用户信息
  const handleSaveUser = async () => {
    if (!editingUser) return

    try {
      setSaving(true)
      const response = await apiClient.put('/admin/user/update', editForm)

      if (response.data && response.data.code === 200) {
        alert('用户信息更新成功')
        setShowEditDialog(false)
        setEditingUser(null)
        setEditForm({ userId: 0 })
        fetchUsers()
      } else {
        throw new Error(response.data?.msg || '更新用户信息失败')
      }
    } catch (error) {
      console.error('更新用户信息失败:', error)
      alert('更新用户信息失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  // 重置密码
  const handleResetPassword = (user: User) => {
    setEditingUser(user)
    setNewPassword('')
    setShowPasswordDialog(true)
  }

  const handleSavePassword = async () => {
    if (!editingUser || !newPassword.trim()) {
      alert('请输入新密码')
      return
    }

    try {
      setSaving(true)
      const passwordData: ResetPasswordRequest = {
        userId: editingUser.userId,
        newPassword: newPassword.trim()
      }

      const response = await apiClient.post('/user/reset-password', passwordData)

      if (response.data && response.data.code === 200) {
        alert('密码重置成功')
        setShowPasswordDialog(false)
        setEditingUser(null)
        setNewPassword('')
      } else {
        throw new Error(response.data?.msg || '重置密码失败')
      }
    } catch (error) {
      console.error('重置密码失败:', error)
      alert('重置密码失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  // 创建用户
  const handleCreateUser = async () => {
    if (!createForm.userName.trim() || !createForm.password.trim() || !createForm.userNick.trim()) {
      alert('请填写完整的用户信息')
      return
    }

    try {
      setSaving(true)
      const response = await apiClient.put('/admin/user/create', createForm)

      if (response.data && response.data.code === 200) {
        alert('创建用户成功')
        setShowCreateDialog(false)
        setCreateForm({
          userName: '',
          password: '',
          userNick: '',
          sex: 'U',
          timeout: 24
        })
        fetchUsers()
      } else {
        throw new Error(response.data?.msg || '创建用户失败')
      }
    } catch (error) {
      console.error('创建用户失败:', error)
      alert('创建用户失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  // 生成应用ID
  const generateAppId = (userName: string) => {
    const timestamp = Date.now()
    return `app_${userName}_${timestamp}`
  }

  // 生成密钥
  const generateSecretKey = () => {
    const timestamp = Date.now()
    const uuid = crypto.randomUUID().replace(/-/g, '')
    return `SK_${uuid}_${timestamp}`
  }

  // 生成加密密钥
  const generateEncryptionKey = () => {
    return crypto.randomUUID().replace(/-/g, '')
  }

  // 绑定应用
  const handleBindApp = async (user: User) => {
    if (!confirm('确定要为该用户绑定新应用吗？')) return

    try {
      setSaving(true)

      // 1. 创建新应用
      const appData: AppCreateRequest = {
        appId: generateAppId(user.userName),
        secretKey: generateSecretKey(),
        encryptionKey: generateEncryptionKey(),
        rateLimit: 1000,
        timeout: user.timeout
      }

      const appResponse = await apiClient.put('/app/create', appData)
      if (appResponse.data && appResponse.data.code === 200) {
        // 2. 更新用户的 appId
        const userUpdateData: UserAppBindRequest = {
          userId: user.userId,
          appId: appData.appId
        }

        const userResponse = await apiClient.put('/user/update', userUpdateData)
        if (userResponse.data && userResponse.data.code === 200) {
          alert('应用绑定成功')
          fetchUsers()
        } else {
          throw new Error(userResponse.data?.msg || '绑定应用失败')
        }
      } else {
        throw new Error(appResponse.data?.msg || '创建应用失败')
      }
    } catch (error) {
      console.error('绑定应用失败:', error)
      alert('绑定应用失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  // 解绑应用
  const handleUnbindApp = async (user: User) => {
    if (!confirm('确定要解绑该用户的应用吗？此操作将删除关联的应用记录。')) return

    try {
      setSaving(true)

      // 1. 清空用户的 appId
      const userUpdateData: UserAppBindRequest = {
        userId: user.userId,
        appId: ''
      }

      const userResponse = await apiClient.put('/user/update', userUpdateData)
      if (userResponse.data && userResponse.data.code === 200) {
        // 2. 删除对应的应用记录
        if (user.appId) {
          const appResponse = await apiClient.delete(`/app/delete/${user.appId}`)
          if (appResponse.data && appResponse.data.code === 200) {
            alert('应用解绑成功')
            fetchUsers()
          } else {
            throw new Error(appResponse.data?.msg || '删除应用失败')
          }
        } else {
          alert('应用解绑成功')
          fetchUsers()
        }
      } else {
        throw new Error(userResponse.data?.msg || '解绑应用失败')
      }
    } catch (error) {
      console.error('解绑应用失败:', error)
      alert('解绑应用失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  // 删除用户
  const handleDeleteUser = async (userId: number) => {
    if (confirm('确定要删除该用户吗？此操作不可恢复。')) {
      try {
        setSaving(true)
        const response = await apiClient.delete(`/user/delete/${userId}`)

        if (response.data && response.data.code === 200) {
          alert('用户删除成功')
          fetchUsers()
        } else {
          throw new Error(response.data?.msg || '删除用户失败')
        }
      } catch (error) {
        console.error('删除用户失败:', error)
        alert('删除用户失败，请稍后重试')
      } finally {
        setSaving(false)
      }
    }
  }

  // 检查用户是否可以删除
  const canDeleteUser = (user: User) => {
    return user.userId !== 1 && user.userName !== 'admin'
  }

  // 获取状态信息
  const getStatusInfo = (user: User) => {
    if (user.expired) {
      return { color: 'text-red-600', bg: 'bg-red-100', text: '已过期' }
    } else if (user.timeout === -1) {
      return { color: 'text-green-600', bg: 'bg-green-100', text: '永久有效' }
    } else {
      return { color: 'text-blue-600', bg: 'bg-blue-100', text: `${user.timeout}小时` }
    }
  }

  // 获取性别文本
  const getSexText = (sex: string) => {
    switch (sex) {
      case 'M': return '男'
      case 'F': return '女'
      case 'U':
      default: return '未知'
    }
  }

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* 页面标题 */}
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">用户管理</h1>
            <p className="text-gray-600 mt-1">管理系统用户账户和权限信息</p>
          </div>
          <div className="flex space-x-2">
            <Button
              onClick={() => setShowCreateDialog(true)}
              disabled={saving}
              className="bg-blue-600 hover:bg-blue-700 text-white"
            >
              <Plus className="w-4 h-4 mr-2" />
              创建用户
            </Button>
            <Button onClick={refreshData} disabled={loading} variant="outline">
              <RefreshCw className="w-4 h-4 mr-2" />
              {loading ? '刷新中...' : '刷新数据'}
            </Button>
          </div>
        </div>

        {/* 统计概览 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <Users className="h-8 w-8 text-blue-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">总用户数</p>
                  <p className="text-2xl font-bold text-gray-900">{users.length}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <UserCheck className="h-8 w-8 text-green-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">正常用户</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {users.filter(u => !u.expired).length}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <UserX className="h-8 w-8 text-red-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">已过期</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {users.filter(u => u.expired).length}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <Clock className="h-8 w-8 text-purple-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">永久有效</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {users.filter(u => u.timeout === -1).length}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和过滤 */}
        <Card>
          <CardHeader>
            <CardTitle>搜索用户</CardTitle>
            <CardDescription>
              根据用户名或昵称搜索用户，共找到 {filteredUsers.length} 个用户
            </CardDescription>
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
              系统中所有用户的详细信息和操作管理
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
                    <tr className="border-b bg-gray-50">
                      <th className="text-left py-3 px-4 font-medium text-gray-900">用户ID</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">用户名</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">昵称</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">性别</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">应用ID</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">超时设置</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">状态</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">创建时间</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.map((user) => {
                      const statusInfo = getStatusInfo(user)
                      return (
                        <tr key={user.userId} className="border-b hover:bg-gray-50">
                          <td className="py-3 px-4 font-mono text-sm">{user.userId}</td>
                          <td className="py-3 px-4 font-medium">{user.userName}</td>
                          <td className="py-3 px-4">{user.userNick}</td>
                          <td className="py-3 px-4">
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                              {getSexText(user.sex)}
                            </span>
                          </td>
                          <td className="py-3 px-4 font-mono text-sm text-gray-600">{user.appId}</td>
                          <td className="py-3 px-4">
                            <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${statusInfo.bg} ${statusInfo.color}`}>
                              {statusInfo.text}
                            </span>
                          </td>
                          <td className="py-3 px-4">
                            <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${user.expired ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'
                              }`}>
                              {user.expired ? '已过期' : '正常'}
                            </span>
                          </td>
                          <td className="py-3 px-4 text-sm text-gray-500">
                            {new Date(user.createAt).toLocaleDateString()}
                          </td>
                          <td className="py-3 px-4">
                            <div className="flex space-x-1">
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleEditUser(user)}
                                disabled={saving}
                                title="编辑用户"
                              >
                                <Edit className="w-4 h-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleResetPassword(user)}
                                disabled={saving}
                                title="重置密码"
                                className="text-blue-600 hover:text-blue-800"
                              >
                                <Key className="w-4 h-4" />
                              </Button>
                              {user.appId ? (
                                canDeleteUser(user) && (
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => handleUnbindApp(user)}
                                    disabled={saving}
                                    title="解绑应用"
                                    className="text-orange-600 hover:text-orange-800"
                                  >
                                    <Unlink className="w-4 h-4" />
                                  </Button>
                                )
                              ) : (
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => handleBindApp(user)}
                                  disabled={saving}
                                  title="绑定应用"
                                  className="text-green-600 hover:text-green-800"
                                >
                                  <Link className="w-4 h-4" />
                                </Button>
                              )}
                              {canDeleteUser(user) && (
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => handleDeleteUser(user.userId)}
                                  disabled={saving}
                                  title="删除用户"
                                  className="text-red-600 hover:text-red-800"
                                >
                                  <Trash2 className="w-4 h-4" />
                                </Button>
                              )}
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
        {/* 编辑用户对话框 */}
        {showEditDialog && editingUser && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-6">
            <div className="bg-white rounded-xl max-w-lg w-full shadow-2xl">
              <div className="flex justify-between items-center px-6 py-4 border-b">
                <h3 className="text-lg font-semibold text-gray-900">编辑用户信息</h3>
                <button
                  onClick={() => {
                    setShowEditDialog(false)
                    setEditingUser(null)
                    setEditForm({ userId: 0 })
                  }}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
              <div className="p-6 space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    用户名
                  </label>
                  <Input
                    value={editingUser.userName}
                    disabled
                    className="bg-gray-50"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    昵称 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    value={editForm.userNick || ''}
                    onChange={(e) => setEditForm({ ...editForm, userNick: e.target.value })}
                    placeholder="请输入用户昵称"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    性别
                  </label>
                  <select
                    value={editForm.sex || 'U'}
                    onChange={(e) => setEditForm({ ...editForm, sex: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="U">未知</option>
                    <option value="M">男</option>
                    <option value="F">女</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    登录超时时间（小时，-1表示永久有效）
                  </label>
                  <Input
                    type="number"
                    value={editForm.timeout !== undefined ? editForm.timeout : ''}
                    onChange={(e) => {
                      const value = e.target.value
                      if (value === '' || value === '-') {
                        setEditForm({ ...editForm, timeout: undefined })
                      } else {
                        const numValue = parseInt(value)
                        setEditForm({ ...editForm, timeout: isNaN(numValue) ? 1 : numValue })
                      }
                    }}
                    placeholder="例如：24 或 -1"
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    设置为-1表示永久有效，大于0表示过期时间（小时）
                  </p>
                </div>
              </div>
              <div className="flex justify-end space-x-3 px-6 py-4 border-t bg-gray-50">
                <Button
                  variant="outline"
                  onClick={() => {
                    setShowEditDialog(false)
                    setEditingUser(null)
                    setEditForm({ userId: 0 })
                  }}
                  disabled={saving}
                >
                  取消
                </Button>
                <Button
                  onClick={handleSaveUser}
                  disabled={saving}
                >
                  <Save className="w-4 h-4 mr-2" />
                  {saving ? '保存中...' : '保存'}
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* 重置密码对话框 */}
        {showPasswordDialog && editingUser && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-6">
            <div className="bg-white rounded-xl max-w-md w-full shadow-2xl">
              <div className="flex justify-between items-center px-6 py-4 border-b">
                <h3 className="text-lg font-semibold text-gray-900">重置用户密码</h3>
                <button
                  onClick={() => {
                    setShowPasswordDialog(false)
                    setEditingUser(null)
                    setNewPassword('')
                  }}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
              <div className="p-6 space-y-4">
                <div className="bg-yellow-50 border border-yellow-200 rounded-md p-3">
                  <div className="flex items-center">
                    <Key className="w-5 h-5 text-yellow-600 mr-2" />
                    <p className="text-sm text-yellow-800">
                      正在为用户 <strong>{editingUser.userName} ({editingUser.userNick})</strong> 重置密码
                    </p>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    新密码 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="请输入新密码"
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    建议密码长度至少8位，包含字母、数字和特殊字符
                  </p>
                </div>
              </div>
              <div className="flex justify-end space-x-3 px-6 py-4 border-t bg-gray-50">
                <Button
                  variant="outline"
                  onClick={() => {
                    setShowPasswordDialog(false)
                    setEditingUser(null)
                    setNewPassword('')
                  }}
                  disabled={saving}
                >
                  取消
                </Button>
                <Button
                  onClick={handleSavePassword}
                  disabled={saving || !newPassword.trim()}
                  className="bg-blue-600 hover:bg-blue-700 text-white"
                >
                  <Key className="w-4 h-4 mr-2" />
                  {saving ? '重置中...' : '重置密码'}
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* 创建用户对话框 */}
        {showCreateDialog && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-6">
            <div className="bg-white rounded-xl max-w-lg w-full shadow-2xl">
              <div className="flex justify-between items-center px-6 py-4 border-b">
                <h3 className="text-lg font-semibold text-gray-900">创建新用户</h3>
                <button
                  onClick={() => {
                    setShowCreateDialog(false)
                    setCreateForm({
                      userName: '',
                      password: '',
                      userNick: '',
                      sex: 'U',
                      timeout: 24
                    })
                  }}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
              <div className="p-6 space-y-4">
                <div className="bg-blue-50 border border-blue-200 rounded-md p-3">
                  <div className="flex items-center">
                    <Plus className="w-5 h-5 text-blue-600 mr-2" />
                    <p className="text-sm text-blue-800">
                      创建用户的同时会自动创建并绑定一个新的应用ID
                    </p>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    用户名 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    value={createForm.userName}
                    onChange={(e) => setCreateForm({ ...createForm, userName: e.target.value })}
                    placeholder="请输入用户名"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    密码 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    type="password"
                    value={createForm.password}
                    onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                    placeholder="请输入密码"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    昵称 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    value={createForm.userNick}
                    onChange={(e) => setCreateForm({ ...createForm, userNick: e.target.value })}
                    placeholder="请输入用户昵称"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    性别
                  </label>
                  <select
                    value={createForm.sex}
                    onChange={(e) => setCreateForm({ ...createForm, sex: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="U">未知</option>
                    <option value="M">男</option>
                    <option value="F">女</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    超时时间（小时，-1表示永久有效）
                  </label>
                  <Input
                    type="number"
                    value={createForm.timeout}
                    onChange={(e) => {
                      const value = e.target.value
                      if (value === '' || value === '-') {
                        setCreateForm({ ...createForm, timeout: 24 })
                      } else {
                        const numValue = parseInt(value)
                        setCreateForm({ ...createForm, timeout: isNaN(numValue) ? 24 : numValue })
                      }
                    }}
                    placeholder="例如：24 或 -1"
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    设置为-1表示永久有效，大于0表示过期时间（小时）
                  </p>
                </div>
              </div>
              <div className="flex justify-end space-x-3 px-6 py-4 border-t bg-gray-50">
                <Button
                  variant="outline"
                  onClick={() => {
                    setShowCreateDialog(false)
                    setCreateForm({
                      userName: '',
                      password: '',
                      userNick: '',
                      sex: 'U',
                      timeout: 24
                    })
                  }}
                  disabled={saving}
                >
                  取消
                </Button>
                <Button
                  onClick={handleCreateUser}
                  disabled={saving || !createForm.userName.trim() || !createForm.password.trim() || !createForm.userNick.trim()}
                  className="bg-blue-600 hover:bg-blue-700 text-white"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  {saving ? '创建中...' : '创建用户'}
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </MainLayout>
  )
}
