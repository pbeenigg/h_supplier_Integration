package com.heytrip.hotel.supplier.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.dto.supplier.SupplierAuth;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import com.heytrip.hotel.supplier.service.SupplierHealthCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * 供应商适配器抽象基类
 * 提供通用的功能实现和配置管理
 * 
 * @author  Pax
 */
public abstract class AbstractSupplierAdapter implements SupplierAdapter {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    
    @Autowired
    protected SupplierConfigRepository supplierConfigRepository;
    
    @Autowired
    protected WebClient.Builder webClientBuilder;
    
    @Autowired
    protected SupplierHealthCheckService healthCheckService;
    
    protected SupplierConfig supplierConfig;
    protected WebClient webClient;
    protected Semaphore rateLimitSemaphore;
    protected Semaphore concurrencyLimitSemaphore;
    
    // JSON处理器
    protected final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 初始化适配器
     */
    protected void initialize() {
        loadSupplierConfig();
        initializeWebClient();
        initializeLimiters();
    }
    
    /**
     * 加载供应商配置
     * 子类必须重写此方法提供供应商标识符
     */
    protected void loadSupplierConfig() {
        String supplierCode = getSupplierCode();
        if (supplierCode == null) {
            throw new RuntimeException("供应商标识符不能为空，请在子类中重写getSupplierIdentifier()方法");
        }
        
        // 优先按供应商代码查找，其次按供应商名称查找
        Optional<SupplierConfig> config = supplierConfigRepository.findBySupplierCodeAndIsActive(supplierCode, true);
        if (!config.isPresent()) {
            config = supplierConfigRepository.findBySupplierNameAndIsActiveTrue(supplierCode);
        }
        
        if (config.isPresent()) {
            this.supplierConfig = config.get();
            logger.info("已加载供应商配置: {} (ID: {}), Code: {}", supplierConfig.getSupplierName(), supplierConfig.getId(),supplierConfig.getSupplierCode());
        } else {
            logger.warn("未找到启用的供应商配置: {}", supplierCode);
            throw new RuntimeException("供应商配置未找到: " + supplierCode);
        }
    }
    
    /**
     * 获取供应商标识符（供应商代码或名称）
     * 子类必须重写此方法提供唯一的供应商标识
     */
    public abstract String getSupplierCode();
    
    /**
     * 初始化WebClient
     */
    protected void initializeWebClient() {
        if (supplierConfig != null) {
            this.webClient = webClientBuilder
                    .baseUrl(supplierConfig.getApiBaseUrl())
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                    .build();
            logger.info("已初始化WebClient，供应商: {}，基础URL: {}", 
                    getSupplierName(), supplierConfig.getApiBaseUrl());
        }
    }
    
    /**
     * 初始化限流器和并发控制
     */
    protected void initializeLimiters() {
        if (supplierConfig != null) {
            // 初始化并发限制信号量
            int maxConcurrent = supplierConfig.getMaxConcurrentRequests() != null ? 
                supplierConfig.getMaxConcurrentRequests() : 10;
            this.concurrencyLimitSemaphore = new Semaphore(maxConcurrent);
            
            // 初始化速率限制信号量（每秒允许的请求数）
            int rateLimit = supplierConfig.getRateLimitPerSecond() != null ? 
                supplierConfig.getRateLimitPerSecond() : 5;
            this.rateLimitSemaphore = new Semaphore(rateLimit);
            
            logger.info("已初始化限流器，供应商: {}，最大并发: {}，每秒限制: {}", 
                    getSupplierName(), maxConcurrent, rateLimit);
        }
    }


    @Override
    public String getSupplierName() {
        return supplierConfig != null ? supplierConfig.getSupplierName() : null;
    }

    @Override
    public Long getSupplierId() {
        return supplierConfig != null ? supplierConfig.getId() : null;
    }

    @Override
    public boolean isEnabled() {
        return supplierConfig != null && supplierConfig.getIsActive();
    }
    
    @Override
    public long getTimeoutMs() {
        return supplierConfig != null && supplierConfig.getTimeoutMs() != null ? 
            supplierConfig.getTimeoutMs() : 30000L;
    }
    
    @Override
    public int getRetryCount() {
        return supplierConfig != null && supplierConfig.getRetryCount() != null ? 
            supplierConfig.getRetryCount() : 3;
    }

    /**
     * 获取最大并发请求数
     */
    public Integer getMaxConcurrentRequests() {
        return supplierConfig != null ? supplierConfig.getMaxConcurrentRequests() : 10;
    }
    
    /**
     * 获取每秒速率限制
     */
    public Integer getRateLimitPerSecond() {
        return supplierConfig != null ? supplierConfig.getRateLimitPerSecond() : 5;
    }
    
    /**
     * 获取供应商描述
     */
    public String getSupplierDescription() {
        return supplierConfig != null ? supplierConfig.getDescription() : null;
    }
    
