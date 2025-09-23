# HeyTrip Supplier Integration JAR包一键部署指南

## 概述

本文档提供了 HeyTrip 酒店供应商集成服务的 JAR包直接部署方案，无需复杂的环境变量配置和额外的服务依赖，适合快速部署到服务器。

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
├── docker-compose.yml            #Docker Compose配置
├── Dockerfile                    #JAR包专用Dockerfile
├── nginx/simple.conf             # 简化版Nginx配置
└── your-app.jar                  #应用JAR包
```

### 3. 一键部署

```bash
# 基础部署（仅应用服务）
./deploy-jar.sh target/heytrip-supplier-1.0.0-SNAPSHOT.jar

# 带Nginx反向代理部署
./deploy-jar.sh your-app.jar --with-nginx

# 指定端口部署
./deploy-jar.sh your-app.jar --port 9090

# 清理旧部署后重新部署
./deploy-jar.sh your-app.jar --clean
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
| `--port PORT` | 指定应用端口（默认9090） | `--port 9090` |
| `--clean` | 清理旧的容器和镜像 | `--clean` |
| `--help` | 显示帮助信息 | `--help` |

### 部署示例

```bash
# 示例1: 基础部署
./deploy-jar.sh target/heytrip-supplier-1.0.0-SNAPSHOT.jar

# 示例2: 带Nginx的生产部署
./deploy-jar.sh app.jar --with-nginx --clean

# 示例3: 自定义端口部署
./deploy-jar.sh app.jar --port 9090

# 示例4: 完整生产部署
./deploy-jar.sh production-app.jar --with-nginx --port 9090 --clean
```

## 配置说明

### 应用配置

应用使用以下默认配置（在 `docker-compose.simple.yml` 中）：

```yaml
environment:
  # Spring Boot配置
  SPRING_PROFILES_ACTIVE: prod
  SERVER_PORT: 9090
  
  # 数据库配置（使用外部数据库）
  SPRING_DATASOURCE_URL: jdbc:p6spy:mysql://...
  SPRING_DATASOURCE_USERNAME: xwd_pax_0389516
  SPRING_DATASOURCE_PASSWORD: lJfGo#Kgj$6H29y!1q&0SpDf4G*oi
  
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
docker-compose -f docker-compose.yml ps

# 查看日志
docker-compose -f docker-compose.yml logs -f

# 查看应用日志
docker-compose -f docker-compose.yml logs -f heytrip-supplier

# 停止服务
docker-compose -f docker-compose.yml stop

# 启动服务
docker-compose -f docker-compose.yml start

# 重启服务
docker-compose -f docker-compose.yml restart

# 停止并删除容器
docker-compose -f docker-compose.yml down
```

### 服务监控

```bash
# 查看容器资源使用
docker stats

# 进入容器
docker-compose -f docker-compose.yml exec heytrip-supplier bash

# 查看应用进程
docker-compose -f docker-compose.yml exec heytrip-supplier ps aux
```

## 故障排查

### 常见问题

1. **JAR包启动失败**
   ```bash
   # 查看详细日志
   docker-compose -f docker-compose.yml logs heytrip-supplier
   
   # 检查JAR包是否完整
   ls -la app.jar
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
   docker-compose -f docker-compose.yml logs | grep -i database
   ```

### 性能调优

1. **JVM调优**
   ```yaml
   # 在docker-compose.simple.yml中调整
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
# 1. 备份当前版本
cp app.jar backup/app-old.jar

# 2. 停止服务
docker-compose -f docker-compose.yml stop

# 3. 替换JAR包
cp new-version.jar app.jar

# 4. 重新部署
./deploy-jar.sh app.jar --clean

# 5. 验证新版本
curl -f http://localhost:9090/actuator/health
```

### 回滚方案

```bash
# 如果新版本有问题，快速回滚
docker-compose -f docker-compose.yml stop
cp backup/app-old.jar app.jar
./deploy-jar.sh app.jar
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
