# HeyTrip 酒店供应商集成系统技术文档

## 1. 项目概述

### 1.1 项目简介
HeyTrip 酒店供应商集成系统是一个基于 Spring Boot 3.2.0 的微服务应用，专门用于集成各类酒店供应商的API接口。该系统提供统一的酒店数据管理、价格查询、订单处理等功能，支持多供应商接入和数据标准化处理。

### 1.2 技术栈
- **后端框架**: Spring Boot 3.2.0
- **JDK版本**: Java 17
- **数据库**: MySQL 8.0
- **ORM框架**: Spring Data JPA + Hibernate
- **缓存**: Caffeine (本地缓存)
- **HTTP客户端**: Spring WebFlux (异步)
- **安全框架**: Spring Security
- **API文档**: SpringDoc OpenAPI 3
- **构建工具**: Maven
- **部署**: Docker + Nginx
- **监控**: Spring Boot Actuator
- **SQL监控**: P6Spy
- **FTP支持**: Apache Commons Net

**定位**：为 HeyTrip 平台提供统一、标准化的酒店供应商对接能力，涵盖静态数据同步、报价检索、订单履约、配置管理以及健康监控。

**核心价值**：
- 通过 `SupplierAdapter` 插件式架构，实现多供应商并行接入与能力路由。
- 提供标准化 API，屏蔽各供应商差异，简化上游调用逻辑。
- 内建缓存、重试、异步与调度能力，增强性能与稳定性。
- 完整的部署脚手架（Dockerfile、Docker Compose、Shell 脚本）确保上线效率。

### 1.3 项目结构
```
heytrip-supplier-integration/
├── src/main/java/com/heytrip/hotel/supplier/
│   ├── HotelSupplierApplication.java           # 主启动类
│   ├── adapter/                                # 供应商适配器层
│   │   ├── AbstractSupplierAdapter.java        # 抽象适配器基类
│   │   ├── SupplierAdapter.java                # 适配器接口
│   │   ├── SupplierAdapterManager.java         # 适配器管理器
│   │   ├── impl/                               # 具体供应商实现
│   │   │   └── AsianOverlandAdapter.java       # Asian Overland适配器
│   │   ├── capability/                         # 业务能力接口
│   │   │   ├── PricingBridge.java              # 价格查询能力
│   │   │   ├── OrderBridge.java                # 订单处理能力
│   │   │   └── StaticBridge.java               # 静态数据能力
│   │   ├── service/                            # 适配器服务层
│   │   ├── parser/                             # 数据解析器
│   │   ├── builder/                            # 查询构建器
│   │   └── tasks/                              # 定时任务
│   ├── controller/                             # REST控制器层
│   │   ├── SuppliersController.java            # 供应商管理接口
│   │   ├── PricingController.java              # 价格查询接口
│   │   ├── OrdersController.java               # 订单管理接口
│   │   ├── StaticDataController.java           # 静态数据接口
│   │   ├── CacheController.java                # 缓存管理接口
│   │   ├── ConfigController.java               # 配置管理接口
│   │   ├── MonitorController.java              # 监控接口
│   │   └── CommonController.java               # 通用接口
│   ├── entity/                                 # JPA实体类
│   │   ├── Hotel.java                          # 酒店实体
│   │   ├── Room.java                           # 房型实体
│   │   ├── RatePlan.java                       # 价格计划实体
│   │   ├── City.java                           # 城市实体
│   │   ├── Country.java                        # 国家实体
│   │   ├── HotelGiata.java                     # 酒店Giata映射
│   │   ├── SupplierConfig.java                 # 供应商配置
│   │   └── 其他实体...
│   ├── repository/                             # 数据访问层
│   ├── service/                                # 业务服务层
│   ├── dto/                                    # 数据传输对象
│   ├── config/                                 # 配置类
│   ├── exception/                              # 异常处理
│   ├── utils/                                  # 工具类
│   ├── client/                                 # 外部客户端
│   ├── filter/                                 # 过滤器
│   ├── constant/                               # 常量定义
│   └── enums/                                  # 枚举类
├── src/main/resources/
│   ├── application.yml                         # 主配置文件
│   ├── spy.properties                          # P6Spy配置
│   └── csv/                                    # CSV数据文件
├── docs/                                       # 文档目录
│   ├── sql/                                    # 数据库脚本
│   ├── 需求记录/                               # 需求文档
│   ├── 供应商对接说明文档.md                   # 供应商对接指南
│   ├── HttpClient使用指南.md                   # HTTP客户端使用指南
│   └── supplier-data-standard-使用手册.md      # 数据标准使用手册
└── deploy/                                     # 部署配置
    ├── docker-compose.yml                      # Docker编排文件
    ├── Dockerfile                              # Docker镜像构建文件
    └── nginx/                                  # Nginx配置
```

