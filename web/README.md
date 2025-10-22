# HeyTrip 供应商集成管理系统 - 前端项目

## 项目概述

HeyTrip 供应商集成管理系统前端是基于 Next.js 14 + TypeScript 开发的现代化后台管理界面，为酒店供应商集成系统提供完整的管理功能。采用前后端分离架构，独立部署，通过 REST API 与后端服务进行数据交互。

## 技术栈

### 核心框架
- **Next.js 14.0.3** - React 全栈框架，支持 SSR/SSG
- **React 18** - 用户界面构建库
- **TypeScript 5** - 类型安全的 JavaScript 超集

### UI 组件与样式
- **Shadcn/ui** - 基于 Radix UI 的高质量组件库
- **Radix UI** - 无障碍访问的底层 UI 组件
- **Tailwind CSS 3.3** - 实用优先的 CSS 框架
- **Lucide React** - 现代化图标库

### 状态管理与数据处理
- **React Query (TanStack Query) 5.8** - 服务器状态管理
- **React Hook Form 7.47** - 高性能表单处理
- **Zod 3.22** - TypeScript 优先的模式验证

### 网络请求与加密
- **Axios 1.6** - HTTP 客户端
- **Crypto-JS 4.2** - 加密算法库（MD5 签名）

### 开发工具
- **ESLint + Prettier** - 代码规范与格式化
- **PostCSS + Autoprefixer** - CSS 处理

## 项目架构

### 目录结构
```
web/
├── src/
│   ├── app/                    # App Router 页面目录
│   │   ├── layout.tsx         # 根布局
│   │   ├── page.tsx           # 首页
│   │   ├── login/             # 登录页面
│   │   ├── dashboard/         # 仪表盘
│   │   ├── users/             # 用户管理
│   │   ├── logs/              # 日志管理
│   │   ├── monitor/           # 监控页面
│   │   └── suppliers/         # 供应商管理
│   ├── components/            # 可复用组件
│   │   ├── ui/                # UI 基础组件
│   │   └── layout/            # 布局组件
│   ├── lib/                   # 工具库
│   │   ├── api-client.ts      # API 请求客户端
│   │   └── utils.ts           # 工具函数
│   ├── types/                 # TypeScript 类型定义
│   │   └── index.ts           # 通用类型
│   └── styles/                # 全局样式
│       └── globals.css        # 全局 CSS
├── public/                    # 静态资源
├── .next/                     # Next.js 构建输出
└── 配置文件...
```

### 核心特性

#### 1. 认证系统
- 基于 Token 的认证机制
- LocalStorage 存储用户凭证
- 自动 Token 刷新
- MD5 签名验证支持

#### 2. API 客户端
- 统一的 Axios 配置
- 自动请求/响应拦截
- 错误处理与重试机制
- 支持 AppId 签名认证和用户 Token 认证

#### 3. 响应式设计
- 基于 Tailwind CSS 的移动端适配
- 灵活的栅格系统
- 现代化的 UI 组件

## 功能模块

### 1. 登录认证 (`/login`)
- 用户名密码登录
- 记住我功能
- 错误提示处理
- 自动跳转到仪表盘

### 2. 用户管理 (`/users`)
- 用户列表展示（表格形式）
- 创建/编辑/删除用户
- 搜索与筛选功能
- 分页显示（每页 20 条，最多 50 条）
- 按创建时间降序排序

### 3. 应用管理 (`/dashboard`)
- 应用列表管理
- 应用配置编辑
- AppId 和密钥管理
- 权限与限流配置

### 4. 缓存管理 (`/cache`)
- 清除全部静态缓存
- 按名称清除指定缓存
- 缓存统计信息展示
- 一键操作按钮

### 5. 系统监控 (`/monitor`)
- 系统健康状态仪表盘
- 性能指标实时展示
- 供应商状态监控
- HttpClient 健康检查
- 系统资源使用情况

### 6. 日志管理 (`/logs`)
- API 调用日志查询
- 分销商调用日志
- 分销商订单日志
- 多维度筛选条件
- 时间范围筛选
- 分页展示

