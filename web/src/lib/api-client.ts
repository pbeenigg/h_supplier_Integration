import axios from 'axios'
import { md5, generateSignature } from './md5.js'

// 创建axios实例
const apiClient = axios.create({
  baseURL: process.env.NODE_ENV === 'production'
    ? (process.env.NEXT_PUBLIC_API_BASE_URL || '/api')
    : (process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:9090'),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 不需要签名认证的接口列表
const NO_AUTH_ENDPOINTS = [
  '/auth/login',
  '/auth/logout'
]

// 检查登录是否过期的工具函数
function checkLoginExpiration(): boolean {
  try {
    const userInfo = localStorage.getItem('pax_user')
    const loginTimestamp = localStorage.getItem('pax_login_time')
    const timeoutHours = localStorage.getItem('pax_timeout_hours')

    if (!userInfo || !loginTimestamp || timeoutHours === null) {
      return false // 缺少必要信息，认为未登录
    }

    const loginTime = parseInt(loginTimestamp, 10)
    const timeout = parseInt(timeoutHours, 10)

    // 如果 timeout = -1，表示永不过期
    if (timeout === -1) {
      console.log('用户登录设置为永不过期')
      return true
    }

    // 如果 timeout > -1，检查是否过期
    if (timeout > -1) {
      const currentTime = Date.now()
      const expirationTime = loginTime + (timeout * 60 * 60 * 1000) // 转换为毫秒

      if (currentTime > expirationTime) {
        // 登录已过期，清理本地缓存
        console.warn('登录已过期，正在清理本地缓存')
        clearLoginCache()
        return false
      }
    }

    return true // 登录仍然有效
  } catch (error) {
    console.error('检查登录过期时发生错误:', error)
    clearLoginCache()
    return false
  }
}

// 清理登录缓存的工具函数
function clearLoginCache(): void {
  console.log('开始清理登录缓存...')

  // 记录清理前的状态
  const beforeToken = localStorage.getItem('pax_token')
  const beforeUser = localStorage.getItem('pax_user')
  const beforeLoginTime = localStorage.getItem('pax_login_time')
  const beforeTimeout = localStorage.getItem('pax_timeout_hours')

  console.log('清理前缓存状态:', {
    token: beforeToken ? '存在' : '不存在',
    user: beforeUser ? '存在' : '不存在',
    loginTime: beforeLoginTime ? '存在' : '不存在',
    timeout: beforeTimeout ? '存在' : '不存在'
  })

  // 清理缓存
  localStorage.removeItem('pax_token')
  localStorage.removeItem('pax_user')
  localStorage.removeItem('pax_login_time')
  localStorage.removeItem('pax_timeout_hours')

  // 验证清理结果
  const afterToken = localStorage.getItem('pax_token')
  const afterUser = localStorage.getItem('pax_user')
  const afterLoginTime = localStorage.getItem('pax_login_time')
  const afterTimeout = localStorage.getItem('pax_timeout_hours')

  console.log('清理后缓存状态:', {
    token: afterToken ? '仍存在' : '已清除',
    user: afterUser ? '仍存在' : '已清除',
    loginTime: afterLoginTime ? '仍存在' : '已清除',
    timeout: afterTimeout ? '仍存在' : '已清除'
  })

  console.log('登录缓存清理完成')
}

// 调用后端退出登录接口
async function callLogoutAPI(): Promise<boolean> {
  try {
    console.log('调用后端退出登录接口...')

    // 获取用户名
    const userInfo = localStorage.getItem('pax_user')
    if (!userInfo) {
      console.warn('未找到用户信息，跳过后端退出接口调用')
      return false
    }

    const parsedUserInfo = JSON.parse(userInfo)
    const userName = parsedUserInfo.userName

    if (!userName) {
      console.warn('用户名为空，跳过后端退出接口调用')
      return false
    }

    // 调用退出接口
    const response = await apiClient.post('/auth/logout', { userName })

    // 响应拦截器会处理统一格式，成功的话会有 success 属性
    if ((response as any).success) {
      console.log('后端退出登录成功:', (response as any).message || '退出成功')
      return true
    } else {
      console.warn('后端退出登录失败:', (response as any).message || '未知错误')
      return false
    }
  } catch (error) {
    console.error('调用后端退出登录接口失败:', error)
    return false
  }
}

// 强制退出登录的工具函数（异步版本，用于用户主动退出）
async function forceLogoutAsync(reason: string = '登录已过期', callBackend: boolean = false): Promise<void> {
  console.log(`开始执行异步退出登录流程: ${reason}`)

  if (callBackend) {
    // 尝试调用后端退出接口
    const backendSuccess = await callLogoutAPI()

    if (backendSuccess) {
      console.log('后端退出成功，继续清理本地缓存')
    } else {
      console.log('后端退出失败或跳过，直接清理本地缓存')
    }
  }

  // 无论后端接口是否成功，都要清理本地缓存
  clearLoginCache()

  if (typeof window !== 'undefined') {
    console.warn(`${reason}，正在跳转到登录页面`)
    window.location.href = '/login'
  }
}

// 强制退出登录的工具函数（同步版本，用于自动过期等情况）
function forceLogout(reason: string = '登录已过期'): void {
  console.log(`开始执行同步退出登录流程: ${reason}`)

  // 直接清理本地缓存，不调用后端接口
  clearLoginCache()

  if (typeof window !== 'undefined') {
    console.warn(`${reason}，正在跳转到登录页面`)
    window.location.href = '/login'
  }
}// 请求拦截器 - 根据接口类型添加认证头部
apiClient.interceptors.request.use(
  async (config) => {
    const url = config.url || ''

    // 检查是否是不需要认证的接口
    const isNoAuthEndpoint = NO_AUTH_ENDPOINTS.some(endpoint => url.includes(endpoint))

    if (!isNoAuthEndpoint) {
      // 对于需要认证的接口，首先检查登录是否过期
      if (!checkLoginExpiration()) {
        // 登录已过期，直接跳转登录页面
        forceLogout('登录会话已过期')
        return Promise.reject(new Error('登录会话已过期，请重新登录'))
      }

      // 需要认证的接口，添加签名头部
      const userInfo = localStorage.getItem('pax_user')

      if (userInfo) {
        try {
          const parsedUserInfo = JSON.parse(userInfo)
          const appId = parsedUserInfo.appId
          const secretKey = parsedUserInfo.secretKey || 'default_secret' // 从登录响应中获取

          if (appId) {
            const timestamp = Math.floor(Date.now() / 1000).toString()
            const sign = generateSignature(appId, secretKey, timestamp)

            config.headers['app'] = appId
            config.headers['timestamp'] = timestamp
            config.headers['sign'] = sign
          }
        } catch (error) {
          console.warn('解析用户信息失败:', error)
        }
      } else {
        // 没有用户信息但需要认证的接口，跳转登录
        forceLogout('未找到登录信息')
        return Promise.reject(new Error('未找到登录信息，请重新登录'))
      }
    }

    // 从localStorage获取token（用于已登录用户的请求）
    const token = localStorage.getItem('pax_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理统一响应格式
apiClient.interceptors.response.use(
  (response) => {
    // 后端统一响应格式为 { code, msg, data, timestamp }
    const originalData = response.data

    // 如果响应状态是200，认为是成功的
    if (response.status >= 200 && response.status < 300) {
      // 检查是否是后端统一格式
      if (originalData && typeof originalData === 'object' && 'code' in originalData) {
        // 统一响应格式，保持完整结构
        if (originalData.code === 200) {
          return {
            ...response,
            data: {
              code: originalData.code,
              msg: originalData.msg,
              data: originalData.data,
              timestamp: originalData.timestamp
            }
          }
        } else {
          // 业务错误，但仍保持统一格式
          return Promise.reject({
            code: originalData.code,
            msg: originalData.msg || originalData.message || '请求失败',
            data: originalData.data,
            timestamp: originalData.timestamp
          })
        }
      } else {
        // 非统一格式的响应，包装成统一格式
        return {
          ...response,
          data: {
            code: 200,
            msg: 'success',
            data: originalData,
            timestamp: Date.now()
          }
        }
      }
    } else {
      // HTTP错误状态
      return Promise.reject({
        code: response.status,
        msg: originalData?.message || originalData?.msg || '请求失败',
        data: null
      })
    }
  },
  (error) => {
    console.error('API请求错误:', error)

    // 处理HTTP错误
    if (error.response?.status === 401) {
      // 只有在明确的认证失败时才清除token并跳转
      // 避免因为其他原因的401错误（如接口不存在）导致意外退出
      const errorMessage = error.response?.data?.message || error.message || ''

      console.error

      // 检查是否是真正的认证失败
      if (errorMessage.includes('认证') || errorMessage.includes('token') || errorMessage.includes('unauthorized')) {
        forceLogout('认证失败')
        return Promise.reject(new Error('认证已过期，请重新登录'))
      }
    }

    // 对于404或其他错误，不强制退出登录
    if (error.response?.status === 404) {
      return Promise.reject(new Error('接口不存在或服务暂时不可用'))
    }

    // 提供更友好的错误信息
    const errorMessage = error.response?.data?.message ||
      error.message ||
      `请求失败 (${error.response?.status || 'Network Error'})`

    return Promise.reject(new Error(errorMessage))
  }
)

// 缓存管理相关API
export const fetchCacheKeys = async (cacheName: string, prefix?: string) => {
  const params: { cacheName: string; prefix?: string } = { cacheName }
  if (prefix && prefix.trim()) {
    params.prefix = prefix
  }

  const response = await apiClient.get('/cache/keys', { params })
  return response.data
}

export const getCacheValue = async (cacheName: string, key: string) => {
  const response = await apiClient.get('/cache/get', {
    params: { cacheName, key }
  })
  return response.data
}

// 登录相关辅助函数
export const saveLoginInfo = (userInfo: any, token: string, timeoutHours: number) => {
  const loginTime = Date.now()

  // 保存用户信息
  localStorage.setItem('pax_user', JSON.stringify(userInfo))
  localStorage.setItem('pax_token', token)
  localStorage.setItem('pax_login_time', loginTime.toString())
  localStorage.setItem('pax_timeout_hours', timeoutHours.toString())

  console.log(`登录成功，会话将在 ${timeoutHours} 小时后过期`)
}

// 获取登录剩余时间（分钟）
// 返回值：-1表示永不过期，0表示已过期或未登录，>0表示剩余分钟数
export const getRemainingLoginTime = (): number => {
  try {
    const loginTimestamp = localStorage.getItem('pax_login_time')
    const timeoutHours = localStorage.getItem('pax_timeout_hours')

    if (!loginTimestamp || timeoutHours === null) {
      return 0
    }

    const loginTime = parseInt(loginTimestamp, 10)
    const timeout = parseInt(timeoutHours, 10)

    // 如果 timeout = -1，表示永不过期
    if (timeout === -1) {
      return -1
    }

    // 如果 timeout > -1，计算剩余时间
    if (timeout > -1) {
      const currentTime = Date.now()
      const expirationTime = loginTime + (timeout * 60 * 60 * 1000)
      const remainingTime = expirationTime - currentTime

      return remainingTime > 0 ? Math.floor(remainingTime / (1000 * 60)) : 0 // 返回分钟
    }

    return 0
  } catch (error) {
    console.error('获取剩余登录时间失败:', error)
    return 0
  }
}

// 手动登出函数
export const logout = async () => {
  await forceLogoutAsync('用户主动登出', true) // true 表示需要调用后端接口
}

// 导出清理缓存函数，用于紧急情况下的手动清理
export const clearCache = () => {
  clearLoginCache()
}

export { apiClient }
