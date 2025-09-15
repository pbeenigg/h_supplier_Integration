package com.heytrip.hotel.supplier.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.entity.ApiCallLog;
import com.heytrip.hotel.supplier.exception.HttpClientException;
import com.heytrip.hotel.supplier.repository.ApiCallLogRepository;
import com.heytrip.hotel.supplier.utils.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
        // 捕获请求/响应头用于日志
        AtomicReference<String> capturedRequestHeaders = new AtomicReference<>(null);
        AtomicReference<String> capturedResponseHeaders = new AtomicReference<>(null);
        WebClient webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
                    try { capturedRequestHeaders.set(toJson(req.headers())); } catch (Exception ignored) {}
                    return Mono.just(req);
                }))
                .filter(ExchangeFilterFunction.ofResponseProcessor(resp -> {
                    try { capturedResponseHeaders.set(toJson(resp.headers().asHttpHeaders())); } catch (Exception ignored) {}
                    return Mono.just(resp);
                }))
                .build();

        long startTime = System.currentTimeMillis();
        String requestData = requestBody != null ? requestBody.toString() : "";

        String finalUrl = UrlUtil.buildFinalUrl(baseUrl, endpoint);
        logResolvedUri("Final Request URI", finalUrl);
        WebClient.RequestBodySpec requestSpec = webClient.method(method).uri(URI.create(finalUrl));


        // 添加请求体（如果有）
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
                    String responseBody = response != null ? toJson(response) : "";
                    String requestParamsJson = parseQueryParamsToJson(endpoint);
                    logApiCall(supplierId, endpoint, method.name(), requestData,
                              responseBody,
                              HttpStatus.OK.value(), responseTime, null,
                              capturedRequestHeaders.get(), capturedResponseHeaders.get(), requestParamsJson,
                              retryCounter.get(),
                              sizeInBytes(requestData), sizeInBytes(responseBody));
                })
                .doOnError(error -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    int statusCode = extractStatusCode(error);
                    String errorMessage = error.getMessage();
                    String requestParamsJson = parseQueryParamsToJson(endpoint);
                    String errorCode = error.getClass().getSimpleName();
                    logApiCall(supplierId, endpoint, method.name(), requestData, "",
                              statusCode, responseTime, errorMessage,
                              capturedRequestHeaders.get(), capturedResponseHeaders.get(), requestParamsJson,
                              retryCounter.get(),
                              sizeInBytes(requestData), 0L);
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(10))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            long attempt = retrySignal.totalRetries() + 1;
                            retryCounter.set(attempt);
                            logger.warn("Retrying request to {} {}, attempt: {}", method, endpoint, attempt);
                        }))
                .timeout(Duration.ofSeconds(30))
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
    private void logApiCall(Long supplierId, String endpoint, String method, String requestData,
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
            log.setApiEndpoint(endpoint);
            log.setHttpMethod(method);
            log.setRequestBody(requestData);
            log.setResponseBody(responseData);
            log.setResponseStatus(statusCode);
            log.setResponseTimeMs(responseTime);
            log.setErrorMessage(errorMessage);
            log.setIsSuccess(statusCode >= 200 && statusCode < 300);
            log.setBusinessType("api_call");
            log.setChannel("HTTP_CLIENT");

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
            Object ua = map.get("User-Agent");
            if (ua == null) ua = map.get("user-agent");
            if (ua instanceof String) return (String) ua;
            if (ua instanceof java.util.List<?> list && !list.isEmpty()) return String.valueOf(list.get(0));
            return ua != null ? String.valueOf(ua) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 HTTP 头中解析客户端 IP 地址
     * 支持常见的代理头：X-Forwarded-For, X-Real-IP
     * @param headersJson
     * @return
     */
    private String resolveClientIp(String headersJson) {
        try {
            if (headersJson == null) return null;
            Map<?,?> map = MAPPER.readValue(headersJson, Map.class);
            String[] keys = new String[]{"X-Forwarded-For","x-forwarded-for","X-Real-IP","x-real-ip"};
            for (String k : keys) {
                Object v = map.get(k);
                if (v == null) continue;
                String val;
                if (v instanceof java.util.List<?> list && !list.isEmpty()) val = String.valueOf(list.get(0));
                else val = String.valueOf(v);
                if (val != null && !val.isEmpty()) {
                    int comma = val.indexOf(',');
                    return comma > 0 ? val.substring(0, comma).trim() : val.trim();
                }
            }
            return null;
        } catch (Exception e) {
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
    


}
