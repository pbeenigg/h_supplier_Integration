# HttpClientService 使用指南

## 概述

`HttpClientService` 是一个统一的HTTP客户端服务，为所有供应商适配器提供标准化的HTTP请求处理功能。它集成了日志记录、重试机制、错误处理、超时控制等企业级功能。

## 核心功能

### 1. 统一的HTTP方法支持
- **GET请求**: 用于数据查询和获取
- **POST请求**: 用于数据提交和创建
- **PUT请求**: 用于数据更新
- **DELETE请求**: 用于数据删除

### 2. 企业级功能
- **自动重试机制**: 支持指数退避重试策略
- **超时控制**: 默认30秒超时，可自定义
- **日志记录**: 自动记录API调用详情到数据库
- **错误处理**: 统一的异常处理和错误响应
- **请求/响应拦截**: 支持自定义头部和拦截器

## 基本使用方法

### 1. 依赖注入

```java
@Component
public class YourAdapter extends AbstractSupplierAdapter {
    
    @Autowired
    private HttpClientService httpClientService;
    
    // 其他代码...
}
```

### 2. GET请求示例

```java
// 简单GET请求
public Mono<ResponseType> getData() {
    return httpClientService.get(
        "https://api.example.com",           // 基础URL
        "/api/v1/data",                      // 端点路径
        ResponseType.class,                  // 响应类型
        headers -> {                         // 自定义头部（可选）
            headers.header("Authorization", "Bearer token");
            headers.header("User-Agent", "YourApp/1.0");
        }
    );
}

// 带查询参数的GET请求
public Mono<SearchResponse> searchData(String query) {
    String endpoint = "/api/v1/search?q=" + query + "&limit=10";
    
    return httpClientService.get(
        "https://api.example.com",
        endpoint,
        SearchResponse.class,
        null  // 不需要自定义头部
    );
}
```

### 3. POST请求示例

```java
// POST请求提交数据
public Mono<BookingResponse> createBooking(BookingRequest request) {
    return httpClientService.post(
        "https://api.example.com",
        "/api/v1/bookings",
        request,                             // 请求体
        BookingResponse.class,
        headers -> {
            headers.header("Content-Type", "application/json");
            headers.header("Authorization", "Bearer token");
        }
    );
}
```

### 4. 自定义重试和超时

```java
// 使用自定义重试次数和超时时间
public Mono<ResponseType> callWithCustomSettings() {
    return httpClientService.executeWithRetry(
        "https://api.example.com",
        "/api/v1/data",
        HttpMethod.GET,
        null,                                // 请求体（GET请求为null）
        ResponseType.class,
        headers -> headers.header("User-Agent", "YourApp/1.0"),
        5,                                   // 重试次数
        Duration.ofSeconds(60)               // 超时时间
    );
}
```

## 在AsianOverlandAdapter中的实际应用

### 1. 酒店搜索API调用

```java
private Mono<QTechSearchResponse> callQTechSearchApi(Map<String, String> params) {
    StringBuilder endpoint = new StringBuilder("/ws/index.php?");
    params.forEach((key, value) -> {
        endpoint.append(key).append("=").append(value).append("&");
    });
    
    logger.debug("调用QTECH搜索API: {}{}", SEARCH_BASE_URL, endpoint.toString());
    
    return httpClientService.get(
        SEARCH_BASE_URL,
        endpoint.toString(),
        QTechSearchResponse.class,
        headers -> headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter/1.0")
    );
}
```

### 2. 酒店预订API调用

```java
private Mono<QTechReservationResponse> executeReservation(String hotelId, String roomId, String agentRefNo, 
                                                        QTechCancellationPolicyResponse policy) {
    Map<String, String> params = buildReservationParams(hotelId, roomId, agentRefNo, policy);
    
    StringBuilder endpoint = new StringBuilder("/ws/index.php?");
    params.forEach((key, value) -> {
        endpoint.append(key).append("=").append(value).append("&");
    });
    
    return httpClientService.get(
        API_BASE_URL,
        endpoint.toString(),
        QTechReservationResponse.class,
        headers -> headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter/1.0")
    );
}
```

## 最佳实践

### 1. 错误处理

```java
public Mono<ResponseType> callApiWithErrorHandling() {
    return httpClientService.get(baseUrl, endpoint, ResponseType.class, null)
        .doOnSuccess(response -> {
            // 成功处理逻辑
            logger.info("API调用成功: {}", response);
        })
        .doOnError(error -> {
            // 错误处理逻辑
            logger.error("API调用失败", error);
        })
        .onErrorResume(error -> {
            // 降级处理或返回默认值
            return Mono.just(getDefaultResponse());
        });
}
```

