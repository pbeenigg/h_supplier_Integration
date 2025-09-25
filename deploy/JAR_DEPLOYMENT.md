# HeyTrip Supplier Integration JAR包一键部署指南

## 概述

本文档提供了 HeyTrip 酒店供应商集成服务的 JAR包直接部署方案，包含Docker镜像版本管理功能，无需复杂的环境变量配置和额外的服务依赖，适合快速部署到服务器。

## 系统要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 2GB 可用内存
- 至少 5GB 可用磁盘空间

## 快速开始

### 1. 准备JAR包

首先构建项目生成JAR包：

```bash
# 在项目根目录执行
mvn clean package -DskipTests

# JAR包将生成在 target/ 目录下
ls target/*.jar
```

### 2. 上传到服务器

将以下文件上传到服务器：

```
部署文件清单:
├── deploy-jar.sh                 # 一键部署脚本
├── version-manager.sh             # Docker镜像版本管理脚本
├── docker-compose.yml            # Docker Compose配置
├── Dockerfile                    # JAR包专用Dockerfile
├── nginx/nginx.conf              # Nginx配置
└── your-app.jar                  # 应用JAR包
```

### 3. 一键部署

```bash
# 基础部署（使用自动生成的版本标签）
./deploy-jar.sh target/heytrip-supplier-1.0.0-SNAPSHOT.jar

# 指定版本标签部署
./deploy-jar.sh target/heytrip-supplier-1.0.0-SNAPSHOT.jar --tag v1.0.0

# 带Nginx反向代理部署
./deploy-jar.sh your-app.jar --with-nginx --tag production

# 指定端口部署
./deploy-jar.sh your-app.jar --port 8080 --tag dev

# 清理旧部署后重新部署
./deploy-jar.sh your-app.jar --clean --tag latest
```

### 4. 验证部署

部署完成后，访问以下地址验证服务：

- **应用服务**: http://localhost:9090
- **健康检查**: http://localhost:9090/actuator/health
- **API文档**: http://localhost:9090/swagger-ui.html
- **Nginx代理**: http://localhost:80 (如果启用)

## 部署选项

### 命令行参数

| 参数 | 说明 | 示例 |
|------|------|------|
| `jar文件路径` | 要部署的JAR包路径（必需） | `app.jar` |
| `--with-nginx` | 同时启动Nginx反向代理 | `--with-nginx` |
| `--port PORT` | 指定应用端口（默认9090） | `--port 8080` |
| `--tag TAG` | 指定Docker镜像标签（默认自动生成） | `--tag v1.0.0` |
| `--clean` | 清理旧的容器和镜像 | `--clean` |
| `--help` | 显示帮助信息 | `--help` |

### 部署示例

```bash
# 示例1: 基础部署（自动生成版本标签）
./deploy-jar.sh target/heytrip-supplier-1.0.0-SNAPSHOT.jar

# 示例2: 开发环境部署
./deploy-jar.sh app.jar --tag dev --clean

# 示例3: 测试环境部署
./deploy-jar.sh app.jar --tag test-20250924 --port 8080

# 示例4: 生产环境部署
./deploy-jar.sh production-app.jar --tag v1.0.0 --with-nginx --clean

# 示例5: 热修复版本部署
./deploy-jar.sh hotfix-app.jar --tag v1.0.1-hotfix
```

## Docker镜像版本管理

### 1. 镜像命名规范

部署脚本会自动生成规范的镜像名称和标签：

- **镜像名称**: `heytrip/supplier-integration`
- **自动标签**: `{版本号}-{时间戳}` (例如: `1.0.0-SNAPSHOT-20250924-104500`)
- **自定义标签**: 通过 `--tag` 参数指定

### 2. 版本管理工具

使用 `version-manager.sh` 脚本管理镜像版本：

```bash
# 列出所有镜像版本
./version-manager.sh list

# 为现有镜像添加新标签
./version-manager.sh tag 1.0.0-SNAPSHOT-20250924-104500 v1.0.0

# 清理旧版本（保留最新3个）
./version-manager.sh clean

# 清理旧版本（保留最新5个）
./version-manager.sh clean --keep 5

# 显示镜像详细信息
./version-manager.sh info latest

# 显示构建历史
./version-manager.sh history

# 清理悬空镜像
./version-manager.sh prune
```

### 3. 版本管理最佳实践

#### 开发环境
```bash
# 使用开发标签
./deploy-jar.sh app.jar --tag dev --clean
```

#### 测试环境
```bash
# 使用测试标签
./deploy-jar.sh app.jar --tag test-$(date +%Y%m%d)
```

#### 生产环境
```bash
# 使用语义化版本号
./deploy-jar.sh app.jar --tag v1.2.3
./version-manager.sh tag v1.2.3 latest
```

#### 版本回滚
```bash
# 查看历史版本
./version-manager.sh history

# 回滚到指定版本
docker compose -f docker-compose.yml down
export IMAGE_TAG=v1.2.2
docker compose -f docker-compose.yml up -d
```

## 配置说明

### 应用配置

应用使用以下默认配置（在 `docker-compose.yml` 中）：

```yaml
environment:
  # Spring Boot配置
  SPRING_PROFILES_ACTIVE: prod
  SERVER_PORT: 9090
  
  # 数据库配置（使用外部数据库）
  DB_URL: jdbc:p6spy:mysql://...
  DB_USERNAME: xwd_pax_0389516
  DB_PASSWORD: lJfGo#Kgj$6H29y!1q&0SpDf4G*oi
  
  # 应用配置
  APP_ID: heytrip_supplier_integration_pax
  APP_SECRET: HeyTrip@Pax#SupplierIntegration!2025
  
  # JVM配置
  JAVA_OPTS: -Xmx1g -Xms512m -XX:+UseG1GC
```

