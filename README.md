
# 酒店供应商对接项目

## 1. 项目需求分析

根据您的描述，该项目旨在对接不同酒店供应商的API，并暴露出特定的业务接口供酒店渠道平台系统调用。项目采用单体服务模式，且规模较小，不考虑过于复杂的设计。开发语言为Java。

核心需求点：
*   **API集成**：需要高效、稳定地调用外部供应商的API。
*   **业务接口暴露**：将集成后的数据和功能通过内部接口提供给其他系统。
*   **单体服务**：项目初期或小规模应用，倾向于单体架构，简化部署和管理。
*   **Java语言**：基于Java生态系统进行开发。
*   **小范围使用**：不追求极致的性能或扩展性，更注重开发效率和维护成本。

核心技术栈：
*   **核心框架**：**Spring Boot**
*   **构建工具**：**Apache Maven** 
*   **数据持久层**：**Spring Data JPA** 
*   **HTTP客户端**：Spring Boot自带的**RestTemplate/WebClient**

这套组合能够提供快速的开发效率、简化的部署流程、强大的功能支持以及良好的可维护性，非常适合小范围使用的单体Java应用。



## 4. 框架架构设计方案

以下是针对酒店供应商对接项目的详细架构设计方案。该设计遵循单体应用的原则，同时保持良好的模块化和可扩展性。

### 4.1 整体架构设计

#### 4.1.1 分层架构

采用经典的三层架构模式，确保职责分离和代码的可维护性：

**表现层（Presentation Layer）**
表现层负责处理HTTP请求和响应，是系统对外的接口。在这一层，我们使用Spring Boot的@RestController注解来创建RESTful API端点。这些端点将接收来自酒店渠道平台系统的请求，并返回处理后的数据。

表现层的主要职责包括：
- 接收和验证HTTP请求参数
- 调用业务逻辑层的服务
- 将业务结果转换为HTTP响应
- 处理异常并返回适当的错误信息
- 实现API文档和版本管理

**业务逻辑层（Business Logic Layer）**
业务逻辑层是系统的核心，包含所有的业务规则和处理逻辑。在这一层，我们实现供应商API的集成、数据转换、业务规则验证等功能。

业务逻辑层的主要职责包括：
- 实现具体的业务逻辑和规则
- 协调不同供应商API的调用
- 数据格式转换和标准化
- 缓存管理和性能优化
- 事务管理和数据一致性保证

**数据访问层（Data Access Layer）**
数据访问层负责与数据库和外部API的交互。这一层抽象了数据存储的细节，为业务逻辑层提供统一的数据访问接口。

数据访问层的主要职责包括：
- 数据库操作（CRUD）
- 外部API调用和响应处理
- 数据映射和转换
- 连接池管理和资源优化
- 错误处理和重试机制

#### 4.1.2 模块化设计

为了提高代码的可维护性和可扩展性，将系统划分为以下几个核心模块：

**供应商集成模块（Supplier Integration Module）**
这是系统的核心模块，负责与各个酒店供应商的API进行集成。由于不同供应商的API格式、认证方式、数据结构可能存在差异，该模块需要具备高度的灵活性和可扩展性。

该模块包含以下子组件：
- 供应商适配器（Supplier Adapter）：为每个供应商创建独立的适配器，处理特定供应商的API调用逻辑
- 统一接口层（Unified Interface Layer）：定义标准的内部接口，屏蔽不同供应商的差异
- 认证管理器（Authentication Manager）：处理不同供应商的认证方式（API Key、OAuth、JWT等）
- 数据转换器（Data Transformer）：将供应商返回的数据转换为系统内部的标准格式

**业务服务模块（Business Service Module）**
该模块实现具体的业务逻辑，包括酒店信息查询、房间可用性检查、价格计算、预订处理等功能。

主要组件包括：
- 酒店静态数据服务（Static Data Service）：处理酒店相关的业务逻辑
- 酒店报价类服务（ Hotel Quotation Service）：处理预订相关的业务逻辑
- 酒店订单类服务（Hotel Orders Service）：处理价格计算和比较逻辑
- 其他服务（ Other Service）：处理房间库存管理逻辑

**配置管理模块（Configuration Management Module）**
该模块负责管理系统的配置信息，包括供应商API的配置、业务规则配置、系统参数配置等。

主要功能包括：
- 供应商配置管理：API端点、认证信息、超时设置等
- 业务规则配置：价格策略、库存阈值、预订规则等
- 系统参数配置：缓存设置、日志级别、性能参数等
- 动态配置更新：支持运行时配置更新，无需重启系统

**监控和日志模块（Monitoring and Logging Module）**
该模块负责系统的监控、日志记录和性能分析，确保系统的稳定运行和问题的快速定位。

主要功能包括：
- API调用监控：记录每次API调用的响应时间、成功率、错误信息等
- 业务指标监控：预订成功率、平均响应时间、并发用户数等
- 系统资源监控：CPU使用率、内存使用率、数据库连接数等
- 日志管理：结构化日志记录、日志级别控制、日志轮转等

### 4.2 技术架构详细设计

#### 4.2.1 Spring Boot应用结构

基于Spring Boot的标准项目结构，建议采用以下目录组织方式：

```
src/main/java/com/heytrip/hotel/supplier/
├── HotelSupplierApplication.java          # Spring Boot启动类
├── config/                                # 配置类
│   ├── WebConfig.java                     # Web相关配置
│   ├── DatabaseConfig.java               # 数据库配置
│   ├── RestTemplateConfig.java           # HTTP客户端配置
│   └── CacheConfig.java                  # 缓存配置
├── controller/                            # 控制器层
│   ├── StaticDataController.java          # 酒店静态数据相关API
│   ├── HotelQuotationController.java      # 酒店报价相关API
│   ├── HotelOrdersController.java         # 酒店订单相关API
│   ├── CommonController.java              # 通用相关API
│   └── HealthController.java              # 健康检查API
├── service/                               # 业务服务层
│   ├── StaticDataService.java              # 酒店静态数据服务接口
│   ├── impl/StaticDataServiceImpl.java     # 酒店静态数据服务实现
│   ├── HotelQuotationService.java           # 酒店报价服务接口
│   └── impl/HotelQuotationServiceImpl.java  # 酒店报价服务实现
├── integration/                           # 供应商集成层
│   ├── supplier/                          # 供应商适配器
│   │   ├── SupplierAdapter.java          # 供应商适配器接口
│   │   ├── BookingComAdapter.java        # Booking.com适配器
│   │   ├── ExpediaAdapter.java           # Expedia适配器
│   │   └── AgodaAdapter.java             # Agoda适配器
│   ├── client/                            # HTTP客户端
│   │   ├── SupplierClient.java           # 供应商客户端接口
│   │   └── impl/RestTemplateClient.java  # RestTemplate实现
│   └── transformer/                       # 数据转换器
│       ├── DataTransformer.java          # 数据转换器接口
│       └── impl/HotelDataTransformer.java # 酒店数据转换器
├── repository/                            # 数据访问层
│   ├── StaticDataRepository.java          # 酒店静态数据访问接口
│   ├── HotelQuotationRepository.java      # 酒店报价数据访问接口
│   ├── CommonRepository.java              # 酒店通用数据访问接口
│   ├── HotelOrdersRepository.java         # 酒店订单数据访问接口
│   └── ConfigRepository.java              # 配置数据访问接口
├── entity/                                # 实体类
│   ├── Hotel.java                        # 酒店实体
│   ├── Room.java                         # 酒店房间实体
│   ├── Booked.java                       # 酒店预订实体
│   └── SupplierConfig.java               # 供应商配置实体
├── dto/                                   # 数据传输对象
│   ├── request/                           # 请求DTO
│   │   ├── HotelSearchRequest.java       # 酒店搜索请求
│   │   ├── GetQuoteRequest.java          # 获取酒店报价请求
│   │   ├── CreateOrderRequest.java       # 创建酒店订单请求
│   │   └── BookedRequest.java            # 酒店预订请求
│   └── response/                          # 响应DTO
│       ├── HotelSearchResponse.java       # 酒店搜索响应
│       ├── GetQuoteResponse.java          # 获取酒店报价响应
│       ├── CreateOrderResponse.java       # 创建酒店订单响应
│       └── BookedResponse.java            # 酒店预订响应
├── exception/                             # 异常处理
│   ├── GlobalExceptionHandler.java       # 全局异常处理器
│   ├── SupplierException.java            # 供应商异常
│   └── BusinessException.java            # 业务异常
└── util/                                  # 工具类
    ├── DateUtil.java                     # 日期工具类
    ├── JsonUtil.java                     # JSON工具类
    └── ValidationUtil.java               # 验证工具类
```

