package com.heytrip.hotel.supplier.client;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.entity.ApiCallLog;
import com.heytrip.hotel.supplier.exception.HttpClientException;
import com.heytrip.hotel.supplier.repository.ApiCallLogRepository;
import com.heytrip.hotel.supplier.utils.JsonCompressionUtil;
import com.heytrip.hotel.supplier.utils.TraceIdHolder;
import com.heytrip.hotel.supplier.utils.UrlUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * HTTP客户端服务
 * 提供统一的HTTP请求处理，包括日志记录、重试机制、错误处理
 *
 * @author  Pax
 */
@Service
public class HttpClientService {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientService.class);

    @Autowired
    private ApiCallLogRepository apiCallLogRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    // 预配置的WebClient实例，避免每次请求都重新创建
    private WebClient webClient;
    
    // 并发控制：限制最大并发请求数为50
    private final Semaphore requestSemaphore = new Semaphore(15);
    
    // 自定义调度器：支持高并发场景
    private final Scheduler customScheduler = Schedulers.newBoundedElastic(
        15,                     // 最大线程数
        100000,                 // 最大队列大小  
        "http-client-scheduler" // 线程名前缀
    );
    
    // 监控指标
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong connectionErrors = new AtomicLong(0);
    

    /**
     * 初始化WebClient实例
     * 配置连接池优化、超时设置和自定义调度器
     */
    @PostConstruct
    private void initWebClient() {
        logger.info("初始化HttpClientService - 配置连接池和调度器优化");
        
        // 配置连接池：针对第三方API调用优化
        ConnectionProvider connectionProvider = ConnectionProvider.builder("http-client-pool")
                .maxConnections(15)                                    // 最大连接数15
                .maxIdleTime(Duration.ofSeconds(30))                   // 连接空闲时间30秒
                .maxLifeTime(Duration.ofSeconds(60))                   // 连接最大生命周期60秒
                .pendingAcquireTimeout(Duration.ofSeconds(30))         // 获取连接超时30秒
                .evictInBackground(Duration.ofSeconds(30))             // 后台清理间隔30秒
                .build();
        
        // 配置HttpClient：设置超时和连接参数
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)   // 连接超时10秒
                .responseTimeout(Duration.ofSeconds(30))               // 响应超时10秒
                .compress(true)                                        // 启用HTTP压缩(gzip/deflate)
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(30))    // 读取超时10秒
                        .addHandlerLast(new WriteTimeoutHandler(30))   // 写入超时10秒
                );
        
        // 构建优化的WebClient实例
        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
                
        logger.info("HttpClientService初始化完成 - 连接池大小:20, 超时:10秒, 并发限制:50");
    }

    /**
     * 执行GET请求
     */
    public <T> Mono<T> get(String baseUrl, String endpoint, Class<T> responseType,
                          Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer) {
        return executeRequest(baseUrl, endpoint, HttpMethod.GET, null, responseType, headersCustomizer, null);
    }

    /**
     * GET请求（带供应商ID）
     */
    public <T> Mono<T> get(String baseUrl, String endpoint, Class<T> responseType,
                          Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer, Long supplierId) {
        return executeRequest(baseUrl, endpoint, HttpMethod.GET, null, responseType, headersCustomizer, supplierId);
    }


    /**
     * 执行POST请求
     */
    public <T> Mono<T> post(String baseUrl, String endpoint, Object requestBody, Class<T> responseType,
                           Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer) {
        return executeRequest(baseUrl, endpoint, HttpMethod.POST, requestBody, responseType, headersCustomizer, null);
    }

    /**
     * POST请求（带供应商ID）
     */
    public <T> Mono<T> post(String baseUrl, String endpoint, Object requestBody, Class<T> responseType,
                           Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer, Long supplierId) {
        return executeRequest(baseUrl, endpoint, HttpMethod.POST, requestBody, responseType, headersCustomizer, supplierId);
    }

    /**
     * 执行PUT请求
     */
    public <T> Mono<T> put(String baseUrl, String endpoint, Object requestBody, Class<T> responseType,
                          Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer) {
        return executeRequest(baseUrl, endpoint, HttpMethod.PUT, requestBody, responseType, headersCustomizer, null);
    }

    /**
     * PUT请求（带供应商ID）
     */
    public <T> Mono<T> put(String baseUrl, String endpoint, Object requestBody, Class<T> responseType,
                          Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer, Long supplierId) {
        return executeRequest(baseUrl, endpoint, HttpMethod.PUT, requestBody, responseType, headersCustomizer, supplierId);
    }

    /**
     * 执行DELETE请求
     */
    public <T> Mono<T> delete(String baseUrl, String endpoint, Class<T> responseType,
                             Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer) {
        return executeRequest(baseUrl, endpoint, HttpMethod.DELETE, null, responseType, headersCustomizer, null);
    }

    /**
     * DELETE请求（带供应商ID）
     */
    public <T> Mono<T> delete(String baseUrl, String endpoint, Class<T> responseType,
                             Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer, Long supplierId) {
        return executeRequest(baseUrl, endpoint, HttpMethod.DELETE, null, responseType, headersCustomizer, supplierId);
    }



    /**
     * 执行HTTP请求的通用方法
     * @param baseUrl 基础URL
     * @param endpoint 接口路径
     * @param method HTTP方法
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @param headersCustomizer 自定义头部设置
     * @param supplierId 供应商ID（用于日志记录）
     * @return
     * @param <T>
     */
    private <T> Mono<T> executeRequest(String baseUrl, String endpoint, HttpMethod method,
                                      Object requestBody, Class<T> responseType,
                                      Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer,
                                      Long supplierId) {

        logger.warn("ExecuteRequest to {}{}",baseUrl,endpoint);
        
        long startTime = System.currentTimeMillis();
        String requestData = requestBody != null ? requestBody.toString() : "";

        String finalUrl = UrlUtil.buildFinalUrl(baseUrl, endpoint);
        logResolvedUri("Final Request URI", finalUrl);
        
        // 使用预创建的WebClient实例，避免重复创建和添加filter
        WebClient.RequestBodySpec requestSpec = this.webClient.method(method).uri(URI.create(finalUrl));


        // 添加请求体（如果有）
        WebClient.RequestHeadersSpec<?> headersSpec;
        String traceId = TraceIdHolder.getTraceId();
        if (requestBody != null && (method == HttpMethod.POST || method == HttpMethod.PUT)) {
            // 自动添加traceId到请求头（用于链路追踪）
            if (traceId != null) {
                headersSpec = requestSpec.bodyValue(requestBody).header(TraceIdHolder.TRACE_ID_HEADER, TraceIdHolder.getOrGenerateTraceId())
                    .header(TraceIdHolder.TRACE_ID_HEADER_LEGACY, TraceIdHolder.getOrGenerateTraceId());
                logger.debug("已添加traceId到HTTP请求头: {} -> {}", endpoint, traceId);
            }else{
                headersSpec = requestSpec.bodyValue(requestBody);
            }
        } else {
            if (traceId != null) {
                 // 自动添加traceId到请求头（用于链路追踪）
                headersSpec = requestSpec.header(TraceIdHolder.TRACE_ID_HEADER, TraceIdHolder.getOrGenerateTraceId())
                    .header(TraceIdHolder.TRACE_ID_HEADER_LEGACY, TraceIdHolder.getOrGenerateTraceId());
                logger.debug("已添加traceId到HTTP请求头: {} -> {}", endpoint, traceId);
            }else {
                headersSpec = requestSpec;
            }
        }
        // 应用自定义头部设置
        if (headersCustomizer != null) {
            headersCustomizer.accept(headersSpec);
        }

        AtomicLong retryCounter = new AtomicLong(0);
        
        // 增加总请求计数
        totalRequests.incrementAndGet();
        
        // 使用信号量限流：获取许可证
        return Mono.fromCallable(() -> {
            try {
                requestSemaphore.acquire(); // 获取许可证，如果没有可用许可证则阻塞
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取请求许可证被中断", e);
            }
        })
        .subscribeOn(customScheduler) // 使用自定义调度器
        .flatMap(ignored -> 
            // 执行实际的HTTP请求
            headersSpec
                .retrieve()
                .toEntity(responseType)
                .map(responseEntity -> {
                    // 成功请求计数
                    successfulRequests.incrementAndGet();

                    // 直接从ResponseEntity获取响应头和响应体
                    long responseTime = System.currentTimeMillis() - startTime;
                    String responseBody = responseEntity.getBody() != null ? JSONUtil.toJsonStr(responseEntity.getBody()) : "";
                    String responseHeadersJson = toJson(responseEntity.getHeaders());
                    String requestParamsJson = parseQueryParamsToJson(endpoint);
                    
                    // 记录API调用日志（包含完整的响应头信息）
                    logApiCall(supplierId,traceId, endpoint, method.name(), requestData,
                              responseBody, responseEntity.getStatusCode().value(), 
                              responseTime, null, 
                              null, // 请求头信息（WebClient限制无法直接获取）
                              responseHeadersJson, // 完整的响应头信息
                              requestParamsJson, retryCounter.get(),
                              sizeInBytes(requestData), sizeInBytes(responseBody));
                    
                    return responseEntity.getBody(); // 返回响应体
                })
                .doOnError(error -> {
                    // 失败请求计数和错误分类
                    failedRequests.incrementAndGet();
                    if (isConnectionError(error)) {
                        connectionErrors.incrementAndGet();
                    }
                    
                    // 错误处理
                    long responseTime = System.currentTimeMillis() - startTime;
                    int statusCode = extractStatusCode(error);
                    String errorMessage = error.getMessage();
                    String requestParamsJson = parseQueryParamsToJson(endpoint);
                    logApiCall(supplierId,traceId, endpoint, method.name(), requestData, "",
                              statusCode, responseTime, errorMessage,
                              null, null, requestParamsJson,
                              retryCounter.get(), sizeInBytes(requestData), 0L);
                })
        )
        .doFinally(signalType -> {
            // 无论成功还是失败，都要释放许可证
            requestSemaphore.release();
        })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(10))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            long attempt = retrySignal.totalRetries() + 1;
                            retryCounter.set(attempt);
                            logger.warn("Retrying request to {} {}, attempt: {} (连接错误: {})", 
                                      method, endpoint, attempt, connectionErrors.get());
                        }))
                .timeout(Duration.ofSeconds(180))
                .onErrorResume(error -> {
                    logger.error("请求失败: {} {}", method, endpoint, error);
                    return Mono.error(new HttpClientException("请求失败: " + error.getMessage(), error));
                });
    }



    /**
     * 执行带重试配置的请求
     */
    public <T> Mono<T> executeWithRetry(String baseUrl, String endpoint, HttpMethod method,
                                       Object requestBody, Class<T> responseType,
                                       Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer,
                                       int retryCount, Duration timeout) {

        logger.warn("ExecuteWithRetry  to {}{}",baseUrl,endpoint);
        WebClient webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        long startTime = System.currentTimeMillis();

        String finalUrl = UrlUtil.buildFinalUrl(baseUrl, endpoint);
        logResolvedUri("Final Request URI (retry)", finalUrl);
        WebClient.RequestBodySpec requestSpec = webClient.method(method).uri(URI.create(finalUrl));


        WebClient.RequestHeadersSpec<?> headersSpec;
        if (requestBody != null && (method == HttpMethod.POST || method == HttpMethod.PUT)) {
            headersSpec = requestSpec.bodyValue(requestBody);
        } else {
            headersSpec = requestSpec;
        }

        // 应用自定义头部设置
        if (headersCustomizer != null) {
            headersCustomizer.accept(headersSpec);
        }

        AtomicLong retryCounter = new AtomicLong(0);
        return headersSpec
                .retrieve()
                .bodyToMono(responseType)
                .doOnSuccess(response -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    String responseBody = response != null ? response.toString() : "";
                    logger.info("API调用成功: {} {}, 响应时间: {}ms", method, endpoint, responseTime);
                    // 此重试方法不记库，可按需后续拓展
                })
                .doOnError(error -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    int statusCode = extractStatusCode(error);
                    String errorMessage = error.getMessage();
                    logger.error("API调用失败: {} {}, 状态码: {}, 响应时间: {}ms, 错误: {}",
                               method, endpoint, statusCode, responseTime, errorMessage);
                })
                .retryWhen(Retry.backoff(retryCount, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(10))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            long attempt = retrySignal.totalRetries() + 1;
                            retryCounter.set(attempt);
                            logger.warn("Retrying request to {} {}, attempt: {}", method, endpoint, attempt);
                        }))
                .timeout(timeout)
                .onErrorResume(error -> {
                    logger.error("Request failed after {} retries: {} {}", retryCount, method, endpoint, error);
                    return Mono.error(new HttpClientException("Request failed: " + error.getMessage(), error));
                });
    }

    /**
     * 批量执行请求
     */
    public <T> Mono<Map<String, T>> executeBatch(Map<String, RequestConfig<T>> requests) {
        return Mono.fromCallable(() -> {
            Map<String, Mono<T>> requestMonos = requests.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> {
                                RequestConfig<T> config = entry.getValue();
                                return executeRequest(config.baseUrl, config.endpoint, config.method,
                                                    config.requestBody, config.responseType, config.headersCustomizer, null);
                            }
                    ));

            return requestMonos;
        }).flatMap(monos ->
                Mono.zip(monos.values(), objects -> {
                    Map<String, T> results = new java.util.HashMap<>();
                    int index = 0;
                    for (String key : monos.keySet()) {
                        results.put(key, (T) objects[index++]);
                    }
                    return results;
                })
        );
    }

    /**
     * 记录API调用日志
     */
    private void logApiCall(Long supplierId,String traceId, String endpoint, String method, String requestData,
                           String responseData, int statusCode, long responseTime, String errorMessage,
                           String requestHeadersJson, String responseHeadersJson, String requestParamsJson,
                           Long retryCount, Long requestSizeBytes, Long responseSizeBytes) {
        try {
            // 如果没有提供supplierId，跳过日志记录
            if (supplierId == null) {
                return;
            }

            ApiCallLog log = new ApiCallLog();
            log.setSupplierId(supplierId);
            log.setTraceId(traceId);
            log.setApiEndpoint(endpoint);
            log.setHttpMethod(method);

            // 压缩阈值配置
            final int COMPRESSION_THRESHOLD = 10000; // 10KB

            // 处理请求体压缩
            boolean requestBodyCompressed = false;
            if (requestData != null && requestData.length() > COMPRESSION_THRESHOLD) {
                try {
                    String compressedRequest = JsonCompressionUtil.compressIfNeeded(
                        requestData, COMPRESSION_THRESHOLD,
                        JsonCompressionUtil.CompressionAlgorithm.GZIP, 6);

                    if (compressedRequest.length() < requestData.length()) {
                        log.setRequestBody(compressedRequest);
                        requestBodyCompressed = true;
                        logger.debug("请求体已压缩: 原长度={}, 压缩后长度={}",
                                   requestData.length(), compressedRequest.length());
                    } else {
                        log.setRequestBody(requestData);
                    }
                } catch (Exception e) {
                    logger.warn("请求体压缩失败，使用原始数据", e);
                    log.setRequestBody(requestData);
                }
            } else {
                log.setRequestBody(requestData);
            }
            log.setRequestBodyCompressed(requestBodyCompressed);

            // 处理响应体压缩
            boolean responseBodyCompressed = false;
            if (responseData != null && responseData.length() > COMPRESSION_THRESHOLD) {
                try {
                    String compressedResponse = JsonCompressionUtil.compressIfNeeded(
                        responseData, COMPRESSION_THRESHOLD,
                        JsonCompressionUtil.CompressionAlgorithm.GZIP, 6);

                    if (compressedResponse.length() < responseData.length()) {
                        log.setResponseBody(compressedResponse);
                        responseBodyCompressed = true;
                        logger.debug("响应体已压缩: 原长度={}, 压缩后长度={}",
                                   responseData.length(), compressedResponse.length());
                    } else {
                        log.setResponseBody(responseData);
                    }
                } catch (Exception e) {
                    logger.warn("响应体压缩失败，使用原始数据", e);
                    log.setResponseBody(responseData);
                }
            } else {
                log.setResponseBody(responseData);
            }
            log.setResponseBodyCompressed(responseBodyCompressed);

            log.setResponseStatus(statusCode);
            log.setResponseTimeMs(responseTime);
            log.setErrorMessage(errorMessage);
            log.setIsSuccess(statusCode >= 200 && statusCode < 300);


            log.setChannel("api");

            //从requestHeadersJson ｜requestParamsJson ｜ requestData 提取 业务类型相关的字段  关键字（action, businessType）
            String businessType = extractBusinessType(requestHeadersJson, requestParamsJson, requestData);
            log.setBusinessType(businessType != null ? businessType : "http_client");


            // 额外补充字段
            log.setRequestHeaders(requestHeadersJson);
            log.setResponseHeaders(responseHeadersJson);
            log.setRequestParams(requestParamsJson);
            log.setRetryCount(retryCount);
            log.setRequestSizeBytes(requestSizeBytes);
            log.setResponseSizeBytes(responseSizeBytes);

            // 从请求头提取 User-Agent / Client-IP
            String userAgent = extractUserAgent(requestHeadersJson);
            String clientIp = resolveClientIp(requestHeadersJson);
            log.setUserAgent(userAgent);
            log.setClientIp(clientIp);



            // 简单错误码（如需要更精细可在调用方传入）
            if (errorMessage != null && !errorMessage.isEmpty()) {
                // 暂不解析具体错误码，字段保留为空或后续扩展
            }

            // 异步保存日志，不影响主流程
            CompletableFuture.runAsync(() -> {
                try {
                    apiCallLogRepository.save(log);
                } catch (Exception error) {
                    logger.warn("保存API调用日志失败 ", error);
                }
            });

        } catch (Exception e) {
            logger.warn("创建API调用日志失败", e);
        }
    }



    /**
     * 对象转换为JSON字符串
     */
    private String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            logger.info("对象转换为JSON失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析URL中的查询参数并转换为JSON字符串
     * @param endpoint 完整的URL或路径，可能包含查询参数
     * @return 查询参数的JSON字符串表示，或null如果没有查询参数
     */
    private String parseQueryParamsToJson(String endpoint) {
        try {
            if (endpoint == null) return null;
            int idx = endpoint.indexOf('?');
            if (idx < 0 || idx == endpoint.length() - 1) return null;
            String query = endpoint.substring(idx + 1);
            Map<String, String> map = new LinkedHashMap<>();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                if (pair.isEmpty()) continue;
                int eq = pair.indexOf('=');
                if (eq < 0) {
                    map.put(urlDecode(pair), null);
                } else {
                    String key = urlDecode(pair.substring(0, eq));
                    String val = urlDecode(pair.substring(eq + 1));
                    map.put(key, val);
                }
            }
            return toJson(map);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * URL解码
     */
    private String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }


    /**
     * 计算字符串的字节大小（UTF-8编码）
     */
    private long sizeInBytes(String s) {
        if (s == null) return 0L;
        return s.getBytes(StandardCharsets.UTF_8).length;
    }



    /**
     * 从 HTTP 头中提取 User-Agent 字段
     * @param headersJson
     * @return
     */
    private String extractUserAgent(String headersJson) {
        try {
            if (headersJson == null) return null;
            Map<?,?> map = MAPPER.readValue(headersJson, Map.class);

            // 支持多种 User-Agent 头名称变体
            String[] userAgentKeys = {"User-Agent", "user-agent", "USER-AGENT", "UserAgent", "useragent"};

            for (String key : userAgentKeys) {
                Object ua = map.get(key);
                if (ua != null) {
                    if (ua instanceof String) return (String) ua;
                    if (ua instanceof java.util.List<?> list && !list.isEmpty()) return String.valueOf(list.get(0));
                    return String.valueOf(ua);
                }
            }
            return null;
        } catch (Exception e) {
            logger.warn("[extractUserAgent] 提取User-Agent失败, headersJson: {}", headersJson, e);
            return null;
        }
    }

    /**
     * 从 HTTP 头中解析客户端 IP 地址
     * 支持常见的代理头和客户端IP头
     * @param headersJson
     * @return
     */
    private String resolveClientIp(String headersJson) {
        try {
            if (headersJson == null) return null;
            Map<?,?> map = MAPPER.readValue(headersJson, Map.class);

            // 按优先级检查各种客户端IP头
            String[] ipHeaders = {
                "X-Forwarded-For", "x-forwarded-for",
                "X-Real-IP", "x-real-ip",
                "Client-IP", "client-ip",
                "X-Client-IP", "x-client-ip",
                "X-Originating-IP", "x-originating-ip",
                "CF-Connecting-IP", "cf-connecting-ip",
                "True-Client-IP", "true-client-ip"
            };

            for (String headerKey : ipHeaders) {
                Object v = map.get(headerKey);
                if (v == null) continue;

                String val;
                if (v instanceof java.util.List<?> list && !list.isEmpty()) {
                    val = String.valueOf(list.get(0));
                } else {
                    val = String.valueOf(v);
                }

                if (val != null && !val.isEmpty() && !"unknown".equalsIgnoreCase(val)) {
                    // 处理多个IP的情况（用逗号分隔，取第一个）
                    int comma = val.indexOf(',');
                    String clientIp = comma > 0 ? val.substring(0, comma).trim() : val.trim();
                    if (!clientIp.isEmpty() && !"unknown".equalsIgnoreCase(clientIp)) {
                        return clientIp;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            logger.warn("[resolveClientIp] 提取Client-IP失败, headersJson: {}", headersJson, e);
            return null;
        }
    }
    
    /**
     * 判断是否为可重试的错误
     */
    private boolean isRetryableError(Throwable error) {
        if (error instanceof WebClientResponseException) {
            WebClientResponseException webClientError = (WebClientResponseException) error;
            HttpStatusCode statusCode = webClientError.getStatusCode();
            int status = statusCode.value();
            
            // 重试服务器错误和部分客户端错误
            return statusCode.is5xxServerError() || 
                   status == HttpStatus.REQUEST_TIMEOUT.value() ||  // 408
                   status == HttpStatus.TOO_MANY_REQUESTS.value();  // 429
        }
        
        // 重试网络相关错误
        return error instanceof java.net.ConnectException ||
               error instanceof java.net.SocketTimeoutException ||
               error instanceof java.io.IOException;
    }
    
    /**
     * 提取HTTP状态码
     */
    private int extractStatusCode(Throwable error) {
        if (error instanceof WebClientResponseException) {
            return ((WebClientResponseException) error).getRawStatusCode();
        }
        return 0; // 网络错误等非HTTP错误
    }
    
    
    /**
     * 截断数据以避免日志过大
     */
    private String truncateData(String data, int maxLength) {
        if (data == null) return null;
        if (data.length() <= maxLength) return data;
        return data.substring(0, maxLength) + "... [truncated]";
    }

    /**
     * 打印已解析的 URI 主机与端口，便于排查网络连通性问题。
     */
    private void logResolvedUri(String label, String url) {
        try {
            URI dbg = java.net.URI.create(url);
            int port = dbg.getPort() > 0 ? dbg.getPort() : ("https".equalsIgnoreCase(dbg.getScheme()) ? 443 : 80);
            logger.warn("{}: {} (host: {}, port: {})", label, url, dbg.getHost(), port);
        } catch (Exception e) {
            logger.warn("{}: {} (无法解析: {})", label, url, e.getMessage());
        }
    }

    /**
     * 请求配置类
     */
    public static class RequestConfig<T> {
        public final String baseUrl;
        public final String endpoint;
        public final HttpMethod method;
        public final Object requestBody;
        public final Class<T> responseType;
        public final Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer;

        public RequestConfig(String baseUrl, String endpoint, HttpMethod method,
                             Object requestBody, Class<T> responseType,
                             Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer) {
            this.baseUrl = baseUrl;
            this.endpoint = endpoint;
            this.method = method;
            this.requestBody = requestBody;
            this.responseType = responseType;
            this.headersCustomizer = headersCustomizer;
        }
    }
    
    /**
     * 判断是否为连接相关错误
     */
    private boolean isConnectionError(Throwable error) {
        return error instanceof ConnectException ||
               error instanceof java.net.SocketTimeoutException ||
               error instanceof java.nio.channels.ClosedChannelException ||
               (error instanceof WebClientResponseException && 
                error.getMessage() != null && 
                (error.getMessage().contains("Connection prematurely closed") ||
                 error.getMessage().contains("executor not accepting a task")));
    }

    /**
     * 获取监控指标信息
     */
    public String getMetrics() {
        long total = totalRequests.get();
        long success = successfulRequests.get();
        long failed = failedRequests.get();
        long connErrors = connectionErrors.get();
        
        double successRate = total > 0 ? (double) success / total * 100 : 0;
        int availablePermits = requestSemaphore.availablePermits();
        
        return String.format(
            "HTTP客户端监控指标 - 总请求:%d, 成功:%d, 失败:%d, 连接错误:%d, 成功率:%.2f%%, 可用许可证:%d/50",
            total, success, failed, connErrors, successRate, availablePermits
        );
    }

    /**
     * 重置监控指标（用于测试或定期重置）
     */
    public void resetMetrics() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        connectionErrors.set(0);
        logger.info("HTTP客户端监控指标已重置");
    }

    /**
     * 获取当前可用的并发许可证数量
     */
    public int getAvailablePermits() {
        return requestSemaphore.availablePermits();
    }

    /**
     * 检查连接池和调度器健康状态
     */
    public boolean isHealthy() {
        // 检查是否有可用的许可证
        boolean hasPermits = requestSemaphore.availablePermits() > 0;
        
        // 检查成功率是否在合理范围内（如果有请求的话）
        long total = totalRequests.get();
        boolean goodSuccessRate = true;
        if (total > 10) { // 至少有10个请求才计算成功率
            double successRate = (double) successfulRequests.get() / total;
            goodSuccessRate = successRate > 0.5; // 成功率大于50%
        }
        
        return hasPermits && goodSuccessRate;
    }

    /**
     * 从请求头、请求参数或请求体中提取业务类型
     * 优先级：请求头 > 请求参数 > 请求体
     * 
     * @param requestHeadersJson 请求头JSON字符串
     * @param requestParamsJson 请求参数JSON字符串
     * @param requestBodyJson 请求体JSON字符串
     * @return 业务类型，如果未找到则返回null
     */
    private String extractBusinessType(String requestHeadersJson, String requestParamsJson, String requestBodyJson) {
        try {
            // 定义业务类型相关的关键字（按优先级排序）
            String[] businessTypeKeys = {"businessType", "business_type", "action", "apiAction", "api_action", "operation", "method"};
            
            // 1. 优先从请求头中提取
            String businessType = extractFromJson(requestHeadersJson, businessTypeKeys);
            if (businessType != null) {
                logger.debug("[extractBusinessType] 从请求头提取到业务类型: {}", businessType);
                return businessType;
            }
            
            // 2. 从请求参数中提取
            businessType = extractFromJson(requestParamsJson, businessTypeKeys);
            if (businessType != null) {
                logger.debug("[extractBusinessType] 从请求参数提取到业务类型: {}", businessType);
                return businessType;
            }
            
            // 3. 从请求体中提取
            businessType = extractFromJson(requestBodyJson, businessTypeKeys);
            if (businessType != null) {
                logger.debug("[extractBusinessType] 从请求体提取到业务类型: {}", businessType);
                return businessType;
            }
            
            return null;
        } catch (Exception e) {
            logger.warn("[extractBusinessType] 提取业务类型失败", e);
            return null;
        }
    }

    /**
     * 从JSON字符串中提取指定关键字的值
     * 
     * @param jsonStr JSON字符串
     * @param keys 要查找的关键字数组（按优先级排序）
     * @return 找到的第一个非空值，如果未找到则返回null
     */
    private String extractFromJson(String jsonStr, String[] keys) {
        try {
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                return null;
            }
            
            Map<?, ?> map = MAPPER.readValue(jsonStr, Map.class);
            
            // 按优先级遍历关键字
            for (String key : keys) {
                Object value = map.get(key);
                if (value != null) {
                    String strValue = String.valueOf(value).trim();
                    if (!strValue.isEmpty() && !"null".equalsIgnoreCase(strValue)) {
                        return strValue;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("[extractFromJson] JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

}
