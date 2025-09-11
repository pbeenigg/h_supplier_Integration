package com.heytrip.hotel.supplier.client;

import com.heytrip.hotel.supplier.entity.ApiCallLog;
import com.heytrip.hotel.supplier.repository.ApiCallLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

        WebClient webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        long startTime = System.currentTimeMillis();
        String requestData = requestBody != null ? requestBody.toString() : "";

        WebClient.RequestBodySpec requestSpec = webClient.method(method).uri(endpoint);

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

        return headersSpec
                .retrieve()
                .bodyToMono(responseType)
                .doOnSuccess(response -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    // 直接使用传入的supplierId记录日志
                    logApiCall(supplierId, endpoint, method.name(), requestData,
                              response != null ? response.toString() : "",
                              HttpStatus.OK.value(), responseTime, null);
                })
                .doOnError(error -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    int statusCode = extractStatusCode(error);
                    String errorMessage = error.getMessage();
                    // 直接使用传入的supplierId记录日志
                    logApiCall(supplierId, endpoint, method.name(), requestData, "",
                              statusCode, responseTime, errorMessage);
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(10))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                                logger.warn("Retrying request to {} {}, attempt: {}",
                                        method, endpoint, retrySignal.totalRetries() + 1)))
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(error -> {
                    logger.error("Request failed after retries: {} {}", method, endpoint, error);
                    return Mono.error(new HttpClientException("Request failed: " + error.getMessage(), error));
                });
    }


    /**
     * 执行带重试配置的请求
     */
    public <T> Mono<T> executeWithRetry(String baseUrl, String endpoint, HttpMethod method,
                                       Object requestBody, Class<T> responseType,
                                       Consumer<WebClient.RequestHeadersSpec<?>> headersCustomizer,
                                       int retryCount, Duration timeout) {

        WebClient webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        long startTime = System.currentTimeMillis();
        String requestData = requestBody != null ? requestBody.toString() : "";

        WebClient.RequestBodySpec requestSpec = webClient.method(method).uri(endpoint);

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

        return headersSpec
                .retrieve()
                .bodyToMono(responseType)
                .doOnSuccess(response -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    // 注意：executeWithRetry方法没有supplierId参数，所以不记录日志
                    logger.info("API调用成功: {} {}, 响应时间: {}ms", method, endpoint, responseTime);
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
                        .doBeforeRetry(retrySignal ->
                                logger.warn("Retrying request to {} {}, attempt: {}",
                                        method, endpoint, retrySignal.totalRetries() + 1)))
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
                           String responseData, int statusCode, long responseTime, String errorMessage) {
        try {
            // 如果没有提供supplierId，跳过日志记录
            if (supplierId == null) {
                return;
            }

            ApiCallLog log = new ApiCallLog();
            log.setSupplierId(supplierId);
            log.setApiEndpoint(endpoint);
            log.setHttpMethod(method);
            log.setRequestBody(truncateData(requestData, 4000));
            log.setResponseBody(truncateData(responseData, 4000));
            log.setResponseStatus(statusCode);
            log.setResponseTimeMs(responseTime);
            log.setErrorMessage(errorMessage);
            log.setIsSuccess(statusCode >= 200 && statusCode < 300);
            log.setBusinessType("api_call");
            log.setChannel("HTTP_CLIENT");
            
            // 异步保存日志，不影响主流程
            CompletableFuture.runAsync(() -> {
                try {
                    apiCallLogRepository.save(log);
                } catch (Exception error) {
                    logger.warn("Failed to save API call log", error);
                }
            });
                    
        } catch (Exception e) {
            logger.warn("Failed to create API call log", e);
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
     * HTTP客户端异常
     */
    public static class HttpClientException extends RuntimeException {
        public HttpClientException(String message) {
            super(message);
        }
        
        public HttpClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