#### 4.2.2 数据库设计

考虑到项目的单体特性和小范围使用，建议采用关系型数据库（如MySQL或PostgreSQL）。数据库设计应该支持以下核心功能：

**核心表结构设计：**

**供应商配置表（supplier_config）**
该表存储各个供应商的配置信息，包括API端点、认证信息、超时设置等。

```sql
CREATE TABLE supplier_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_name VARCHAR(100) NOT NULL UNIQUE,
    api_base_url VARCHAR(500) NOT NULL,
    auth_type VARCHAR(50) NOT NULL,
    auth_config TEXT,
    timeout_seconds INT DEFAULT 30,
    retry_count INT DEFAULT 3,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**酒店信息表（hotel_info）**
该表存储从各个供应商同步的酒店基础信息。

```sql
CREATE TABLE hotel_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    supplier_hotel_id VARCHAR(100) NOT NULL,
    hotel_name VARCHAR(200) NOT NULL,
    hotel_address TEXT,
    city VARCHAR(100),
    country VARCHAR(100),
    star_rating DECIMAL(2,1),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    description TEXT,
    amenities JSON,
    images JSON,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES supplier_config(id),
    UNIQUE KEY uk_supplier_hotel (supplier_id, supplier_hotel_id)
);
```

**预订记录表（booked_record）**
该表存储预订相关的信息，用于跟踪和管理预订状态。

```sql
CREATE TABLE booking_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_reference VARCHAR(100) NOT NULL UNIQUE,
    supplier_id BIGINT NOT NULL,
    supplier_booking_id VARCHAR(100),
    hotel_id BIGINT NOT NULL,
    guest_name VARCHAR(100) NOT NULL,
    guest_email VARCHAR(200),
    guest_phone VARCHAR(50),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    room_type VARCHAR(100),
    room_count INT NOT NULL,
    guest_count INT NOT NULL,
    total_amount DECIMAL(10,2),
    currency VARCHAR(10),
    booking_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES supplier_config(id),
    FOREIGN KEY (hotel_id) REFERENCES hotel_info(id)
);
```

**API调用日志表（api_call_log）**
该表记录所有对外部供应商API的调用情况，用于监控和问题排查。

```sql
CREATE TABLE api_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    api_endpoint VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_data TEXT,
    response_data TEXT,
    response_status INT,
    response_time_ms INT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES supplier_config(id),
    INDEX idx_supplier_created (supplier_id, created_at),
    INDEX idx_created_at (created_at)
);
```

#### 4.2.3 缓存策略设计

为了提高系统性能和减少对供应商API的频繁调用，建议实施多层缓存策略：

**本地缓存（Local Cache）**
使用Spring Boot集成的Caffeine缓存，用于缓存频繁访问的数据，如酒店基础信息、供应商配置等。本地缓存具有最快的访问速度，但仅限于单个应用实例。

配置示例：
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats());
        return cacheManager;
    }
}
```

**分布式缓存（Distributed Cache）**
如果未来需要扩展到多实例部署，可以考虑引入Redis作为分布式缓存。Redis可以缓存搜索结果、价格信息等具有时效性的数据。

**数据库查询缓存**
利用JPA的二级缓存机制，缓存实体对象和查询结果，减少数据库访问次数。

#### 4.2.4 错误处理和重试机制

