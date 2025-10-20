# HeyTrip Supplier Integration - 前后端一键部署指南

## 概述

本文档介绍了HeyTrip供应商集成系统的Docker一键部署功能，支持前端Web、后端JAR包的独立或联合部署。

**🔗 Nginx代理集成**: 系统已支持与生产环境Nginx代理的无缝集成，详见 [NGINX_INTEGRATION.md](./NGINX_INTEGRATION.md)。

## 架构说明

### 后端服务 (heytrip-supplier)
- **技术栈**: Spring Boot 3.2 + Java 17
- **端口**: 9090 (默认)
- **功能**: API服务、供应商集成、数据处理
- **镜像**: `heytrip/supplier`

### 前端服务 (heytrip-web)
- **技术栈**: Next.js 14 + TypeScript + Tailwind CSS
- **端口**: 9091 (默认)
- **功能**: 管理界面、日志查看、系统监控
- **镜像**: `heytrip/web`
- **代理**: Nginx配置API代理到后端服务

## 部署类型

### 1. 后端部署 (backend)
仅部署后端JAR包服务

```bash
# 本地部署
./deploy-jar.sh target/heytrip-supplier-1.0.0-SNAPSHOT.jar

# 远程部署
./upload-and-deploy.sh --key ~/.ssh/id_rsa --deploy-type backend
```

### 2. 前端部署 (web)
仅部署前端Web应用

```bash
# 本地部署
./deploy-jar.sh dummy.jar --type web --web-port 9091

# 远程部署
./upload-and-deploy.sh --key ~/.ssh/id_rsa --deploy-type web
```

### 3. 完整部署 (full)
同时部署前后端应用

```bash
# 本地部署
./deploy-jar.sh target/heytrip-supplier-1.0.0-SNAPSHOT.jar --type full --clean

# 远程部署
./upload-and-deploy.sh --key ~/.ssh/id_rsa --deploy-type full
```

## 配置文件

### 1. deploy/upload-and-deploy.env
```bash
UPLOAD_HOST=your-server-host
UPLOAD_USER=root
UPLOAD_PORT=22
UPLOAD_REMOTE_DIR=/root/pax/
DEPLOY_TYPE=full  # backend|web|full
```

### 2. deploy/docker-compose.yml
使用Docker Compose profiles进行服务管理：
- `backend`: 后端服务
- `web`: 前端服务  
- `full`: 完整应用

## 部署流程

### 远程部署流程 (upload-and-deploy.sh)

1. **构建阶段**
   - 后端: Maven构建JAR包
   - 前端: pnpm构建静态文件

2. **上传阶段**
   - 创建远程目录结构
   - 上传JAR包 (backend/full)
   - 上传前端源码 (web/full)
   - 同步部署脚本

3. **部署阶段**
   - 传输部署参数
   - 执行远程部署脚本
   - 启动对应服务

### 本地部署流程 (deploy-jar.sh)

1. **前期检查**
   - Docker环境检查
   - 文件完整性验证
   - 部署类型验证

2. **镜像构建**
   - 后端: Spring Boot应用镜像
   - 前端: 多阶段构建 (Node.js + Nginx)

3. **服务启动**
   - 使用Docker Compose启动服务
   - 健康检查验证
   - 状态监控

## 服务访问

### 后端服务端点
```
应用服务: http://localhost:9090
健康检查: http://localhost:9090/actuator/health
API文档: http://localhost:9090/swagger-ui.html
```

### 前端服务端点
```
Web应用: http://localhost:9091
API代理: http://localhost:9091/api/* -> http://heytrip-supplier:9090/*
健康检查代理: http://localhost:9091/actuator/* -> http://heytrip-supplier:9090/actuator/*
```

### 完整应用端点
```
前端界面: http://localhost:9091 (推荐入口)
后端API: http://localhost:9090 (直接访问)
```

## 管理命令

### 日志查看
```bash
# 后端日志
docker compose -f docker-compose.yml --profile backend logs -f

# 前端日志
docker compose -f docker-compose.yml --profile web logs -f

# 完整应用日志
docker compose -f docker-compose.yml --profile full logs -f
```

### 服务控制
```bash
# 停止服务
docker compose -f docker-compose.yml --profile [backend|web|full] stop

# 启动服务
docker compose -f docker-compose.yml --profile [backend|web|full] start

# 删除服务
docker compose -f docker-compose.yml --profile [backend|web|full] down
```

### 镜像管理
```bash
# 查看镜像
docker images --filter "reference=heytrip/*"

# 清理旧镜像
docker image prune -f

# 重新构建
./deploy-jar.sh <jar-file> --type <type> --clean
```

## 高级配置

### 自定义端口
```bash
# 后端自定义端口
./deploy-jar.sh app.jar --port 8080

# 前端自定义端口
./deploy-jar.sh app.jar --type web --web-port 8081

# 完整应用自定义端口
./deploy-jar.sh app.jar --type full --port 8080 --web-port 8081
```

### 自定义镜像标签
```bash
./deploy-jar.sh app.jar --tag v1.0.0 --type full
```

### 环境变量配置
在`docker-compose.yml`中可以配置：
- `HOST_PORT`: 后端服务主机端口
- `WEB_HOST_PORT`: 前端服务主机端口  
- `IMAGE_TAG`: 后端镜像标签
- `WEB_IMAGE_TAG`: 前端镜像标签

## 故障排查

### 常见问题

1. **前端构建失败**
   ```bash
   # 检查Node.js和pnpm版本
   node --version  # 需要 >= 18
   pnpm --version  # 需要 >= 8
   ```

2. **容器启动失败**
   ```bash
   # 查看容器状态
   docker ps -a
   
   # 查看容器日志
   docker logs <container-name>
   ```

3. **端口冲突**
   ```bash
   # 检查端口占用
   netstat -tulpn | grep :9090
   
   # 使用自定义端口
   ./deploy-jar.sh app.jar --port 8080
   ```

4. **网络连接问题**
   ```bash
   # 检查Docker网络
   docker network ls
   docker network inspect heytrip_backend
   ```

### 健康检查

```bash
# 后端健康检查
curl http://localhost:9090/actuator/health

# 前端健康检查  
curl http://localhost:9091/

# 通过前端代理访问后端
curl http://localhost:9091/api/actuator/health
```

## 版本管理

### 镜像版本策略
- 开发环境: `latest`
- 测试环境: `dev-YYYYMMDD-HHMMSS`
- 生产环境: `v1.0.0` (语义化版本)

### 回滚策略
```bash
# 停止当前服务
docker compose -f docker-compose.yml --profile full down

# 使用旧版本镜像
export IMAGE_TAG=v1.0.0
export WEB_IMAGE_TAG=web-v1.0.0

# 启动服务
docker compose -f docker-compose.yml --profile full up -d
```

## 性能优化

### 构建优化
- 前端使用多阶段构建，减少镜像大小
- 启用Docker构建缓存
- 使用pnpm提高依赖安装速度

### 运行时优化
- Nginx静态文件缓存配置
- Gzip压缩优化
- JVM内存参数调优

### 监控建议
- 使用Actuator端点监控应用健康状态
- 配置日志聚合和分析
- 设置告警机制

---

**维护**: HeyTrip开发团队  
**版本**: 1.0.0  
**更新**: 2025年10月17日