### 自定义配置

如需修改配置，编辑 `docker-compose.simple.yml` 文件：

```bash
# 编辑配置文件
vim docker-compose.yml

# 重新部署
./deploy-jar.sh your-app.jar --clean
```

## 服务管理

### 常用命令

```bash
# 查看服务状态
docker compose -f docker-compose.yml ps

# 查看日志
docker compose -f docker-compose.yml logs -f

# 查看应用日志
docker compose -f docker-compose.yml logs -f heytrip-supplier

# 查看镜像信息
./version-manager.sh list

# 停止服务
docker compose -f docker-compose.yml stop

# 启动服务
docker compose -f docker-compose.yml start

# 重启服务
docker compose -f docker-compose.yml restart

# 停止并删除容器
docker compose -f docker-compose.yml down
```

### 服务监控

```bash
# 查看容器资源使用
docker stats

# 进入容器
docker compose -f docker-compose.yml exec heytrip-supplier bash

# 查看应用进程
docker compose -f docker-compose.yml exec heytrip-supplier ps aux

# 查看镜像大小和统计
./version-manager.sh list
```

## 故障排查

### 常见问题

1. **JAR包启动失败**
   ```bash
   # 查看详细日志
   docker compose -f docker-compose.yml logs heytrip-supplier
   
   # 检查JAR包是否完整
   ls -la app.jar
   
   # 检查镜像构建状态
   ./version-manager.sh info latest
   ```

2. **端口冲突**
   ```bash
   # 检查端口占用
   netstat -tulpn | grep :9090
   
   # 使用其他端口部署
   ./deploy-jar.sh app.jar --port 9090
   ```

3. **内存不足**
   ```bash
   # 检查系统内存
   free -h
   
   # 调整JVM内存配置（编辑docker-compose.simple.yml）
   JAVA_OPTS: -Xmx512m -Xms256m -XX:+UseG1GC
   ```

4. **数据库连接失败**
   ```bash
   # 检查数据库连接
   telnet db-host 3306
   
   # 查看数据库相关日志
   docker compose -f docker-compose.yml logs | grep -i database
   ```

### 性能调优

1. **JVM调优**
   ```yaml
   # 在docker-compose.yml中调整
   JAVA_OPTS: -Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
   ```

2. **容器资源限制**
   ```yaml
   # 调整内存和CPU限制
   mem_limit: 2g
   cpus: 2.0
   ```

## 生产环境建议

### 安全配置

1. **修改默认密码和密钥**
   ```yaml
   APP_SECRET: your-production-secret
   ENCRYPTION_KEY: your-production-encryption-key
   ```

2. **数据库安全**
   - 使用生产数据库连接
   - 配置SSL连接
   - 使用强密码

3. **网络安全**
   - 配置防火墙规则
   - 使用HTTPS（配置SSL证书）
   - 限制管理端点访问

### 监控和日志

1. **日志管理**
   ```bash
   # 配置日志轮转
   docker run --log-driver=json-file --log-opt max-size=10m --log-opt max-file=3
   ```

2. **健康检查**
   ```bash
   # 定期检查应用健康状态
   curl -f http://localhost:9090/actuator/health
   ```

### 备份策略

1. **应用备份**
   ```bash
   # 备份JAR包
   cp app.jar backup/app-$(date +%Y%m%d).jar
   
   # 备份配置
   cp docker-compose.yml backup/
   ```

2. **日志备份**
   ```bash
   # 定期备份日志
   tar -czf logs-$(date +%Y%m%d).tar.gz logs/
   ```

## 版本升级

### 应用升级流程

```bash
# 1. 查看当前版本
./version-manager.sh list

# 2. 部署新版本（保留旧版本）
./deploy-jar.sh new-version.jar --tag v1.1.0

# 3. 验证新版本
curl -f http://localhost:9090/actuator/health

# 4. 标记为最新版本（可选）
./version-manager.sh tag v1.1.0 latest

# 5. 清理旧版本（保留最新3个）
./version-manager.sh clean --keep 3
```

### 回滚方案

```bash
# 1. 查看可用版本
./version-manager.sh history

# 2. 快速回滚到上一个版本
docker compose -f docker-compose.yml down
export IMAGE_TAG=v1.0.9  # 指定要回滚的版本
docker compose -f docker-compose.yml up -d

# 3. 验证回滚结果
curl -f http://localhost:9090/actuator/health

# 4. 更新latest标签（可选）
./version-manager.sh tag v1.0.9 latest
```

## 优势特点

✅ **简单易用** - 一条命令完成部署  
✅ **无需配置** - 内置生产级配置  
✅ **快速部署** - 直接使用JAR包，无需编译  
✅ **资源节省** - 不依赖Redis等额外服务  
✅ **易于维护** - 简化的服务架构  
✅ **灵活扩展** - 可选择是否启用Nginx代理  

## 支持

如遇到问题，请：

1. 查看部署日志：`docker-compose -f docker-compose.simple.yml logs -f`
2. 检查系统资源：`docker stats`
3. 验证JAR包完整性：`java -jar app.jar --version`
4. 联系开发团队获取技术支持

---

**作者**: Pax  
**版本**: 1.0.0  
**更新日期**: 2025-09-22
