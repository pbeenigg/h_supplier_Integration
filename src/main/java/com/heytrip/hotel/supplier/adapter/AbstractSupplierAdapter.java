package com.heytrip.hotel.supplier.adapter;

import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import com.heytrip.hotel.supplier.dto.request.HotelSearchRequest;
import com.heytrip.hotel.supplier.dto.request.CreateOrderRequest;
import com.heytrip.hotel.supplier.dto.response.HotelSearchResponse;
import com.heytrip.hotel.supplier.dto.response.CreateOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;

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
    
    protected SupplierConfig supplierConfig;
    protected WebClient webClient;
    
    /**
     * 初始化适配器
     */
    protected void initialize() {
        loadSupplierConfig();
        initializeWebClient();
    }
    
    /**
     * 加载供应商配置
     */
    protected void loadSupplierConfig() {
        Optional<SupplierConfig> config = supplierConfigRepository.findBySupplierNameAndIsActive(getSupplierName(), true);
        if (config.isPresent()) {
            this.supplierConfig = config.get();
            logger.info("Loaded config for supplier: {}", getSupplierName());
        } else {
            logger.warn("No active config found for supplier: {}", getSupplierName());
            throw new RuntimeException("Supplier config not found: " + getSupplierName());
        }
    }
    
    /**
     * 初始化WebClient
     */
    protected void initializeWebClient() {
        if (supplierConfig != null) {
            this.webClient = webClientBuilder
                    .baseUrl(supplierConfig.getApiBaseUrl())
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                    .build();
            logger.info("Initialized WebClient for supplier: {} with base URL: {}", 
                    getSupplierName(), supplierConfig.getApiBaseUrl());
        }
    }
    
    /**
     * 执行HTTP请求并处理重试
     */
    protected <T> Mono<T> executeWithRetry(Mono<T> request) {
        return request
                .timeout(Duration.ofMillis(getTimeoutMs()))
                .retryWhen(Retry.backoff(getRetryCount(), Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(10))
                        .doBeforeRetry(retrySignal -> 
                                logger.warn("Retrying request for supplier: {}, attempt: {}", 
                                        getSupplierName(), retrySignal.totalRetries() + 1)))
                .doOnError(error -> 
                        logger.error("Request failed for supplier: {} after {} retries", 
                                getSupplierName(), getRetryCount(), error));
    }
    
    @Override
    public boolean isEnabled() {
        return supplierConfig != null && supplierConfig.getIsActive();
    }
    
    @Override
    public long getTimeoutMs() {
        return supplierConfig != null ? supplierConfig.getTimeoutMs() : 30000L;
    }
    
    @Override
    public int getRetryCount() {
        return supplierConfig != null ? supplierConfig.getRetryCount() : 3;
    }
    
    @Override
    public Mono<Boolean> healthCheck() {
        if (!isEnabled()) {
            return Mono.just(false);
        }
        
        return executeWithRetry(
                webClient.get()
                        .uri("/health")
                        .retrieve()
                        .toBodilessEntity()
                        .map(response -> response.getStatusCode().is2xxSuccessful())
        ).onErrorReturn(false);
    }
    
    /**
     * 构建认证头信息
     * 子类可以重写此方法实现特定的认证逻辑
     */
    protected abstract void addAuthHeaders(HttpHeaders headers);
    
    /**
     * 验证搜索请求参数
     */
    protected void validateSearchRequest(HotelSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Search request cannot be null");
        }
        if (request.getCity() == null || request.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        if (request.getCheckInDate() == null) {
            throw new IllegalArgumentException("Check-in date is required");
        }
        if (request.getCheckOutDate() == null) {
            throw new IllegalArgumentException("Check-out date is required");
        }
        if (request.getCheckInDate().isAfter(request.getCheckOutDate())) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }
    }
    
    /**
     * 验证订单请求参数
     */
    protected void validateOrderRequest(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request cannot be null");
        }
        if (request.getHotelId() == null || request.getHotelId().trim().isEmpty()) {
            throw new IllegalArgumentException("Hotel ID is required");
        }
        if (request.getRoomId() == null || request.getRoomId().trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID is required");
        }
        if (request.getGuestName() == null || request.getGuestName().trim().isEmpty()) {
            throw new IllegalArgumentException("Guest name is required");
        }
    }
}