### 2. 响应式编程链式调用

```java
public Mono<FinalResult> complexApiFlow() {
    return httpClientService.get(baseUrl, "/step1", Step1Response.class, null)
        .flatMap(step1Response -> {
            // 使用第一步的结果调用第二步
            return httpClientService.post(baseUrl, "/step2", 
                buildStep2Request(step1Response), Step2Response.class, null);
        })
        .flatMap(step2Response -> {
            // 使用第二步的结果调用第三步
            return httpClientService.put(baseUrl, "/step3", 
                buildStep3Request(step2Response), FinalResult.class, null);
        })
        .doOnSuccess(result -> logger.info("复杂流程完成: {}", result))
        .doOnError(error -> logger.error("复杂流程失败", error));
}
```

### 3. 批量请求处理

```java
public Mono<Map<String, ResponseType>> batchApiCalls() {
    Map<String, HttpClientService.RequestConfig<ResponseType>> requests = new HashMap<>();
    
    // 构建多个请求配置
    requests.put("request1", new HttpClientService.RequestConfig<>(
        baseUrl, "/api/data1", HttpMethod.GET, null, ResponseType.class, null));
    requests.put("request2", new HttpClientService.RequestConfig<>(
        baseUrl, "/api/data2", HttpMethod.GET, null, ResponseType.class, null));
    
    return httpClientService.executeBatch(requests);
}
```

### 4. 日志记录最佳实践

```java
public Mono<ResponseType> callWithProperLogging() {
    logger.info("开始调用外部API - 端点: {}", endpoint);
    
    return httpClientService.get(baseUrl, endpoint, ResponseType.class, null)
        .doOnSubscribe(subscription -> 
            logger.debug("API请求已提交: {}{}", baseUrl, endpoint))
        .doOnSuccess(response -> 
            logger.info("API调用成功，响应状态: {}", response.getStatus()))
        .doOnError(error -> 
            logger.error("API调用失败 - 端点: {}, 错误: {}", endpoint, error.getMessage()))
        .doFinally(signalType -> 
            logger.debug("API调用完成，信号类型: {}", signalType));
}
```

## 配置说明

### 1. 默认配置
- **连接超时**: 30秒
- **读取超时**: 30秒
- **重试次数**: 3次
- **重试策略**: 指数退避（1秒起始，最大10秒间隔）
- **内存缓冲区**: 16MB

### 2. 可重试的错误类型
- 5xx服务器错误
- 408 请求超时
- 429 请求过多
- 网络连接异常
- Socket超时异常
- IO异常

### 3. 日志记录内容
- 请求URL和方法
- 请求体（截断至4000字符）
- 响应体（截断至4000字符）
- 响应状态码
- 响应时间（毫秒）
- 错误信息（如有）
- 供应商ID（自动识别）

## 注意事项

### 1. 内存管理
- 大文件上传/下载时注意内存使用
- 响应体过大时会自动截断日志记录
- 建议对大数据量请求使用流式处理

### 2. 安全考虑
- 敏感信息（如密码、令牌）不会记录到日志中
- 使用HTTPS进行敏感数据传输
- 实现适当的认证和授权机制

### 3. 性能优化
- 合理设置超时时间，避免长时间阻塞
- 使用批量请求减少网络开销
- 利用响应式编程的非阻塞特性

### 4. 监控和告警
- HttpClientService自动记录API调用指标
- 可通过ApiCallLog表查询调用历史
- 建议设置基于响应时间和错误率的告警

## 故障排查

### 1. 常见问题
- **连接超时**: 检查网络连接和目标服务状态
- **读取超时**: 检查目标服务响应时间，考虑增加超时设置
- **序列化错误**: 检查响应格式是否与期望的类型匹配
- **认证失败**: 检查认证信息和头部设置

### 2. 调试技巧
- 启用DEBUG日志查看详细请求信息
- 查询ApiCallLog表分析历史调用记录
- 使用doOnXxx操作符添加调试日志
- 利用WebClient的日志功能查看底层HTTP交互

## 扩展功能

### 1. 自定义拦截器
```java
// 可以通过配置添加全局拦截器
// 例如：认证拦截器、日志拦截器、指标收集拦截器
```

### 2. 缓存集成
```java
// 可以结合Redis实现响应缓存
// 减少重复API调用，提高性能
```

### 3. 熔断器集成
```java
// 可以集成Resilience4j实现熔断器模式
// 防止级联故障，提高系统稳定性
```

通过遵循这些最佳实践和指南，您可以充分利用HttpClientService的强大功能，构建稳定、高效、可维护的供应商适配器。
