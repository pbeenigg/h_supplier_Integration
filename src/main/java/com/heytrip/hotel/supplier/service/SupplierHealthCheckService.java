package com.heytrip.hotel.supplier.service;

import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.entity.SupplierHealthLog;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import com.heytrip.hotel.supplier.repository.SupplierHealthLogRepository;
import com.heytrip.hotel.supplier.client.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 供应商健康检查服务
 * 负责执行供应商健康检查并记录结果
 * 
 * @author Pax
 */
@Service
public class SupplierHealthCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(SupplierHealthCheckService.class);
    
    @Autowired
    private SupplierConfigRepository supplierConfigRepository;
    
    @Autowired
    private SupplierHealthLogRepository supplierHealthLogRepository;
    
    @Autowired
    private HttpClientService httpClientService;
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    /**
     * 执行所有启用供应商的健康检查
     */
    @Async
    public CompletableFuture<Void> performHealthCheckForAllSuppliers() {
        try {
            List<SupplierConfig> enabledSuppliers = supplierConfigRepository.findByIsActiveTrue();
            logger.info("开始执行健康检查，共{}个启用的供应商", enabledSuppliers.size());
            
            for (SupplierConfig supplier : enabledSuppliers) {
                try {
                    performHealthCheck(supplier).join();
                } catch (Exception e) {
                    logger.error("供应商{}健康检查失败", supplier.getSupplierName(), e);
                }
            }
            
            logger.info("所有供应商健康检查完成");
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("执行全部供应商健康检查时发生错误", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 执行指定供应商的健康检查
     */
    @Async
    public CompletableFuture<SupplierHealthLog> performHealthCheck(SupplierConfig supplier) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("开始执行供应商{}的健康检查", supplier.getSupplierName());
            
            SupplierHealthLog healthLog = new SupplierHealthLog();
            healthLog.setSupplierId(supplier.getId());
            healthLog.setCheckType(SupplierHealthLog.CheckType.AUTOMATIC.getCode());
            healthLog.setCreatedAt(LocalDateTime.now());
            
            long startTime = System.currentTimeMillis();
            
            try {
                // 执行连接性检查
                boolean isConnectable = checkConnectivity(supplier);
                
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                
                healthLog.setResponseTimeMs(responseTime);
                
                if (isConnectable) {
                    // 如果连接成功，执行API可用性检查
                    boolean isApiAvailable = checkApiAvailability(supplier);
                    
                    if (isApiAvailable) {
                        healthLog.setHealthStatus(SupplierHealthLog.HealthStatus.UP.name());
                        healthLog.setStatusMessage("供应商服务正常");
                    } else {
                        healthLog.setHealthStatus(SupplierHealthLog.HealthStatus.DEGRADED.name());
                        healthLog.setStatusMessage("供应商连接正常但API服务异常");
                    }
                } else {
                    healthLog.setHealthStatus(SupplierHealthLog.HealthStatus.DOWN.name());
                    healthLog.setStatusMessage("供应商连接失败");
                }
                
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                
                healthLog.setResponseTimeMs(responseTime);
                healthLog.setHealthStatus(SupplierHealthLog.HealthStatus.DOWN.name());
                healthLog.setStatusMessage("健康检查异常");
                healthLog.setErrorMessage(e.getMessage());
                
                logger.warn("供应商{}健康检查异常", supplier.getSupplierName(), e);
            }
            
            // 保存健康检查日志
            try {
                SupplierHealthLog savedLog = supplierHealthLogRepository.save(healthLog);
                logger.debug("供应商{}健康检查完成，状态: {}", 
                    supplier.getSupplierName(), savedLog.getHealthStatus());
                return savedLog;
            } catch (Exception e) {
                logger.error("保存供应商{}健康检查日志失败", supplier.getSupplierName(), e);
                return healthLog;
            }
        });
    }
    
    /**
     * 执行指定供应商ID的健康检查
     */
    public CompletableFuture<SupplierHealthLog> performHealthCheckById(Long supplierId) {
        return supplierConfigRepository.findById(supplierId)
            .map(this::performHealthCheck)
            .orElse(CompletableFuture.failedFuture(
                new IllegalArgumentException("供应商不存在: " + supplierId)));
    }
    
    /**
     * 手动触发供应商健康检查
     */
    public CompletableFuture<SupplierHealthLog> performManualHealthCheck(Long supplierId) {
        return supplierConfigRepository.findById(supplierId)
            .map(supplier -> {
                return CompletableFuture.supplyAsync(() -> {
                    logger.info("手动触发供应商{}的健康检查", supplier.getSupplierName());
                    
                    SupplierHealthLog healthLog = new SupplierHealthLog();
                    healthLog.setSupplierId(supplier.getId());
                    healthLog.setCheckType(SupplierHealthLog.CheckType.MANUAL.getCode());
                    healthLog.setCreatedAt(LocalDateTime.now());
                    
                    long startTime = System.currentTimeMillis();
                    
                    try {
                        // 执行全面的健康检查
                        boolean isConnectable = checkConnectivity(supplier);
                        boolean isApiAvailable = checkApiAvailability(supplier);
                        
                        long endTime = System.currentTimeMillis();
                        long responseTime = endTime - startTime;
                        
                        healthLog.setResponseTimeMs(responseTime);
                        
                        if (isConnectable && isApiAvailable) {
                            healthLog.setHealthStatus(SupplierHealthLog.HealthStatus.UP.name());
                            healthLog.setStatusMessage("供应商服务完全正常");
                        } else if (isConnectable) {
                            healthLog.setHealthStatus(SupplierHealthLog.HealthStatus.DEGRADED.name());
                            healthLog.setStatusMessage("供应商连接正常但API服务异常");
                        } else {
                            healthLog.setHealthStatus(SupplierHealthLog.HealthStatus.UNKNOWN.name());
                            healthLog.setStatusMessage("供应商连接失败");
                        }
                        
                    } catch (Exception e) {
                        long endTime = System.currentTimeMillis();
                        long responseTime = endTime - startTime;
                        
                        healthLog.setResponseTimeMs(responseTime);
                        healthLog.setHealthStatus(SupplierHealthLog.HealthStatus.UNKNOWN.name());
                        healthLog.setStatusMessage("手动健康检查异常");
                        healthLog.setErrorMessage(e.getMessage());
                        
                        logger.warn("手动健康检查供应商{}异常", supplier.getSupplierName(), e);
                    }
                    
                    // 保存健康检查日志
                    try {
                        return supplierHealthLogRepository.save(healthLog);
                    } catch (Exception e) {
                        logger.error("保存手动健康检查日志失败", e);
                        return healthLog;
                    }
                });
            })
            .orElse(CompletableFuture.failedFuture(
                new IllegalArgumentException("供应商不存在: " + supplierId)));
    }
    
    /**
     * 检查供应商连接性
     */
    private boolean checkConnectivity(SupplierConfig supplier) {
        try {
            if (supplier.getApiBaseUrl() == null || supplier.getApiBaseUrl().trim().isEmpty()) {
                logger.warn("供应商{}的baseUrl为空", supplier.getSupplierName());
                return false;
            }
            
            // 使用WebClient进行简单的连接测试
            WebClient webClient = webClientBuilder
                .baseUrl(supplier.getApiBaseUrl())
                .build();
            
            // 发送HEAD请求测试连接
            Mono<String> response = webClient
                .head()
                .uri("/")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(10))
                .onErrorReturn("connection_failed");
            
            String result = response.block();
            return !"connection_failed".equals(result);
            
        } catch (Exception e) {
            logger.debug("供应商{}连接性检查失败: {}", supplier.getSupplierName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查供应商API可用性
     */
    private boolean checkApiAvailability(SupplierConfig supplier) {
        try {
            // 根据不同供应商执行不同的API检查
            switch (supplier.getSupplierCode()) {
                case "ASIANOVERLAND":
                    return checkAsianOverlandApi(supplier);
                case "TEST_SUPPLIER":
                    return checkTestSupplierApi(supplier);
                default:
                    // 对于未知供应商，执行通用API检查
                    return checkGenericApi(supplier);
            }
            
        } catch (Exception e) {
            logger.debug("供应商{}API可用性检查失败: {}", supplier.getSupplierName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查AsianOverland供应商API
     */
    private boolean checkAsianOverlandApi(SupplierConfig supplier) {
        try {
            // 这里可以调用AsianOverland的健康检查端点或简单的API
            // 暂时返回连接性检查结果
            return checkConnectivity(supplier);
        } catch (Exception e) {
            logger.debug("AsianOverland API检查失败", e);
            return false;
        }
    }
    
    /**
     * 检查测试供应商API
     */
    private boolean checkTestSupplierApi(SupplierConfig supplier) {
        try {
            // 对测试供应商执行简单的ping检查
            return checkConnectivity(supplier);
        } catch (Exception e) {
            logger.debug("测试供应商API检查失败", e);
            return false;
        }
    }
    
    /**
     * 通用API检查
     */
    private boolean checkGenericApi(SupplierConfig supplier) {
        try {
            // 执行通用的API健康检查
            return checkConnectivity(supplier);
        } catch (Exception e) {
            logger.debug("通用API检查失败", e);
            return false;
        }
    }
    
    /**
     * 获取供应商最新健康状态
     */
    public SupplierHealthLog.HealthStatus getLatestHealthStatus(Long supplierId) {
        try {
            List<SupplierHealthLog> recentLogs = supplierHealthLogRepository
                .findRecentLogsBySupplierId(supplierId, 
                    org.springframework.data.domain.PageRequest.of(0, 1));
            
            if (!recentLogs.isEmpty()) {
               return SupplierHealthLog.HealthStatus.fromCode(recentLogs.get(0).getHealthStatus());
            } else {
                return SupplierHealthLog.HealthStatus.UNKNOWN;
            }
            
        } catch (Exception e) {
            logger.warn("获取供应商{}最新健康状态失败", supplierId, e);
            return SupplierHealthLog.HealthStatus.UNKNOWN;
        }
    }
    
    /**
     * 检查供应商是否健康
     */
    public boolean isSupplierHealthy(Long supplierId) {
        SupplierHealthLog.HealthStatus status = getLatestHealthStatus(supplierId);
        return status == SupplierHealthLog.HealthStatus.UP;
    }
    
    /**
     * 获取不健康的供应商列表
     */
    public List<SupplierConfig> getUnhealthySuppliers() {
        try {
            List<SupplierConfig> allSuppliers = supplierConfigRepository.findByIsActiveTrue();
            return allSuppliers.stream()
                .filter(supplier -> !isSupplierHealthy(supplier.getId()))
                .toList();
        } catch (Exception e) {
            logger.error("获取不健康供应商列表失败", e);
            return List.of();
        }
    }
}
