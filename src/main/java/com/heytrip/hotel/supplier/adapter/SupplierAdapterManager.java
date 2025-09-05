package com.heytrip.hotel.supplier.adapter;

import com.heytrip.hotel.supplier.dto.request.HotelSearchRequest;
import com.heytrip.hotel.supplier.dto.request.CreateOrderRequest;
import com.heytrip.hotel.supplier.dto.response.HotelSearchResponse;
import com.heytrip.hotel.supplier.dto.response.CreateOrderResponse;
import com.heytrip.hotel.supplier.dto.response.HotelInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 供应商适配器管理器
 * 负责管理所有供应商适配器，协调多供应商查询和聚合结果
 * 
 * @author  Pax
 */
@Component
public class SupplierAdapterManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SupplierAdapterManager.class);
    
    @Autowired
    private List<SupplierAdapter> supplierAdapters;
    
    private List<SupplierAdapter> enabledAdapters;
    
    @PostConstruct
    public void initialize() {
        // 过滤启用的适配器并按优先级排序
        enabledAdapters = supplierAdapters.stream()
                .filter(SupplierAdapter::isEnabled)
                .sorted(Comparator.comparingInt(SupplierAdapter::getPriority))
                .collect(Collectors.toList());
        
        logger.info("Initialized {} enabled supplier adapters", enabledAdapters.size());
        enabledAdapters.forEach(adapter -> 
                logger.info("Enabled adapter: {} with priority: {}", 
                        adapter.getSupplierName(), adapter.getPriority()));
    }
    
    /**
     * 聚合搜索所有支持的供应商
     * @param request 搜索请求
     * @return 聚合的搜索结果
     */
    public Mono<HotelSearchResponse> searchHotelsFromAllSuppliers(HotelSearchRequest request) {
        logger.info("Starting hotel search across all suppliers for city: {}", request.getCity());
        
        List<SupplierAdapter> supportedAdapters = enabledAdapters.stream()
                .filter(adapter -> adapter.supportsCity(request.getCity()))
                .collect(Collectors.toList());
        
        if (supportedAdapters.isEmpty()) {
            logger.warn("No suppliers support city: {}", request.getCity());
            return Mono.just(createEmptyResponse("No suppliers available for city: " + request.getCity()));
        }
        
        logger.info("Found {} suppliers supporting city: {}", supportedAdapters.size(), request.getCity());
        
        return Flux.fromIterable(supportedAdapters)
                .flatMap(adapter -> searchFromSupplier(adapter, request))
                .collectList()
                .map(this::aggregateSearchResults)
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(error -> {
                    logger.error("Error during multi-supplier search", error);
                    return Mono.just(createErrorResponse("Search failed: " + error.getMessage()));
                });
    }
    
    /**
     * 从指定供应商搜索酒店
     * @param supplierName 供应商名称
     * @param request 搜索请求
     * @return 搜索结果
     */
    public Mono<HotelSearchResponse> searchHotelsFromSupplier(String supplierName, HotelSearchRequest request) {
        logger.info("Searching hotels from specific supplier: {}", supplierName);
        
        SupplierAdapter adapter = findAdapterByName(supplierName);
        if (adapter == null) {
            return Mono.just(createErrorResponse("Supplier not found: " + supplierName));
        }
        
        if (!adapter.supportsCity(request.getCity())) {
            return Mono.just(createErrorResponse("Supplier " + supplierName + " does not support city: " + request.getCity()));
        }
        
        return searchFromSupplier(adapter, request);
    }
    
    /**
     * 创建订单（使用指定供应商）
     * @param supplierName 供应商名称
     * @param request 订单请求
     * @return 订单响应
     */
    public Mono<CreateOrderResponse> createOrder(String supplierName, CreateOrderRequest request) {
        logger.info("Creating order with supplier: {}", supplierName);
        
        SupplierAdapter adapter = findAdapterByName(supplierName);
        if (adapter == null) {
            return Mono.just(createErrorOrderResponse("Supplier not found: " + supplierName));
        }
        
        return adapter.createOrder(request)
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(error -> {
                    logger.error("Order creation failed for supplier: {}", supplierName, error);
                    return Mono.just(createErrorOrderResponse("Order creation failed: " + error.getMessage()));
                });
    }
    
    /**
     * 取消订单
     * @param supplierName 供应商名称
     * @param bookingReference 订单号
     * @param reason 取消原因
     * @return 取消结果
     */
    public Mono<Boolean> cancelOrder(String supplierName, String bookingReference, String reason) {
        logger.info("Cancelling order {} with supplier: {}", bookingReference, supplierName);
        
        SupplierAdapter adapter = findAdapterByName(supplierName);
        if (adapter == null) {
            logger.error("Supplier not found for cancellation: {}", supplierName);
            return Mono.just(false);
        }
        
        return adapter.cancelOrder(bookingReference, reason)
                .timeout(Duration.ofSeconds(30))
                .onErrorReturn(false);
    }
    
    /**
     * 查询订单状态
     * @param supplierName 供应商名称
     * @param bookingReference 订单号
     * @return 订单状态
     */
    public Mono<Integer> getOrderStatus(String supplierName, String bookingReference) {
        logger.info("Getting order status for {} with supplier: {}", bookingReference, supplierName);
        
        SupplierAdapter adapter = findAdapterByName(supplierName);
        if (adapter == null) {
            logger.error("Supplier not found for status check: {}", supplierName);
            return Mono.just(0);
        }
        
        return adapter.getOrderStatus(bookingReference)
                .timeout(Duration.ofSeconds(15))
                .onErrorReturn(0);
    }
    
    /**
     * 获取所有启用的供应商列表
     * @return 供应商名称列表
     */
    public List<String> getEnabledSuppliers() {
        return enabledAdapters.stream()
                .map(SupplierAdapter::getSupplierName)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查供应商健康状态
     * @param supplierName 供应商名称
     * @return 健康状态
     */
    public Mono<Boolean> checkSupplierHealth(String supplierName) {
        SupplierAdapter adapter = findAdapterByName(supplierName);
        if (adapter == null) {
            return Mono.just(false);
        }
        
        return adapter.healthCheck()
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(false);
    }
    
    /**
     * 检查所有供应商健康状态
     * @return 健康状态映射
     */
    public Mono<List<SupplierHealthStatus>> checkAllSuppliersHealth() {
        return Flux.fromIterable(enabledAdapters)
                .flatMap(adapter -> 
                        adapter.healthCheck()
                                .map(healthy -> new SupplierHealthStatus(adapter.getSupplierName(), healthy))
                                .onErrorReturn(new SupplierHealthStatus(adapter.getSupplierName(), false))
                )
                .collectList();
    }
    
    private Mono<HotelSearchResponse> searchFromSupplier(SupplierAdapter adapter, HotelSearchRequest request) {
        return adapter.searchHotels(request)
                .timeout(Duration.ofMillis(adapter.getTimeoutMs()))
                .onErrorResume(error -> {
                    logger.error("Search failed for supplier: {}", adapter.getSupplierName(), error);
                    return Mono.just(createEmptyResponse("Search failed for " + adapter.getSupplierName()));
                });
    }
    
    private SupplierAdapter findAdapterByName(String supplierName) {
        return enabledAdapters.stream()
                .filter(adapter -> adapter.getSupplierName().equalsIgnoreCase(supplierName))
                .findFirst()
                .orElse(null);
    }
    
    private HotelSearchResponse aggregateSearchResults(List<HotelSearchResponse> responses) {
        HotelSearchResponse aggregatedResponse = new HotelSearchResponse();
        List<HotelInfo> allHotels = new ArrayList<>();
        
        for (HotelSearchResponse response : responses) {
            if (response.getHotels() != null) {
                allHotels.addAll(response.getHotels());
            }
        }
        
        // 去重和排序
        List<HotelInfo> uniqueHotels = allHotels.stream()
                .distinct()
                .sorted((h1, h2) -> {
                    // 按价格排序，价格为空的排在后面
                    if (h1.getLowestPrice() == null && h2.getLowestPrice() == null) return 0;
                    if (h1.getLowestPrice() == null) return 1;
                    if (h2.getLowestPrice() == null) return -1;
                    return h1.getLowestPrice().compareTo(h2.getLowestPrice());
                })
                .collect(Collectors.toList());
        
        aggregatedResponse.setHotels(uniqueHotels);
        aggregatedResponse.setTotalCount(uniqueHotels.size());
        aggregatedResponse.setMessage("Success");
        aggregatedResponse.setCurrency("MYR"); // 默认货币
        
        logger.info("Aggregated {} hotels from {} suppliers", uniqueHotels.size(), responses.size());
        
        return aggregatedResponse;
    }
    
    private HotelSearchResponse createEmptyResponse(String message) {
        HotelSearchResponse response = new HotelSearchResponse();
        response.setHotels(new ArrayList<>());
        response.setTotalCount(0);
        response.setMessage(message);
        return response;
    }
    
    private HotelSearchResponse createErrorResponse(String errorMessage) {
        HotelSearchResponse response = new HotelSearchResponse();
        response.setHotels(new ArrayList<>());
        response.setTotalCount(0);
        response.setMessage(errorMessage);
        return response;
    }
    
    private CreateOrderResponse createErrorOrderResponse(String errorMessage) {
        return new CreateOrderResponse(-1, -1, errorMessage);
    }
    
    /**
     * 供应商健康状态内部类
     */
    public static class SupplierHealthStatus {
        private String supplierName;
        private boolean healthy;
        
        public SupplierHealthStatus(String supplierName, boolean healthy) {
            this.supplierName = supplierName;
            this.healthy = healthy;
        }
        
        public String getSupplierName() {
            return supplierName;
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        @Override
        public String toString() {
            return "SupplierHealthStatus{" +
                    "supplierName='" + supplierName + '\'' +
                    ", healthy=" + healthy +
                    '}';
        }
    }
}