### 1.4 核心特性
- **多供应商支持**: 通过适配器模式支持多个酒店供应商接入
- **异步处理**: 使用WebFlux实现高性能异步HTTP调用
- **数据标准化**: 集成HeyTrip供应商数据标准接口
- **缓存优化**: 使用Caffeine实现高效本地缓存
- **安全认证**: 基于Spring Security的API安全控制
- **监控运维**: 集成Actuator提供健康检查和指标监控
- **定时同步**: 支持酒店静态数据和价格数据定时同步
- **FTP支持**: 支持通过FTP获取供应商数据文件

### 1.5 业务模块
1. **静态数据管理**: 酒店、房型、城市、国家等基础数据管理
2. **价格查询**: 实时价格查询和缓存管理
3. **订单处理**: 预订、确认、取消等订单生命周期管理
4. **数据同步**: 定时同步供应商数据到本地数据库
5. **配置管理**: 供应商配置、系统配置的动态管理
6. **监控告警**: 系统健康状态监控和API调用统计

## 2. 架构设计

```
┌────────────────────────────┐
│        API 接入层 (REST)   │
│  • controller/*            │
└──────────────┬─────────────┘
               │
┌──────────────▼─────────────┐
│   领域服务层                │
│  • adapter/service/*        │
│  • service/*                │
└──────────────┬─────────────┘
               │
┌──────────────▼─────────────┐
│  供应商适配管理 (Adapter)  │
│  • SupplierAdapterManager   │
│  • adapter/impl/*           │
│  • capability/{Pricing,...} │
└──────────────┬─────────────┘
               │
┌──────────────▼─────────────┐
│       数据访问层            │
│  • repository/*             │
│  • entity/*                 │
│  • resources/sql/*          │
└──────────────┬─────────────┘
               │
┌──────────────▼─────────────┐
│  基础设施                   │
│  • config/*                 │
│  • filter/* (MD5 签名)      │
│  • utils/*                  │
└────────────────────────────┘
```

### 2.1 整体架构
系统采用分层架构设计，从上到下分为：
- **接口层 (Controller)**: 提供REST API接口
- **业务层 (Service)**: 处理业务逻辑
- **适配器层 (Adapter)**: 适配不同供应商接口
- **数据层 (Repository)**: 数据持久化操作
- **基础设施层**: 缓存、配置、工具等

### 2.2 适配器模式
使用适配器模式实现多供应商支持：
- **SupplierAdapter**: 供应商适配器接口
- **AbstractSupplierAdapter**: 抽象适配器基类
- **具体适配器实现**: 如AsianOverlandAdapter
- **能力接口**: PricingBridge、OrderBridge、StaticBridge

### 2.3 数据流转
1. **请求接收**: Controller接收HTTP请求
2. **业务处理**: Service层处理业务逻辑
3. **适配器调用**: 通过适配器调用供应商API
4. **数据转换**: 将供应商数据转换为标准格式
5. **结果返回**: 返回标准化的响应数据

### 2.4 关键设计点
1. **适配器路由**：`SupplierAdapterManager` 在启动时扫描 `SupplierAdapter` 实现，基于 `supplierName` 构建映射并按照 `priority` 排序，实现多供应商优先级调度与健康检查。
2. **能力隔离**：`adapter/capability` 细分 Static/Pricing/Order 三类桥接接口，保证供应商按需实现并保持接口收敛。
3. **响应式健康检查**：健康检查接口基于 Reactor `Mono/Flux` 实现，支持异步超时控制与错误退避。
4. **配置中心**：`ConfigController` 提供系统配置 CRUD、统计分析及 JSON 配置读写 API，配置实体 `SystemConfig` 支持类型和加密标记。
5. **安全链路**：`SecurityFilter` 执行基于 header 的 MD5 签名认证，结合 `Config.security` 动态白名单 + 认证路径列表，实现灵活的访问控制。


---


## 3. 核心功能模块

### 3.1 供应商适配器模块

#### 3.1.1 适配器架构
供应商适配器是系统的核心组件，负责统一不同供应商的API接口。

**核心类：**
- `SupplierAdapter`: 供应商适配器接口，定义标准操作
- `AbstractSupplierAdapter`: 抽象基类，提供通用功能
- `SupplierAdapterManager`: 适配器管理器，负责适配器的注册和调用
- `AsianOverlandAdapter`: Asian Overland供应商的具体实现

**能力接口：**
- `PricingBridge`: 价格查询能力接口
- `OrderBridge`: 订单处理能力接口
- `StaticBridge`: 静态数据获取能力接口


### 3.2 静态数据管理模块

