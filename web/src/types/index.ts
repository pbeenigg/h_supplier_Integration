// 用户信息接口
export interface User {
  userId: number
  userName: string
  password?: string
  userNick: string
  sex: string
  timeout: number
  appId: string
  updateAt: string
  createAt: string
  updateBy: string
  createBy: string
  expired: boolean
}

// 用户列表API响应格式
export type UserListResponse = UnifiedApiResponse<User[]>

// 用户更新请求接口
export interface UserUpdateRequest {
  userId: number
  userNick?: string
  sex?: string
  timeout?: number
}

// 重置密码请求接口
export interface ResetPasswordRequest {
  userId: number
  newPassword: string
}

// 创建用户请求接口
export interface UserCreateRequest {
  userName: string
  password: string
  userNick: string
  sex: string
  timeout: number
}

// 创建应用请求接口
export interface AppCreateRequest {
  appId: string
  secretKey: string
  encryptionKey: string
  rateLimit: number
  timeout: number
}

// 用户应用绑定更新请求接口
export interface UserAppBindRequest {
  userId: number
  appId?: string
}

// 登录请求接口
export interface LoginRequest {
  userName: string
  password: string
}

// 登录响应接口
export interface LoginResponse {
  userId: number
  userName: string
  userNick: string
  appId: string
  loginTime: number
  secretKey: string
  timeout: number
}

// 应用信息接口
export interface App {
  appId: string
  secretKey: string
  encryptionKey: string
  rateLimit: number
  timeout: number
  updateAt: string
  createAt: string
  updateBy: string
  createBy: string
  expired: boolean
}

// 应用列表API响应格式
export type AppListResponse = UnifiedApiResponse<App[]>

// 应用更新请求接口
export interface AppUpdateRequest {
  appId: string
  secretKey?: string
  encryptionKey?: string
  rateLimit?: number
  timeout?: number
}

// 应用详情API响应格式
export type AppInfoResponse = UnifiedApiResponse<App>

// 统一API响应格式
export interface UnifiedApiResponse<T = any> {
  code: number
  msg: string
  data: T
  timestamp: number
}

// 兼容老版本的响应格式
export interface ApiResponse<T = any> {
  success: boolean
  message: string
  data: T
}

// 供应商列表API响应格式 - 使用统一格式
export type SupplierListResponse = UnifiedApiResponse<SupplierConfig[]>

// 单个供应商健康状态
export interface SupplierHealth {
  supplierName: string
  healthy: boolean
}

// 供应商健康检查数据结构
export interface SupplierHealthData {
  suppliers: SupplierHealth[]
  totalCount: number
  healthyCount: number
  unhealthyCount: number
  timestamp: number
}

// 供应商健康检查响应格式 - 使用统一格式
export type SupplierHealthResponse = UnifiedApiResponse<SupplierHealthData>

// 分页响应格式
export interface PageResponse<T = any> {
  content: T[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
  hasNext: boolean
  hasPrevious: boolean
}

// 供应商配置接口
export interface SupplierConfig {
  id: number
  supplierName: string
  supplierCode: string
  apiBaseUrl: string
  authType: string
  authConfig: string
  ftpConfig: string
  timeoutMs: number
  retryCount: number
  maxConcurrentRequests: number
  rateLimitPerSecond: number
  isActive: boolean
  isSyncStatic: boolean
  isSyncHotel: boolean
  priority: number
  description: string
  contactInfo: string
  supportedCountries: string
  supportedCities: string
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

// 日志查询参数接口
export interface LogQueryParams {
  appId?: string
  supplierCode?: string
  traceId?: string
  hotelKey?: string
  businessType?: string
  distributionOrdersKey?: string
  supplierBookingKey?: string
  checkInKey?: string
  checkOutKey?: string
  supplierId?: string
  isSuccess?: boolean
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

// 分销商订单日志查询参数接口
export interface DistributionOrdersLogQueryParams {
  appId?: string
  supplierId?: string
  supplierCode?: string
  traceId?: string
  businessType?: string
  isSuccess?: boolean
  hotelKey?: string
  distributionOrdersKey?: string
  checkInKey?: string
  checkOutKey?: string
  supplierBookingKey?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

// 分销商调用日志查询参数接口
export interface DistributionCallLogQueryParams {
  appId?: string
  supplierId?: string
  supplierCode?: string
  traceId?: string
  businessType?: string
  isSuccess?: boolean
  hotelKey?: string
  checkInKey?: string
  checkOutKey?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

// API调用日志查询参数接口
export interface ApiCallLogQueryParams {
  appId?: string
  supplierId?: string
  supplierCode?: string
  traceId?: string
  isSuccess?: boolean
  hotelKey?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

// 同步日志查询参数接口
export interface SyncLogQueryParams {
  appId?: string
  supplierId?: string
  supplierCode?: string
  isSuccess?: boolean
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

// API调用日志接口
export interface ApiCallLog {
  id: number
  supplierId: number
  traceId?: string
  apiEndpoint: string
  httpMethod: string
  requestHeaders?: string
  requestParams?: string
  requestBody: string
  responseHeaders: string
  responseBody: string
  responseStatus: number
  responseTimeMs: number
  errorCode?: string
  errorMessage?: string
  retryCount: number
  isSuccess: boolean
  businessType: string
  channel: string
  clientIp?: string
  userAgent?: string
  appId?: string
  userId?: string
  sessionId?: string
  requestSizeBytes: number
  responseSizeBytes: number
  requestBodyCompressed: boolean
  responseBodyCompressed: boolean
  createdAt: string
}

// 分销商调用日志接口
export interface DistributionCallLog {
  id: number
  appId: string
  supplierId: number
  supplierCode: string
  traceId: string
  apiEndpoint: string
  httpMethod: string
  requestHeaders: string
  requestParams: string
  requestBody: string
  requestBodyCompressed: boolean
  responseBodyCompressed: boolean
  responseHeaders?: string
  responseBody: string
  responseStatus: number
  responseTimeMs: number
  errorCode?: string
  errorMessage?: string
  isSuccess: boolean
  businessType: string
  hotelKey: string
  checkInKey: string
  checkOutKey: string
  distributionOrdersKey?: string
  clientIp: string
  userAgent: string
  createdAt: string
}

// 分销商订单日志接口
export interface DistributionOrdersLog {
  id: number
  appId: string
  supplierId: number
  supplierCode: string
  traceId: string
  hotelKey: string
  checkInKey: string
  checkOutKey: string
  roomKey: string
  rateKey: string
  nights: number
  guests: number
  rooms: number
  occupancy: string
  currency: string
  national?: string
  totalAmount: number
  distributionOrdersKey: string
  supplierBookingKey?: string
  bookingStatus?: string
  errorCode?: string
  errorMessage?: string
  isSuccess: boolean
  businessType: string
  originalRequest: string
  originalResponse: string
  requestBodyCompressed: boolean
  responseBodyCompressed: boolean
  createdAt: string
}

// 同步日志接口
export interface SyncLog {
  id: number
  supplierId: number
  supplierCode: string
  businessType: string
  fileName: string
  startTime: string
  endTime: string
  totalCount: number
  successCount: number
  skipCount: number
  errorCount: number
  isSuccess: boolean
  message?: string
  createdAt: string
}
