# 环境配置说明

本项目支持多环境配置，可以通过环境变量灵活配置不同环境的参数。

## 环境配置文件

- `.env.development` - 开发环境配置
- `.env.production` - 生产环境配置  
- `.env.example` - 配置模板文件
- `.env.local` - 当前激活的环境配置（由切换脚本自动生成）

## 配置变量

| 变量名 | 描述 | 开发环境值 | 生产环境值 |
|--------|------|------------|------------|
| `NODE_ENV` | 环境类型 | `development` | `production` |
| `PORT` | 应用端口 | `9091` | `9091` |
| `NEXT_PUBLIC_API_BASE_URL` | API 基础 URL | `http://localhost:9090` | `http://47.76.191.223:9090` |
| `NEXT_PUBLIC_APP_ENV` | 应用环境标识 | `development` | `production` |

## 使用方法

### 1. 快速切换环境

使用环境切换脚本：

```bash
# 切换到开发环境
./set-env.sh dev

# 切换到生产环境  
./set-env.sh prod
```

### 2. 手动配置

复制对应的环境配置文件为 `.env.local`：

```bash
# 开发环境
cp .env.development .env.local

# 生产环境
cp .env.production .env.local
```

### 3. 启动应用

```bash
# 开发服务器
pnpm dev

# 生产服务器
pnpm prod

# 构建
pnpm build
```

## API 客户端配置

API 客户端会根据环境变量自动选择对应的后端地址：

- **开发环境**: `http://localhost:9090`
- **生产环境**: `http://47.76.191.223:9090`

## 注意事项

1. **环境变量前缀**: 客户端可访问的环境变量必须以 `NEXT_PUBLIC_` 开头
2. **重启服务**: 修改环境配置后需要重启开发服务器
3. **安全性**: 生产环境的敏感配置不要提交到代码仓库
4. **默认值**: 代码中包含了默认值，即使环境变量缺失也能正常运行

## 故障排除

如果遇到 API 请求问题，请检查：

1. 环境配置是否正确加载
2. API 服务器是否正常运行
3. 网络连接是否正常
4. 防火墙或代理设置