#### 3.2.1 数据实体
- **Hotel**: 酒店基础信息
- **Room**: 房型信息
- **RatePlan**: 价格计划
- **City**: 城市信息
- **Country**: 国家信息
- **HotelGiata**: 酒店Giata ID映射
- **HotelBookable**: 酒店可售列表

#### 3.2.2 数据同步
- **StaticDataSyncScheduler**: 静态数据同步调度器
- **StaticDataSyncService**: 静态数据同步服务
- **StaticDataParser**: 数据解析器接口
- **AOStaticDataParser**: Asian Overland数据解析器

#### 3.2.3 同步流程
1. 定时任务触发数据同步
2. 通过FTP或HTTP获取供应商数据文件
3. 解析CSV/JSON格式数据
4. 数据清洗和标准化
5. 批量更新到数据库
6. 更新缓存

### 3.3 价格查询模块

#### 3.3.1 查询流程
1. 接收价格查询请求
2. 参数验证和预处理
3. 检查缓存是否有有效数据
4. 调用供应商适配器查询实时价格
5. 数据标准化处理
6. 缓存查询结果
7. 返回标准格式响应

#### 3.3.2 缓存策略
- 使用Caffeine本地缓存
- 支持多级缓存配置
- 缓存过期时间可配置
- 支持缓存预热和失效


### 3.4 配置管理模块

#### 3.4.1 配置类型
- **SupplierConfig**: 供应商配置（认证信息、API地址等）
- **SystemConfig**: 系统配置（超时时间、重试次数等）
- **CacheConfig**: 缓存配置

#### 3.4.2 动态配置
支持运行时动态修改配置，无需重启服务：
- 通过REST API修改配置
- 配置变更实时生效
- 配置历史记录和回滚

### 3.5 监控告警模块

#### 3.5.1 健康检查
- **SupplierHealthCheckService**: 供应商健康检查服务
- 定期检查供应商API可用性
- 记录健康检查日志
- 支持告警通知

#### 3.5.2 API调用监控
- **ApiCallLog**: API调用日志记录
- 记录请求响应时间
- 统计成功率和错误率
- 支持性能分析

## 4. 数据传输对象 (DTO)

### 4.1 QTech供应商DTO
- **请求DTO**: QTechSearchRequest、QTechBookingRequest等
- **响应DTO**: QTechSearchResponse、QTechBookingResponse等
- **基础DTO**: QTechBaseResponse提供通用响应结构

### 4.2 标准化DTO
- **XHotelGiata**: 酒店Giata映射DTO
- **XNationality**: 国籍信息DTO
- **SupplierAuth**: 供应商认证DTO
- **SupplierFtp**: 供应商FTP配置DTO

## 5. 工具类和辅助组件

### 5.1 HTTP客户端
- **HttpClientService**: 基于WebFlux的异步HTTP客户端
- 支持重试机制
- 连接池管理
- 超时控制

### 5.2 FTP客户端
- **FtpClientService**: FTP文件传输服务
- 支持文件上传下载
- 连接池管理
- 异常处理

### 5.3 工具类
- **AuthHelper**: 认证辅助工具
- **SignUtil**: 签名工具
- **MD5Util**: MD5加密工具
- **HeyUtil**: 通用工具类
- **CsvStreamReaderUtil**: CSV流式读取工具

### 5.4 安全组件
- **SecurityFilter**: 安全过滤器
- **SecurityConfig**: 安全配置
- 支持API Key认证
- 请求签名验证


## 6. 安全与认证

- **认证模型**：基于自定义 MD5 签名的无状态认证。
- **HTTP 头要求**：
    - `app`：调用方身份，需与 `app.authorization.app-id` 匹配。
    - `timestamp`：Unix 时间戳（秒级），允许最大 ±5 分钟偏差。
    - `sign`：`MD5(appId + timestamp + secretKey)`，`secretKey` 来自配置。
- **白名单路径**：由 `Config.security.permitAllPatterns` 管理，默认放行健康检查、监控、文档与 `/suppliers/**` 等接口。
- **强制认证路径**：`Config.security.authenticatedPatterns`，默认覆盖 `/pax/**`、`/common/**`、`/config/**`。
- **实现细节**：
    - `SecurityFilter` 校验头部并在成功时写入 `SecurityContext`。
    - `AuthHelper` 提供认证信息读取工具，在服务或适配器中判断授权状态。
    - `SecurityConfig` 统一装配 Spring Security 过滤链，并关闭 Session 与 CSRF。

## 7. 配置与环境变量
默认配置位于 `src/main/resources/application.yml`。常用环境变量如下（均支持通过 JVM `-D` 或系统环境覆盖）：