### 7. 供应商管理 (`/suppliers`)
- 供应商列表展示
- 供应商配置管理
- 健康检查功能
- 状态监控

## 部署配置

### 开发环境
```bash
# 开发环境
pnpm run dev       # 开发服务器 (localhost:9091)

# 构建
pnpm run build     # 构建静态文件到 out/

# 预览/生产
pnpm run preview   # 预览构建结果 (localhost:9091) ✅ 已修复
pnpm run prod      # 构建 + 生产服务器 (localhost:9091)
pnpm run start     # 直接启动生产服务器 (localhost:9091)

# 代码质量
pnpm run lint      # ESLint 检查
pnpm run type-check # TypeScript 类型检查
```

### 环境变量
- 开发环境：API 地址 `http://localhost:9090`
- 生产环境：通过 Nginx 反向代理 `/api` 路径

### Docker 部署
```dockerfile
# 多阶段构建
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

FROM node:18-alpine AS runner
WORKDIR /app
COPY --from=builder /app/node_modules ./node_modules
COPY . .
RUN npm run build
EXPOSE 9091
CMD ["npm", "start"]
```

### Nginx 配置
```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态资源
    location / {
        proxy_pass http://localhost:9091;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:9090/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # CORS 头部
        add_header Access-Control-Allow-Origin *;
        add_header Access-Control-Allow-Methods 'GET, POST, PUT, DELETE, OPTIONS';
        add_header Access-Control-Allow-Headers 'Content-Type, Authorization, app, timestamp, sign';
    }
}
```

## API 接口对接

### 认证接口
- `POST /auth/login` - 用户登录
- `GET /auth/user/info` - 获取用户信息
- `POST /auth/user/change-password` - 修改密码
- `POST /auth/logout` - 退出登录

### 管理接口
- `POST /admin/user/create` - 创建用户
- `PUT /admin/user/update` - 更新用户
- `DELETE /admin/user/delete/{userId}` - 删除用户
- `POST /admin/app/create` - 创建应用
- `PUT /admin/app/update` - 更新应用

### 系统接口
- 缓存管理：`GET /cache/*`
- 监控指标：`GET /monitor/*`
- 日志查询：`GET /logs/*`
- 供应商管理：`GET /suppliers/*`

## 开发规范

### 代码规范
- 使用 TypeScript 严格模式
- 遵循 ESLint + Prettier 配置
- 组件采用函数式组件 + Hooks
- 使用 React Query 管理服务器状态

### 命名规范
- 组件：PascalCase（如 `UserManagement`）
- 文件：kebab-case（如 `user-management.tsx`）
- 变量/函数：camelCase（如 `handleSubmit`）
- 常量：UPPER_SNAKE_CASE（如 `API_BASE_URL`）

### 提交规范
- feat: 新功能
- fix: 修复 bug
- docs: 文档更新
- style: 代码格式
- refactor: 重构
- test: 测试相关
- chore: 构建/工具相关

## 性能优化

### 代码分割
- 基于路由的代码分割
- 动态导入组件
- 懒加载非关键资源

### 缓存策略
- React Query 数据缓存
- Next.js 自动静态优化
- 浏览器缓存控制

### 构建优化
- Tree-shaking 去除无用代码
- 压缩 CSS/JS 资源
- 图片优化处理

## 安全考虑

### 认证安全
- Token 自动过期处理
- 安全的本地存储
- CSRF 防护

### 数据传输
- HTTPS 加密传输
- API 签名验证
- 敏感信息脱敏

### 前端安全
- XSS 防护
- 安全的路由配置
- 输入验证与清理

## 浏览器兼容性

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## 版本说明

- **当前版本**：0.1.0
- **Node.js 版本**：18.x LTS
- **包管理器**：pnpm
- **构建工具**：Next.js + Webpack

## 维护与更新

### 依赖更新
```bash
# 检查过时依赖
pnpm outdated

# 更新依赖
pnpm update
```

### 性能监控
- 使用 Next.js 内置分析工具
- 监控包体积变化
- 关注首屏加载时间

---

**维护团队**：HeyTrip 开发团队  
**最后更新**：2025年10月16日