    /**
     * 获取联系信息
     */
    public String getContactInfo() {
        return supplierConfig != null ? supplierConfig.getContactInfo() : null;
    }
    


    /**
     * 从供应商配置中提取认证参数（Cache缓存）
     */
    @Cacheable(value = "supplierAuth", key = "#supplierName")
    protected SupplierAuth extractFromAuthConfig(String supplierName) {
        logger.debug("开始解析认证配置（将被缓存）");

        try {
            if (supplierConfig != null && supplierConfig.getAuthConfig() != null) {
                String authConfig = supplierConfig.getAuthConfig();
                logger.debug("开始解析认证配置: {}", authConfig);

                // 使用Jackson ObjectMapper解析JSON配置
                SupplierAuth supplierAuth = objectMapper.readValue(authConfig, SupplierAuth.class);

                // 验证必要的认证信息
                if (supplierAuth != null) {
                    logger.info("成功解析认证配置 - 用户名: {}, AppId: {}",
                            supplierAuth.getUsername(), supplierAuth.getAppId());

                    // 对于QTECH，主要使用用户名和密码认证
                    if (supplierAuth.getUsername() == null || supplierAuth.getPassword() == null) {
                        logger.warn("QTECH认证配置缺少必要的用户名或密码信息");
                    }

                    logger.debug("认证配置解析成功，已加入Cache");
                    return supplierAuth;
                } else {
                    logger.error("认证配置解析结果为空");
                }
            } else {
                logger.warn("供应商配置或认证配置为空");
            }
        } catch (Exception e) {
            logger.error("提取认证配置失败", e);
        }
        throw new RuntimeException("无法提取有效的认证配置");
    }

    /**
     * 清除认证配置缓存（用于配置更新时）
     */
    @CacheEvict(value = "supplierAuth", key = "#supplierName")
    public void clearAuthConfigCache(String supplierName) {
        logger.info("认证配置缓存已清除（Spring Cache）");
    }


    /**
     * 执行HTTP请求并处理重试、限流和并发控制
     */
    protected <T> Mono<T> executeWithRetry(Mono<T> request) {
        return Mono.fromCallable(() -> {
                    // 获取并发控制许可
                    try {
                        concurrencyLimitSemaphore.acquire();
                        logger.debug("获得并发许可，供应商: {}", getSupplierName());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("获取并发许可被中断", e);
                    }
                    return null;
                })
                .then(Mono.fromCallable(() -> {
                    // 获取速率限制许可
                    try {
                        rateLimitSemaphore.acquire();
                        logger.debug("获得速率限制许可，供应商: {}", getSupplierName());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("获取速率限制许可被中断", e);
                    }
                    return null;
                }))
                .then(request)
                .timeout(Duration.ofMillis(getTimeoutMs()))
                .retryWhen(Retry.backoff(getRetryCount(), Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(10))
                        .doBeforeRetry(retrySignal ->
                                logger.warn("重试请求，供应商: {}，尝试次数: {}",
                                        getSupplierName(), retrySignal.totalRetries() + 1)))
                .doOnError(error ->
                        logger.error("请求失败，供应商: {}，重试{}次后仍失败",
                                getSupplierName(), getRetryCount(), error))
                .doFinally(signalType -> {
                    // 释放许可
                    concurrencyLimitSemaphore.release();
                    rateLimitSemaphore.release();
                    logger.debug("释放许可，供应商: {}，信号类型: {}", getSupplierName(), signalType);
                });
    }

    /**
     * 执行带限流控制的请求
     */
    protected <T> Mono<T> executeWithLimits(Mono<T> request) {
        return executeWithRetry(request);
    }


    @Override
    public Mono<Boolean> healthCheck() {
        if (!isEnabled()) {
            logger.warn("供应商未启用，健康检查返回false: {}", getSupplierName());
            return Mono.just(false);
        }
        
        return executeWithLimits(
                webClient.get()
                        .uri("/monitor/health")
                        .retrieve()
                        .toBodilessEntity()
                        .map(response -> {
                            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
                            logger.debug("健康检查结果，供应商: {}，状态: {}", getSupplierName(), isHealthy);
                            return isHealthy;
                        })
        ).onErrorReturn(false);
    }
    
    /**
     * 执行供应商健康检查并记录日志
     */
    public Mono<Boolean> performHealthCheckWithLogging() {
        if (healthCheckService != null && supplierConfig != null) {
            return Mono.fromFuture(healthCheckService.performHealthCheck(supplierConfig))
                    .map(healthLog -> {
                        boolean isHealthy = healthLog.getHealthStatus().equals("HEALTHY");
                        logger.info("执行健康检查并记录日志，供应商: {}，结果: {}", 
                                getSupplierName(), healthLog.getHealthStatus());
                        return isHealthy;
                    })
                    .onErrorReturn(false);
        } else {
            return healthCheck();
        }
    }


}