| 变量 | 默认值 | 说明 |
| :-- | :-- | :-- |
| `SERVER_PORT` | `9090` | Spring Boot 端口 |
| `SERVER_PATH` | `/` | Servlet context path |
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring 配置 Profile |
| `DB_URL` | `jdbc:p6spy:mysql://127.0.0.1:3306/supplier_pax` | 数据库连接（默认走 P6Spy 代理） |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `Root@123456` | 数据库凭据 |
| `DB_DRIVER` | `com.p6spy.engine.spy.P6SpyDriver` | JDBC Driver，可改为 `com.mysql.cj.jdbc.Driver` |
| `APP_ID` / `APP_SECRET` | `heytrip_supplier_integration_pax` / `HeyTrip@Pax#SupplierIntegration!2025` | 签名认证信息 |
| `ENCRYPTION_KEY` | `427ae41e4649b934ca495991b7852b855` | 自定义加密密钥 |
| `SWAGGER_UI` / `API_DOCS` | `true` | 是否开启 SpringDoc(API 文档默认路径 `/swagger-ui.html`) |

> 如需对接生产环境，请在启动命令或容器环境中覆写敏感配置，避免使用默认凭据。

## 8. 开发环境准备与运行

### 先决条件

- JDK 17+
- Maven 3.9+
- 本地 MySQL 8 实例，并导入 `docs/sql/basic.sql` 与 `docs/sql/hotel.sql`
- 可选：Docker 20+（用于容器化部署）

### 本地启动

1. 配置数据库并确保应用有权访问。
2. 安装依赖并编译：
   ```bash
   mvn clean install
   ```
3. 使用开发配置启动：
   ```bash
   mvn spring-boot:run
   ```
4. 访问：
    - 健康检查：`http://localhost:9090/actuator/health`
    - API 文档：`http://localhost:9090/swagger-ui.html`

### 调试技巧

- 通过 `application-dev.yml`（可新增）或环境变量覆写调试参数。
- 使用 P6Spy（`spy.properties`）观察 SQL，日志输出位于 `logs/heytrip-supplier.log`。
- Caffeine 缓存配置位于 `spring.cache` 段，可按需调整最大缓存数或过期时间。



## 9. 测试与质量保障

- **单元测试**：`src/test/java/com/heytrip/hotel/supplier` 下提供控制器、服务、适配器测试样例。运行：
  ```bash
  mvn test
  ```
- **集成测试建议**：
    - 针对不同供应商 Mock Adapter，实现端到端 API 测试。
    - 使用 `@DataJpaTest` 校验仓储查询逻辑，特别是自定义 JPQL/Specification。
- **并发/性能验证**：`deploy/api_concurrent_test.sh` 提供针对报价接口的并发压测脚本，可在 Staging 环境评估吞吐与稳定性。




## 10. 部署方案

### 10.1 Docker Compose（推荐）

- 依赖文件：`deploy/docker-compose.yml`、`deploy/Dockerfile`
- 构建镜像并启动：
  ```bash
  cd deploy
  export JAR_FILE_PATH=target/heytrip-supplier-1.0.0-SNAPSHOT.jar
  docker compose build
  docker compose up -d
  ```
- 生产化配置：通过 `.env` 或 CI/CD 注入数据库、认证等敏感信息。
- 可选 `nginx` profile 提供 80 端口反向代理与静态资源托管。

### 10.2 Shell 脚本一键部署

- `deploy/deploy-jar.sh <jar-path> [--with-nginx] [--port 9090] [--tag <version>] [--clean]`
- 功能覆盖：依赖检查、镜像构建、容器启动、健康检查、日志查看提示。

### 10.3 其他部署资产

- `deploy/JAR_DEPLOYMENT.md` 描述了 JAR 包部署最佳实践与注意事项。
- `deploy/version-manager.sh` 帮助维护多环境镜像版本号。

## 11. 扩展指南：新增供应商适配器

1. **定义适配器实现**：在 `adapter/impl/` 下新增类，实现 `SupplierAdapter`，并按需实现 `StaticBridge`、`PricingBridge`、`OrderBridge`。
2. **配置启用条件**：重写 `isEnabled()` 与 `getPriority()` 控制适配器注册顺序与启用状态，可结合数据库配置。
3. **路由标识**：确保 `getSupplierName()` 与上游请求的 `supplierType` 一致，`SupplierAdapterManager` 会自动映射。
4. **健康检查**：实现 `healthCheck()` 返回 `Mono<Boolean>`，用于 `/suppliers/**/health`。
5. **数据持久化**：如需参数化配置，扩展 `SupplierConfig` 或在 `SystemConfig` 中存储 JSON。
6. **测试与文档**：
    - 编写单元测试覆盖新适配器能力。
    - 补充 `docs/供应商对接说明文档.md` 或新增条目，记录供应商特性与验收要点。




**文档版本**: v1.0.0  
**维护团队**: HeyTrip开发团队 Pax