**统一异常处理**
使用Spring Boot的@ControllerAdvice注解创建全局异常处理器，统一处理系统中的各种异常情况。

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(SupplierException.class)
    public ResponseEntity<ErrorResponse> handleSupplierException(SupplierException e) {
        ErrorResponse error = new ErrorResponse(
            "SUPPLIER_ERROR", 
            e.getMessage(), 
            System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorResponse error = new ErrorResponse(
            "BUSINESS_ERROR", 
            e.getMessage(), 
            System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

**重试机制**
对于网络调用失败、超时等临时性错误，实施智能重试机制。可以使用Spring Retry或自定义重试逻辑。

```java
@Service
public class SupplierClientImpl implements SupplierClient {
    
    @Retryable(value = {ConnectTimeoutException.class, SocketTimeoutException.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public SupplierResponse callSupplierApi(SupplierRequest request) {
        // API调用逻辑
    }
    
    @Recover
    public SupplierResponse recover(Exception ex, SupplierRequest request) {
        // 重试失败后的恢复逻辑
        throw new SupplierException("供应商API调用失败: " + ex.getMessage());
    }
}
```

### 4.3 安全性设计

#### 4.3.1 API安全

**认证和授权**
实施基于Token的认证机制，确保只有授权的系统才能访问API。可以使用JWT（JSON Web Token）或简单的API Key机制。

```java
@Component
public class ApiKeyAuthenticationFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String apiKey = httpRequest.getHeader("X-API-Key");
        
        if (!isValidApiKey(apiKey)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

**数据验证**
对所有输入数据进行严格验证，防止SQL注入、XSS攻击等安全威胁。使用Bean Validation注解进行参数验证。

```java
@PostMapping("/hotels/search")
public ResponseEntity<HotelSearchResponse> searchHotels(
        @Valid @RequestBody HotelSearchRequest request) {
    // 业务逻辑处理
}

public class HotelSearchRequest {
    @NotBlank(message = "城市不能为空")
    @Size(max = 100, message = "城市名称长度不能超过100个字符")
    private String city;
    
    @NotNull(message = "入住日期不能为空")
    @Future(message = "入住日期必须是未来日期")
    private LocalDate checkInDate;
    
    @NotNull(message = "离店日期不能为空")
    @Future(message = "离店日期必须是未来日期")
    private LocalDate checkOutDate;
}
```

#### 4.3.2 供应商API安全

**敏感信息保护**
供应商的API密钥、认证信息等敏感数据应该加密存储，并通过配置管理系统进行管理。

```java
@Component
public class EncryptionService {
    
    @Value("${app.encryption.key}")
    private String encryptionKey;
    
    public String encrypt(String plainText) {
        // 使用AES加密算法加密敏感信息
    }
    
    public String decrypt(String encryptedText) {
        // 解密敏感信息
    }
}
```

**HTTPS通信**
确保所有与供应商API的通信都使用HTTPS协议，保证数据传输的安全性。

### 4.4 性能优化策略

#### 4.4.1 异步处理

对于不需要实时响应的操作，如日志记录、数据同步等，采用异步处理机制，提高系统的响应速度。

```java
@Service
public class AsyncService {
    
    @Async
    public CompletableFuture<Void> logApiCall(ApiCallLog log) {
        // 异步记录API调用日志
        apiCallLogRepository.save(log);
        return CompletableFuture.completedFuture(null);
    }
    
    @Async
    public CompletableFuture<Void> syncHotelData(Long supplierId) {
        // 异步同步酒店数据
        return CompletableFuture.completedFuture(null);
    }
}
```

#### 4.4.2 连接池优化

合理配置数据库连接池和HTTP连接池的参数，确保系统在高并发情况下的稳定性。

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

# HTTP连接池配置
http:
  client:
    connection-pool:
      max-total: 200
      max-per-route: 50
      connection-timeout: 5000
      socket-timeout: 30000
```

#### 4.4.3 批量处理

对于需要处理大量数据的场景，采用批量处理机制，减少网络开销和数据库访问次数。

```java
@Service
public class BatchProcessingService {
    
    public void batchUpdateHotelInfo(List<Hotel> hotels) {
        int batchSize = 100;
        for (int i = 0; i < hotels.size(); i += batchSize) {
            int end = Math.min(i + batchSize, hotels.size());
            List<Hotel> batch = hotels.subList(i, end);
            hotelRepository.saveAll(batch);
        }
    }
}
```

这种架构设计确保了系统的可维护性、可扩展性和性能，同时保持了单体应用的简洁性。通过模块化的设计，即使在单体架构下，也能够清晰地分离不同的业务关注点，为未来可能的架构演进奠定良好的基础。


## 5. 具体实现指导和最佳实践

### 5.1 项目初始化和环境搭建

#### 5.1.1 使用Spring Initializr创建项目

推荐使用Spring Initializr（https://start.spring.io/）快速创建项目骨架。选择以下配置：

**项目基本配置：**
- Project: Maven Project
- Language: Java
- Spring Boot:  3.x（推荐最新稳定版）
- Project Metadata:
  - Group: com.heytrip.hotel
  - Artifact: heytrip-supplier-integration
  - Name: HeyTrip Hotel Supplier Integration
  - Package name: com.heytrip.hotel.supplier
  - Packaging: Jar
  - Java:  17

**依赖选择：**
- Spring Web: 用于构建RESTful API
- Spring Data JPA: 用于数据持久层
- MySQL Driver: 数据库驱动
- Spring Boot Actuator: 用于监控和管理
- Validation: 用于数据验证
- Spring Cache: 缓存支持
- Sptring Webflux : 用于异步HTTP客户端

#### 5.1.2 Maven依赖配置

在生成的pom.xml基础上，添加以下额外依赖：

```xml
<dependencies>
    <!-- Spring Boot基础依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!--  webflux   -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- 数据库驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- 缓存实现 -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
    
    <!-- JSON处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    
    
    <!-- 重试机制 -->
    <dependency>
        <groupId>org.springframework.retry</groupId>
        <artifactId>spring-retry</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-aspects</artifactId>
    </dependency>
    
    <!-- 工具类 -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
    </dependency>
    
    <dependency>
        <groupId>commons-codec</groupId>
        <artifactId>commons-codec</artifactId>
    </dependency>
    
    <!-- 测试依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### 5.1.3 应用配置文件

创建application.yml配置文件，包含数据库、缓存、日志等配置：

```yaml
server:
  port: 8080
  servlet:
    context-path: /pax/api/xiwanSupplier/supp

spring:
  application:
    name: hotel-supplier-integration(pax)
  
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/supplier_pax?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:password}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: HotelSupplierHikariCP
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
  
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=30m
  
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null

logging:
  level:
    com.heytrip.hotel.supplier: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/hotel-supplier-integration(pax).log
    max-size: 100MB
    max-history: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

# 自定义配置
app:
  supplier:
    timeout: 30000
    retry:
      max-attempts: 3
      delay: 1000
      multiplier: 2
  encryption:
    key: ${ENCRYPTION_KEY:your-encryption-key-here}
  api:
    rate-limit:
      requests-per-minute: 1000
```

### 5.2 核心组件实现

#### 5.2.1 供应商适配器实现

首先定义供应商适配器的通用接口：

```java
package com.heytrip.hotel.supplier.integration.supplier;

import com.heytrip.hotel.supplier.dto.request.HotelSearchRequest;
import com.heytrip.hotel.supplier.dto.request.BookingRequest;
import com.heytrip.hotel.supplier.dto.response.HotelSearchResponse;
import com.heytrip.hotel.supplier.dto.response.BookingResponse;

public interface SupplierAdapter {
    
    /**
     * 获取供应商名称
     */
    String getSupplierName();
    
    /**
     * 搜索酒店
     */
    HotelSearchResponse searchHotels(HotelSearchRequest request);
    
    /**
     * 创建预订
     */
    BookingResponse createBooking(BookingRequest request);
    
    /**
     * 取消预订
     */
    BookingResponse cancelBooking(String bookingReference);
    
    /**
     * 查询预订状态
     */
    BookingResponse getBookingStatus(String bookingReference);
    
    /**
     * 检查供应商可用性
     */
    boolean isAvailable();
}
```

然后实现具体的供应商适配器，以Booking.com为例：

```java
package com.heytrip.hotel.supplier.integration.supplier.impl;

import com.heytrip.hotel.supplier.integration.supplier.SupplierAdapter;
import com.heytrip.hotel.supplier.integration.client.SupplierClient;
import com.heytrip.hotel.supplier.dto.request.HotelSearchRequest;
import com.heytrip.hotel.supplier.dto.request.BookingRequest;
import com.heytrip.hotel.supplier.dto.response.HotelSearchResponse;
import com.heytrip.hotel.supplier.dto.response.BookingResponse;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import com.heytrip.hotel.supplier.exception.SupplierException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.cache.annotation.Cacheable;

@Component
public class BookingComAdapter implements SupplierAdapter {
    
    private static final String SUPPLIER_NAME = "booking.com";
    
    @Autowired
    private SupplierClient supplierClient;
    
    @Autowired
    private SupplierConfigRepository configRepository;
    
    @Override
    public String getSupplierName() {
        return SUPPLIER_NAME;
    }
    
    @Override
    @Cacheable(value = "hotelSearch", key = "#request.city + '_' + #request.checkInDate + '_' + #request.checkOutDate")
    public HotelSearchResponse searchHotels(HotelSearchRequest request) {
        try {
            SupplierConfig config = getSupplierConfig();
            
            // 构建Booking.com特定的请求参数
            Map<String, Object> params = buildSearchParams(request);
            
            // 调用供应商API
            String response = supplierClient.get(
                config.getApiBaseUrl() + "/hotels/search", 
                params, 
                buildHeaders(config)
            );
            
            // 转换响应数据为标准格式
            return transformSearchResponse(response);
            
        } catch (Exception e) {
            throw new SupplierException("Booking.com酒店搜索失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public BookingResponse createBooking(BookingRequest request) {
        try {
            SupplierConfig config = getSupplierConfig();
            
            // 构建预订请求数据
            Map<String, Object> bookingData = buildBookingData(request);
            
            // 调用预订API
            String response = supplierClient.post(
                config.getApiBaseUrl() + "/bookings", 
                bookingData, 
                buildHeaders(config)
            );
            
            // 转换响应数据
            return transformBookingResponse(response);
            
        } catch (Exception e) {
            throw new SupplierException("Booking.com预订创建失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public BookingResponse cancelBooking(String bookingReference) {
        try {
            SupplierConfig config = getSupplierConfig();
            
            String response = supplierClient.delete(
                config.getApiBaseUrl() + "/bookings/" + bookingReference,
                buildHeaders(config)
            );
            
            return transformCancelResponse(response);
            
        } catch (Exception e) {
            throw new SupplierException("Booking.com预订取消失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public BookingResponse getBookingStatus(String bookingReference) {
        try {
            SupplierConfig config = getSupplierConfig();
            
            String response = supplierClient.get(
                config.getApiBaseUrl() + "/bookings/" + bookingReference,
                null,
                buildHeaders(config)
            );
            
            return transformStatusResponse(response);
            
        } catch (Exception e) {
            throw new SupplierException("Booking.com预订状态查询失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            SupplierConfig config = getSupplierConfig();
            
            String response = supplierClient.get(
                config.getApiBaseUrl() + "/health",
                null,
                buildHeaders(config)
            );
            
            return response != null && response.contains("\"status\":\"ok\"");
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private SupplierConfig getSupplierConfig() {
        return configRepository.findBySupplierNameAndIsActive(SUPPLIER_NAME, true)
            .orElseThrow(() -> new SupplierException("未找到" + SUPPLIER_NAME + "的配置信息"));
    }
    
    private Map<String, String> buildHeaders(SupplierConfig config) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + decryptAuthToken(config.getAuthConfig()));
        return headers;
    }
    
    private Map<String, Object> buildSearchParams(HotelSearchRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("destination", request.getCity());
        params.put("checkin", request.getCheckInDate().toString());
        params.put("checkout", request.getCheckOutDate().toString());
        params.put("adults", request.getGuestCount());
        params.put("rooms", request.getRoomCount());
        return params;
    }
    
    private HotelSearchResponse transformSearchResponse(String response) {
        // 将Booking.com的响应格式转换为系统标准格式
        // 这里需要根据实际的API响应格式进行实现
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(response);
            HotelSearchResponse result = new HotelSearchResponse();
            
            // 解析酒店列表
            JsonNode hotels = jsonNode.get("hotels");
            List<HotelInfo> hotelList = new ArrayList<>();
            
            for (JsonNode hotel : hotels) {
                HotelInfo hotelInfo = new HotelInfo();
                hotelInfo.setSupplierHotelId(hotel.get("id").asText());
                hotelInfo.setHotelName(hotel.get("name").asText());
                hotelInfo.setAddress(hotel.get("address").asText());
                hotelInfo.setStarRating(hotel.get("star_rating").asDouble());
                // ... 其他字段映射
                hotelList.add(hotelInfo);
            }
            
            result.setHotels(hotelList);
            result.setTotalCount(jsonNode.get("total_count").asInt());
            
            return result;
            
        } catch (Exception e) {
            throw new SupplierException("响应数据解析失败", e);
        }
    }
    
    // 其他转换方法的实现...
}
```

#### 5.2.2 HTTP客户端实现

实现统一的HTTP客户端，用于与供应商API通信：

```java
package com.heytrip.hotel.supplier.integration.client.impl;

import com.heytrip.hotel.supplier.integration.client.SupplierClient;
import com.heytrip.hotel.supplier.exception.SupplierException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Map;

@Component
public class RestTemplateClient implements SupplierClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    @Retryable(value = {ResourceAccessException.class, HttpServerErrorException.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public String get(String url, Map<String, Object> params, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            
            // 构建URL参数
            String fullUrl = buildUrlWithParams(url, params);
            
            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new SupplierException("HTTP请求失败: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new SupplierException("网络连接失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new SupplierException("请求处理失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Retryable(value = {ResourceAccessException.class, HttpServerErrorException.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public String post(String url, Object requestBody, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, httpHeaders);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                String.class
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new SupplierException("HTTP POST请求失败: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SupplierException("POST请求处理失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Retryable(value = {ResourceAccessException.class, HttpServerErrorException.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public String put(String url, Object requestBody, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, httpHeaders);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.PUT, 
                entity, 
                String.class
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new SupplierException("HTTP PUT请求失败: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SupplierException("PUT请求处理失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Retryable(value = {ResourceAccessException.class, HttpServerErrorException.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public String delete(String url, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.DELETE, 
                entity, 
                String.class
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new SupplierException("HTTP DELETE请求失败: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SupplierException("DELETE请求处理失败: " + e.getMessage(), e);
        }
    }
    
    private String buildUrlWithParams(String url, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }
        
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?");
        
        params.forEach((key, value) -> {
            urlBuilder.append(key).append("=").append(value).append("&");
        });
        
        // 移除最后一个&符号
        return urlBuilder.substring(0, urlBuilder.length() - 1);
    }
}
```

#### 5.2.3 业务服务层实现

实现核心的业务服务，整合多个供应商的数据：

```java
package com.heytrip.hotel.supplier.service.impl;


@Service
public class HotelOrderServiceImpl implements HotelOrderService {
    
    @Autowired
    private List<SupplierAdapter> supplierAdapters;
    
    @Autowired
    private HotelOrderRepository hotelOrderRepository;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    @Override
    @Cacheable(value = "hotelSearch", key = "#request.city + '_' + #request.checkInDate + '_' + #request.checkOutDate")
    public HotelSearchResponse searchHotels(HotelSearchRequest request) {
        validateSearchRequest(request);
        
        // 并行调用所有可用的供应商
        List<CompletableFuture<HotelSearchResponse>> futures = supplierAdapters.stream()
            .filter(SupplierAdapter::isAvailable)
            .map(adapter -> CompletableFuture.supplyAsync(() -> {
                try {
                    return adapter.searchHotels(request);
                } catch (Exception e) {
                    // 记录错误但不影响其他供应商的调用
                    logger.warn("供应商 {} 搜索失败: {}", adapter.getSupplierName(), e.getMessage());
                    return new HotelSearchResponse(); // 返回空结果
                }
            }, executorService))
            .collect(Collectors.toList());
        
        // 等待所有供应商响应并合并结果
        List<HotelSearchResponse> responses = futures.stream()
            .map(CompletableFuture::join)
            .filter(response -> response.getHotels() != null && !response.getHotels().isEmpty())
            .collect(Collectors.toList());
        
        if (responses.isEmpty()) {
            throw new BusinessException("没有找到符合条件的酒店");
        }
        
        // 合并和去重酒店数据
        return mergeHotelSearchResults(responses);
    }
    
    @Override
    public HotelInfo getHotelDetails(String hotelId) {
        return hotelOrderRepository.findById(Long.parseLong(hotelId))
            .orElseThrow(() -> new BusinessException("酒店信息不存在: " + hotelId));
    }
    
    @Override
    @Async
    public CompletableFuture<Void> syncHotelData() {
        supplierAdapters.parallelStream()
            .filter(SupplierAdapter::isAvailable)
            .forEach(adapter -> {
                try {
                    syncHotelDataFromSupplier(adapter);
                } catch (Exception e) {
                    logger.error("从供应商 {} 同步数据失败: {}", adapter.getSupplierName(), e.getMessage(), e);
                }
            });
        
        return CompletableFuture.completedFuture(null);
    }
    
    private void validateSearchRequest(HotelSearchRequest request) {
        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new BusinessException("入住日期不能早于今天");
        }
        
        if (request.getCheckOutDate().isBefore(request.getCheckInDate().plusDays(1))) {
            throw new BusinessException("离店日期必须晚于入住日期");
        }
        
        if (request.getRoomCount() <= 0 || request.getGuestCount() <= 0) {
            throw new BusinessException("房间数和客人数必须大于0");
        }
    }
    
    private HotelSearchResponse mergeHotelSearchResults(List<HotelSearchResponse> responses) {
        HotelSearchResponse mergedResponse = new HotelSearchResponse();
        Map<String, HotelInfo> hotelMap = new HashMap<>();
        
        // 合并所有供应商的酒店数据
        for (HotelSearchResponse response : responses) {
            for (HotelInfo hotel : response.getHotels()) {
                String key = generateHotelKey(hotel);
                
                if (hotelMap.containsKey(key)) {
                    // 如果酒店已存在，合并价格信息（选择最低价格）
                    HotelInfo existingHotel = hotelMap.get(key);
                    if (hotel.getLowestPrice() != null && 
                        (existingHotel.getLowestPrice() == null || 
                         hotel.getLowestPrice().compareTo(existingHotel.getLowestPrice()) < 0)) {
                        existingHotel.setLowestPrice(hotel.getLowestPrice());
                        existingHotel.setSupplierName(hotel.getSupplierName());
                    }
                } else {
                    hotelMap.put(key, hotel);
                }
            }
        }
        
        List<HotelInfo> mergedHotels = new ArrayList<>(hotelMap.values());
        
        // 按价格排序
        mergedHotels.sort(Comparator.comparing(HotelInfo::getLowestPrice, 
            Comparator.nullsLast(Comparator.naturalOrder())));
        
        mergedResponse.setHotels(mergedHotels);
        mergedResponse.setTotalCount(mergedHotels.size());
        mergedResponse.setSearchTime(System.currentTimeMillis());
        
        return mergedResponse;
    }
    
    private String generateHotelKey(HotelInfo hotel) {
        // 基于酒店名称和地址生成唯一键，用于去重
        return (hotel.getHotelName() + "_" + hotel.getAddress()).toLowerCase()
            .replaceAll("\\s+", "_");
    }
    
    private void syncHotelDataFromSupplier(SupplierAdapter adapter) {
        // 这里实现从供应商同步酒店基础数据的逻辑
        // 可以定期执行，更新酒店的基础信息
        logger.info("开始从供应商 {} 同步酒店数据", adapter.getSupplierName());
        
        // 实际实现会根据供应商API的具体情况来定制
        // 例如：分页获取所有酒店数据，然后保存到本地数据库
    }
}
```

### 5.3 配置和部署最佳实践

#### 5.3.1 环境配置管理

使用Spring Profile管理不同环境的配置：

**application-dev.yml（开发环境）：**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hotel_supplier_dev
    username: dev_user
    password: dev_password
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
logging:
  level:
    com.heytrip.hotel.supplier: DEBUG
    root: INFO

app:
  supplier:
    timeout: 60000  # 开发环境使用更长的超时时间
```

**application-prod.yml（生产环境）：**
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:hotel_supplier}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
logging:
  level:
    com.heytrip.hotel.supplier: INFO
    root: WARN
  file:
    name: /var/log/hotel-supplier/application.log

app:
  supplier:
    timeout: 30000
```

#### 5.3.2 Docker化部署

创建Dockerfile：

```dockerfile
FROM openjdk:17-jdk-slim

LABEL maintainer="your-email@company.com"

# 设置工作目录
WORKDIR /app

# 复制Maven构建的JAR文件
COPY target/supplier-integration-*.jar app.jar

# 创建日志目录
RUN mkdir -p /var/log/hotel-supplier

# 暴露端口
EXPOSE 8080

# 设置JVM参数
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+PrintGCDetails"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
```

创建docker-compose.yml：

```yaml
version: '3.8'

services:
  hotel-supplier:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=hotel_supplier
      - DB_USERNAME=hotel_user
      - DB_PASSWORD=hotel_password
      - ENCRYPTION_KEY=your-encryption-key-here
    depends_on:
      - mysql
    volumes:
      - ./logs:/var/log/hotel-supplier
    restart: unless-stopped
    networks:
      - hotel-network

  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root_password
      - MYSQL_DATABASE=hotel_supplier
      - MYSQL_USER=hotel_user
      - MYSQL_PASSWORD=hotel_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    restart: unless-stopped
    networks:
      - hotel-network

volumes:
  mysql_data:

networks:
  hotel-network:
    driver: bridge
```

#### 5.3.3 监控和健康检查

实现自定义健康检查：

```java
package com.heytrip.hotel.supplier.health;

import com.heytrip.hotel.supplier.integration.supplier.SupplierAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SupplierHealthIndicator implements HealthIndicator {
    
    @Autowired
    private List<SupplierAdapter> supplierAdapters;
    
    @Override
    public Health health() {
        Map<String, Boolean> supplierStatus = supplierAdapters.stream()
            .collect(Collectors.toMap(
                SupplierAdapter::getSupplierName,
                adapter -> {
                    try {
                        return adapter.isAvailable();
                    } catch (Exception e) {
                        return false;
                    }
                }
            ));
        
        long availableCount = supplierStatus.values().stream()
            .mapToLong(status -> status ? 1 : 0)
            .sum();
        
        Health.Builder builder = availableCount > 0 ? Health.up() : Health.down();
        
        return builder
            .withDetail("suppliers", supplierStatus)
            .withDetail("availableSuppliers", availableCount)
            .withDetail("totalSuppliers", supplierAdapters.size())
            .build();
    }
}
```

### 5.4 测试策略

#### 5.4.1 单元测试

为核心业务逻辑编写单元测试：

```java
package com.heytrip.hotel.supplier.service;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceImplTest {
    
    @Mock
    private SupplierAdapter supplierAdapter1;
    
    @Mock
    private SupplierAdapter supplierAdapter2;
    
    @Mock
    private HotelOrderRepository hotelOrderRepository;
    
    @InjectMocks
    private HotelServiceImpl hotelService;
    
    private List<SupplierAdapter> supplierAdapters;
    
    @BeforeEach
    void setUp() {
        supplierAdapters = Arrays.asList(supplierAdapter1, supplierAdapter2);
        // 使用反射设置私有字段
        ReflectionTestUtils.setField(hotelService, "supplierAdapters", supplierAdapters);
    }
    
    @Test
    void testSearchHotels_Success() {
        // Given
        HotelSearchRequest request = new HotelSearchRequest();
        request.setCity("北京");
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(3));
        request.setRoomCount(1);
        request.setGuestCount(2);
        
        HotelSearchResponse mockResponse1 = createMockHotelSearchResponse("supplier1");
        HotelSearchResponse mockResponse2 = createMockHotelSearchResponse("supplier2");
        
        when(supplierAdapter1.isAvailable()).thenReturn(true);
        when(supplierAdapter2.isAvailable()).thenReturn(true);
        when(supplierAdapter1.searchHotels(request)).thenReturn(mockResponse1);
        when(supplierAdapter2.searchHotels(request)).thenReturn(mockResponse2);
        
        // When
        HotelSearchResponse result = hotelService.searchHotels(request);
        
        // Then
        assertNotNull(result);
        assertFalse(result.getHotels().isEmpty());
        verify(supplierAdapter1).searchHotels(request);
        verify(supplierAdapter2).searchHotels(request);
    }
    
    @Test
    void testSearchHotels_InvalidRequest() {
        // Given
        HotelSearchRequest request = new HotelSearchRequest();
        request.setCity("北京");
        request.setCheckInDate(LocalDate.now().minusDays(1)); // 过去的日期
        request.setCheckOutDate(LocalDate.now().plusDays(1));
        
        // When & Then
        assertThrows(BusinessException.class, () -> hotelService.searchHotels(request));
    }
    
    private HotelSearchResponse createMockHotelSearchResponse(String supplierName) {
        HotelSearchResponse response = new HotelSearchResponse();
        HotelInfo hotel = new HotelInfo();
        hotel.setSupplierName(supplierName);
        hotel.setHotelName("测试酒店");
        hotel.setLowestPrice(BigDecimal.valueOf(299.00));
        response.setHotels(Arrays.asList(hotel));
        return response;
    }
}
```

#### 5.4.2 集成测试

创建集成测试验证整个API流程：

```java
package com.heytrip.hotel.supplier.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.dto.request.HotelSearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class HotelSearchIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testHotelSearchEndpoint() throws Exception {
        HotelSearchRequest request = new HotelSearchRequest();
        request.setCity("北京");
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(3));
        request.setRoomCount(1);
        request.setGuestCount(2);
        
        mockMvc.perform(post("/hotels/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hotels").isArray());
    }
}
```

## 6. API接口文档

### 6.1 接口概览

本系统提供三大类API接口：

1. **静态数据类接口**：获取供应商信息、支持的城市、货币等静态数据
2. **报价类接口**：酒店搜索、房型查询、价格获取等功能
3. **订单类接口**：订单创建、取消、状态查询等订单管理功能

### 6.2 认证机制

所有API请求都需要在HTTP头中包含以下认证信息：

```
X-App-Id: your-app-id
X-Timestamp: 1609459200000
X-Signature: calculated-md5-signature
```

签名计算方式：
```
signature = MD5(appId + timestamp + secretKey + requestBody)
```

### 6.3 静态数据类接口

#### 6.3.1 获取供应商列表

**接口地址**：`GET /suppliers`

**请求参数**：无

**响应示例**：
```json
{
    "code": "SUCCESS",
    "message": "操作成功",
    "data": [
        {
            "supplierId": 1,
            "supplierName": "booking.com",
            "isActive": true,
            "supportedCities": ["北京", "上海", "广州"],
            "supportedCurrencies": ["CNY", "USD"]
        }
    ]
}
```

#### 6.3.2 获取支持的城市列表

**接口地址**：`GET /cities`

**请求参数**：
- `country`（可选）：国家代码，如"CN"

**响应示例**：
```json
{
    "code": "SUCCESS",
    "message": "操作成功",
    "data": [
        {
            "cityCode": "BJ",
            "cityName": "北京",
            "countryCode": "CN",
            "countryName": "中国"
        }
    ]
}
```

### 6.4 报价类接口

#### 6.4.1 酒店搜索

**接口地址**：`POST /hotels/search`

**请求参数**：
```json
{
    "city": "北京",
    "checkInDate": "2024-03-15",
    "checkOutDate": "2024-03-17",
    "roomCount": 1,
    "guestCount": 2,
    "starRating": 4,
    "priceRange": {
        "minPrice": 200,
        "maxPrice": 800
    }
}
```

**响应示例**：
```json
{
    "code": "SUCCESS",
    "message": "操作成功",
    "data": {
        "hotels": [
            {
                "hotelId": "12345",
                "hotelName": "北京国际酒店",
                "address": "北京市朝阳区建国门外大街1号",
                "starRating": 5.0,
                "latitude": 39.9042,
                "longitude": 116.4074,
                "lowestPrice": 599.00,
                "currency": "CNY",
                "supplierName": "booking.com",
                "images": [
                    "https://example.com/hotel1.jpg"
                ],
                "amenities": ["WiFi", "停车场", "健身房"],
                "rooms": [
                    {
                        "roomType": "标准双人间",
                        "price": 599.00,
                        "currency": "CNY",
                        "availability": 5
                    }
                ]
            }
        ],
        "totalCount": 1,
        "searchTime": 1609459200000
    }
}
```

#### 6.4.2 获取酒店详情

**接口地址**：`GET /hotels/{hotelId}`

**路径参数**：
- `hotelId`：酒店ID

**响应示例**：
```json
{
    "code": "SUCCESS",
    "message": "操作成功",
    "data": {
        "hotelId": "12345",
        "hotelName": "北京国际酒店",
        "description": "位于市中心的豪华酒店...",
        "facilities": ["餐厅", "会议室", "商务中心"],
        "policies": {
            "checkInTime": "14:00",
            "checkOutTime": "12:00",
            "cancellationPolicy": "免费取消至入住前24小时"
        }
    }
}
```

### 6.5 订单类接口

#### 6.5.1 创建订单

**接口地址**：`POST /orders`

**请求参数**：
```json
{
    "hotelId": "12345",
    "roomType": "标准双人间",
    "checkInDate": "2024-03-15",
    "checkOutDate": "2024-03-17",
    "guestInfo": {
        "name": "张三",
        "email": "zhangsan@example.com",
        "phone": "13800138000"
    },
    "roomCount": 1,
    "guestCount": 2,
    "specialRequests": "高层房间"
}
```

**响应示例**：
```json
{
    "code": "SUCCESS",
    "message": "订单创建成功",
    "data": {
        "orderId": "ORD20240315001",
        "supplierOrderId": "SUP123456",
        "status": "CONFIRMED",
        "totalAmount": 1198.00,
        "currency": "CNY",
        "confirmationNumber": "CONF789012",
        "createdAt": "2024-03-15T10:30:00Z"
    }
}
```

#### 6.5.2 取消订单

**接口地址**：`DELETE /orders/{orderId}`

**路径参数**：
- `orderId`：订单ID

**响应示例**：
```json
{
    "code": "SUCCESS",
    "message": "订单取消成功",
    "data": {
        "orderId": "ORD20240315001",
        "status": "CANCELLED",
        "refundAmount": 1198.00,
        "refundCurrency": "CNY",
        "cancelledAt": "2024-03-15T15:45:00Z"
    }
}
```

#### 6.5.3 查询订单状态

**接口地址**：`GET /orders/{orderId}`

**路径参数**：
- `orderId`：订单ID

**响应示例**：
```json
{
    "code": "SUCCESS",
    "message": "查询成功",
    "data": {
        "orderId": "ORD20240315001",
        "status": "CONFIRMED",
        "hotelInfo": {
            "hotelName": "北京国际酒店",
            "address": "北京市朝阳区建国门外大街1号"
        },
        "bookingDetails": {
            "checkInDate": "2024-03-15",
            "checkOutDate": "2024-03-17",
            "roomType": "标准双人间",
            "guestCount": 2
        },
        "paymentInfo": {
            "totalAmount": 1198.00,
            "currency": "CNY",
            "paymentStatus": "PAID"
        }
    }
}
```

### 6.6 错误响应格式

所有错误响应都遵循统一格式：

```json
{
    "code": "ERROR_CODE",
    "message": "错误描述信息",
    "timestamp": 1609459200000,
    "path": "/hotels/search",
    "details": {
        "field": "checkInDate",
        "rejectedValue": "2024-02-30",
        "message": "日期格式无效"
    }
}
```

常见错误代码：
- `INVALID_REQUEST`：请求参数无效
- `SUPPLIER_ERROR`：供应商API调用失败
- `BUSINESS_ERROR`：业务逻辑错误
- `AUTHENTICATION_FAILED`：认证失败
- `RATE_LIMIT_EXCEEDED`：请求频率超限

## 7. 部署和运维指南

### 7.1 环境要求

**系统要求**：
- Java 17+
- MySQL 8.0+
- Redis 6.0+（可选，用于分布式缓存）
- Docker 20.0+（容器化部署）

**硬件要求**：
- CPU：2核心以上
- 内存：4GB以上
- 磁盘：20GB以上可用空间
- 网络：稳定的互联网连接

### 7.2 部署步骤

#### 7.2.1 传统部署

1. **准备数据库**：
```sql
CREATE DATABASE hotel_supplier CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'hotel_user'@'%' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON hotel_supplier.* TO 'hotel_user'@'%';
FLUSH PRIVILEGES;
```

2. **编译应用**：
```bash
mvn clean package -DskipTests
```

3. **配置环境变量**：
```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=hotel_supplier
export DB_USERNAME=hotel_user
export DB_PASSWORD=secure_password
export ENCRYPTION_KEY=your-32-char-encryption-key-here
```

4. **启动应用**：
```bash
java -jar target/supplier-integration-1.0.0.jar
```

#### 7.2.2 Docker部署

1. **构建镜像**：
```bash
docker build -t hotel-supplier:latest .
```

2. **使用Docker Compose启动**：
```bash
docker-compose up -d
```

3. **查看服务状态**：
```bash
docker-compose ps
docker-compose logs hotel-supplier
```

### 7.3 监控和日志

#### 7.3.1 健康检查

应用提供以下健康检查端点：

- `/actuator/health`：应用整体健康状态
- `/actuator/health/db`：数据库连接状态
- `/actuator/health/suppliers`：供应商可用性状态

#### 7.3.2 指标监控

通过Prometheus收集以下指标：

- HTTP请求数量和响应时间
- 数据库连接池状态
- 缓存命中率
- 供应商API调用成功率
- JVM内存和GC指标

#### 7.3.3 日志管理

日志配置支持：

- 按日期轮转日志文件
- 不同级别的日志输出
- 结构化日志格式（JSON）
- 集中化日志收集（ELK Stack）

### 7.4 性能调优

#### 7.4.1 JVM参数优化

```bash
-Xms512m -Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/hotel-supplier/
```

#### 7.4.2 数据库优化

- 合理设置连接池大小
- 添加适当的数据库索引
- 定期分析和优化慢查询
- 配置读写分离（如需要）

#### 7.4.3 缓存优化

- 调整缓存过期时间
- 监控缓存命中率
- 合理设置缓存大小
- 考虑使用Redis集群

## 8. 故障排除

### 8.1 常见问题

#### 8.1.1 应用启动失败

**问题**：应用无法启动，提示数据库连接失败

**解决方案**：
1. 检查数据库服务是否正常运行
2. 验证数据库连接配置是否正确
3. 确认数据库用户权限是否足够
4. 检查网络连接是否正常

#### 8.1.2 供应商API调用失败

**问题**：供应商API调用超时或返回错误

**解决方案**：
1. 检查供应商API服务状态
2. 验证认证信息是否正确
3. 调整超时配置
4. 检查网络连接和防火墙设置

#### 8.1.3 内存溢出

**问题**：应用出现OutOfMemoryError

**解决方案**：
1. 增加JVM堆内存大小
2. 分析内存泄漏问题
3. 优化缓存配置
4. 检查是否有大对象未及时释放

### 8.2 日志分析

#### 8.2.1 关键日志位置

- 应用日志：`/var/log/hotel-supplier/application.log`
- 访问日志：`/var/log/hotel-supplier/access.log`
- 错误日志：`/var/log/hotel-supplier/error.log`
- GC日志：`/var/log/hotel-supplier/gc.log`

#### 8.2.2 日志级别说明

- `ERROR`：系统错误，需要立即处理
- `WARN`：警告信息，可能影响功能
- `INFO`：一般信息，记录重要操作
- `DEBUG`：调试信息，用于问题排查

## 9. 扩展和维护

### 9.1 添加新供应商

1. **实现供应商适配器**：
```java
@Component
public class NewSupplierAdapter implements SupplierAdapter {
    // 实现接口方法
}
```

2. **添加供应商配置**：
```sql
INSERT INTO supplier_config (supplier_name, api_base_url, auth_type, auth_config) 
VALUES ('new_supplier', 'https://api.newsupplier.com', 'API_KEY', '{"apiKey": "your-key"}');
```

3. **更新测试用例**：
```java
@Test
void testNewSupplierIntegration() {
    // 添加新供应商的测试用例
}
```

### 9.2 版本升级

1. **数据库迁移**：
```sql
-- V3__Add_new_features.sql
ALTER TABLE hotel_info ADD COLUMN new_field VARCHAR(100);
```

2. **配置更新**：
```yaml
# 添加新的配置项
app:
  new-feature:
    enabled: true
```

3. **向后兼容性**：
- 保持API接口向后兼容
- 提供版本控制机制
- 逐步废弃旧功能

### 9.3 性能监控

定期监控以下指标：

- API响应时间
- 数据库查询性能
- 缓存命中率
- 供应商API成功率
- 系统资源使用情况

## 10. 总结

本项目实现了一个完整的酒店供应商集成系统，具有以下特点：

**技术优势**：
- 基于Spring Boot的现代化架构
- 模块化设计，易于扩展和维护
- 完善的错误处理和重试机制
- 多层缓存策略，提升性能
- 容器化部署，便于运维

**业务价值**：
- 统一的API接口，简化集成复杂度
- 多供应商支持，提高可用性
- 实时数据聚合，提供最优价格
- 完整的订单生命周期管理
- 详细的监控和日志，保障系统稳定

**扩展性**：
- 插件化的供应商适配器
- 灵活的配置管理
- 支持水平扩展
- 预留分布式架构升级路径

该系统为酒店业务提供了稳定、高效、可扩展的供应商集成解决方案，能够满足当前业务需求，并为未来发展奠定了良好基础。
        request.setCity("北京");
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(3));
        request.setRoomCount(1);
        request.setGuestCount(2);
        
        HotelSearchResponse response1 = new HotelSearchResponse();
        HotelSearchResponse response2 = new HotelSearchResponse();
        
        when(supplierAdapter1.isAvailable()).thenReturn(true);
        when(supplierAdapter2.isAvailable()).thenReturn(true);
        when(supplierAdapter1.searchHotels(request)).thenReturn(response1);
        when(supplierAdapter2.searchHotels(request)).thenReturn(response2);
        
        // When
        HotelSearchResponse result = hotelService.searchHotels(request);
        
        // Then
        assertNotNull(result);
        verify(supplierAdapter1).searchHotels(request);
        verify(supplierAdapter2).searchHotels(request);
    }
    
    @Test
    void testSearchHotels_InvalidDate() {
        // Given
        HotelSearchRequest request = new HotelSearchRequest();
        request.setCity("北京");
        request.setCheckInDate(LocalDate.now().minusDays(1)); // 过去的日期
        request.setCheckOutDate(LocalDate.now().plusDays(1));
        
        // When & Then
        assertThrows(BusinessException.class, () -> hotelService.searchHotels(request));
    }
}
```

#### 5.4.2 集成测试

编写集成测试验证API端点：

```java
package com.heytrip.hotel.supplier.controller;

import com.heytrip.hotel.supplier.dto.request.HotelSearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class HotelControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testSearchHotels() throws Exception {
        HotelSearchRequest request = new HotelSearchRequest();
        request.setCity("北京");
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(3));
        request.setRoomCount(1);
        request.setGuestCount(2);
        
        mockMvc.perform(post("/api/v1/hotels/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hotels").exists())
                .andExpect(jsonPath("$.totalCount").exists());
    }
}
```

### 5.5 性能优化建议

#### 5.5.1 数据库优化

**索引策略：**
```sql
-- 为常用查询字段创建索引
CREATE INDEX idx_hotel_city ON hotel_info(city);
CREATE INDEX idx_hotel_supplier_active ON hotel_info(supplier_id, is_active);
CREATE INDEX idx_booking_status_date ON booking_record(booking_status, created_at);
CREATE INDEX idx_api_log_supplier_time ON api_call_log(supplier_id, created_at);

-- 为JSON字段创建函数索引（MySQL 8.0+）
CREATE INDEX idx_hotel_amenities_wifi ON hotel_info((JSON_EXTRACT(amenities, '$.wifi')));
```

**查询优化：**
```java
// 使用JPA的@Query注解优化查询
@Repository
public interface HotelOrderRepository extends JpaRepository<HotelInfo, Long> {
    
    @Query("SELECT h FROM HotelInfo h WHERE h.city = :city AND h.isActive = true")
    List<HotelInfo> findActiveHotelsByCity(@Param("city") String city);
    
    @Query(value = "SELECT * FROM hotel_info WHERE city = :city AND is_active = true LIMIT :limit", 
           nativeQuery = true)
    List<HotelInfo> findActiveHotelsByCityWithLimit(@Param("city") String city, @Param("limit") int limit);
}
```

#### 5.5.2 缓存优化

**多级缓存策略：**
```java
@Service
public class CacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "hotelSearch", key = "#request.hashCode()")
    public HotelSearchResponse getCachedSearchResult(HotelSearchRequest request) {
        // 首先检查本地缓存（Caffeine）
        // 然后检查分布式缓存（Redis）
        // 最后调用实际的搜索逻辑
        return null;
    }
    
    @CacheEvict(value = "hotelSearch", allEntries = true)
    public void clearSearchCache() {
        // 清除搜索缓存
    }
}
```

#### 5.5.3 异步处理优化

**线程池配置：**
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("HotelSupplier-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
```

通过以上详细的实现指导和最佳实践，您可以构建一个稳定、高效、可维护的酒店供应商对接系统。这个架构设计既满足了当前单体应用的需求，又为未来可能的扩展和优化留下了充分的空间。



## 6 项目总结
### 6.1 架构设计要点

**分层架构的重要性**：通过清晰的分层设计，我们将表现层、业务逻辑层和数据访问层进行了有效分离。这种设计不仅提高了代码的可维护性，也使得不同层次的组件可以独立演进和测试。

**供应商适配器模式**：采用适配器模式来处理不同供应商API的差异，这是整个架构的核心设计。每个供应商都有独立的适配器实现，既保证了代码的模块化，也便于新增或修改供应商集成。

**统一的错误处理机制**：通过全局异常处理器和重试机制，确保系统能够优雅地处理各种异常情况。这对于集成多个外部API的系统来说至关重要。

**多层缓存策略**：结合本地缓存和分布式缓存，在提高系统性能的同时，也为未来的扩展做好了准备。


### 6.2 风险控制

**供应商依赖风险**：由于系统依赖多个外部供应商API，需要建立完善的降级和容错机制。当某个供应商不可用时，系统应该能够继续使用其他供应商的服务。

**数据一致性风险**：不同供应商返回的数据格式和内容可能存在差异，需要建立标准化的数据转换和验证机制，确保数据的一致性和准确性。

**性能风险**：随着集成供应商数量的增加，系统的响应时间可能会受到影响。需要通过缓存、异步处理和并行调用等技术手段来优化性能。

**安全风险**：系统需要存储和管理多个供应商的认证信息，必须采用加密存储和安全传输等措施来保护敏感数据。


---


