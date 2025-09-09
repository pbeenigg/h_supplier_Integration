package com.heytrip.hotel.supplier.controller;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.config.Config;
import com.heytrip.hotel.supplier.repository.ApiCallLogRepository;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import com.heytrip.hotel.supplier.repository.SupplierHealthLogRepository;
import com.heytrip.hotel.supplier.repository.SystemConfigRepository;
import com.heytrip.hotel.supplier.entity.ApiCallLog;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.entity.SupplierHealthLog;
import com.heytrip.hotel.supplier.entity.SystemConfig;
import com.heytrip.hotel.supplier.service.SystemConfigService;
import com.heytrip.hotel.supplier.utils.SignUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Monitoring
 * 监控控制器
 * 提供系统监控、健康检查和统计信息API
 * 
 * @author  Pax
 */
@RestController
@RequestMapping("/monitor")
public class MonitoringController implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);
    
    @Autowired
    private SupplierAdapterManager supplierAdapterManager;
    
    @Autowired
    private SupplierConfigRepository supplierConfigRepository;
    
    @Autowired
    private ApiCallLogRepository apiCallLogRepository;
    
    @Autowired
    private SupplierHealthLogRepository supplierHealthLogRepository;
    
    @Autowired
    private SystemConfigRepository systemConfigRepository;
    
    @Autowired
    private SystemConfigService systemConfigService;

    @Resource
    private Config config;
    
    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("Performing system health check");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "Hotel Supplier Integration Service");
        health.put("version", "1.0.0");
        
        try {
            // 检查数据库连接
            long apiCallCount = apiCallLogRepository.count();
            long supplierCount = supplierConfigRepository.countActiveSuppliers();
            long healthLogCount = supplierHealthLogRepository.count();
            
            health.put("database", Map.of(
                    "status", "UP",
                    "apiCallCount", apiCallCount,
                    "supplierCount", supplierCount,
                    "healthLogCount", healthLogCount
            ));
            
            // 检查供应商状态
            List<String> enabledSuppliers = supplierAdapterManager.getEnabledSuppliers();
            List<SupplierHealthLog> latestHealthStatus = supplierHealthLogRepository.findLatestHealthStatusForAllSuppliers();
            
            health.put("suppliers", Map.of(
                    "enabled", enabledSuppliers,
                    "count", enabledSuppliers.size(),
                    "healthChecks", latestHealthStatus.size()
            ));
            
            // 检查系统配置
            long activeConfigs = systemConfigRepository.countActiveConfigs();
            health.put("systemConfig", Map.of(
                    "activeConfigs", activeConfigs,
                    "status", "UP"
            ));
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * 获取系统统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        logger.info("Getting system statistics");
        
        try {
            Map<String, Object> stats = new HashMap<>();

            // API调用统计
            long totalApiCalls = apiCallLogRepository.count();
            long successfulCalls = apiCallLogRepository.countByIsSuccessTrue();
            stats.put("apiCalls", Map.of(
                    "total", totalApiCalls,
                    "successful", successfulCalls,
                    "failed", totalApiCalls - successfulCalls,
                    "successRate", totalApiCalls > 0 ? (double) successfulCalls / totalApiCalls * 100 : 0.0
            ));
            
            // 供应商统计
            long totalSuppliers = supplierConfigRepository.countAllSuppliers();
            long activeSuppliers = supplierConfigRepository.countActiveSuppliers();
            stats.put("suppliers", Map.of(
                    "total", totalSuppliers,
                    "active", activeSuppliers,
                    "inactive", totalSuppliers - activeSuppliers,
                    "enabled", supplierAdapterManager.getEnabledSuppliers().size(),
                    "list", supplierAdapterManager.getEnabledSuppliers()
            ));
            
            // 系统配置统计
            long totalConfigs = systemConfigRepository.countAllConfigs();
            long activeConfigs = systemConfigRepository.countActiveConfigs();
            long encryptedConfigs = systemConfigRepository.countEncryptedConfigs();
            stats.put("systemConfig", Map.of(
                    "total", totalConfigs,
                    "active", activeConfigs,
                    "encrypted", encryptedConfigs
            ));
            
            stats.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Failed to get system statistics", e);
            Map<String, Object> errorStats = Map.of(
                    "error", "Failed to retrieve statistics: " + e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.internalServerError().body(errorStats);
        }
    }
    
    /**
     * 获取供应商性能指标
     */
    @GetMapping("/metrics/suppliers")
    public Mono<ResponseEntity<Map<String, Object>>> getSupplierMetrics() {
        logger.info("Getting supplier performance metrics");
        
        return supplierAdapterManager.checkAllSuppliersHealth()
                .map(healthStatuses -> {
                    Map<String, Object> metrics = new HashMap<>();
                    
                    healthStatuses.forEach(status -> {
                        String supplierName = status.getSupplierName();
                        
                        // 获取该供应商的API调用统计
                        try {
                            // 这里简化实现，实际项目中应该根据supplier_id查询
                            long totalCalls = apiCallLogRepository.count();
                            long successfulCalls = apiCallLogRepository.countByResponseStatus(200);
                            
                            metrics.put(supplierName, Map.of(
                                    "healthy", status.isHealthy(),
                                    "totalApiCalls", totalCalls,
                                    "successfulCalls", successfulCalls,
                                    "successRate", totalCalls > 0 ? (double) successfulCalls / totalCalls * 100 : 0.0
                            ));
                        } catch (Exception e) {
                            logger.warn("Failed to get metrics for supplier: {}", supplierName, e);
                            metrics.put(supplierName, Map.of(
                                    "healthy", status.isHealthy(),
                                    "error", "Failed to retrieve metrics"
                            ));
                        }
                    });
                    
                    Map<String, Object> response = Map.of(
                            "suppliers", metrics,
                            "timestamp", LocalDateTime.now()
                    );
                    
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    logger.error("Failed to get supplier metrics", error);
                    Map<String, Object> errorResponse = Map.of(
                            "error", "Failed to retrieve supplier metrics: " + error.getMessage(),
                            "timestamp", LocalDateTime.now()
                    );
                    return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
                });
    }
    
    /**
     * 获取系统性能指标
     */
    @GetMapping("/metrics/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        logger.info("Getting system performance metrics");
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            Map<String, Object> jvmMetrics = Map.of(
                    "maxMemory", runtime.maxMemory() / 1024 / 1024, // MB
                    "totalMemory", runtime.totalMemory() / 1024 / 1024, // MB
                    "freeMemory", runtime.freeMemory() / 1024 / 1024, // MB
                    "usedMemory", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024, // MB
                    "availableProcessors", runtime.availableProcessors()
            );
            
            // 获取平均响应时间（简化实现）
            Double avgResponseTime = apiCallLogRepository.findAverageResponseTime();
            
            Map<String, Object> metrics = Map.of(
                    "jvm", jvmMetrics,
                    "averageResponseTime", avgResponseTime != null ? avgResponseTime : 0.0,
                    "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Failed to get performance metrics", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to retrieve performance metrics: " + e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 系统就绪检查
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        logger.info("Performing readiness check");
        
        try {
            // 检查数据库连接
            apiCallLogRepository.count();
            
            // 检查至少有一个供应商可用
            boolean hasEnabledSuppliers = !supplierAdapterManager.getEnabledSuppliers().isEmpty();
            
            if (hasEnabledSuppliers) {
                Map<String, Object> response = Map.of(
                        "status", "READY",
                        "message", "Service is ready to accept requests",
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                        "status", "NOT_READY",
                        "message", "No suppliers available",
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(503).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Readiness check failed", e);
            Map<String, Object> response = Map.of(
                    "status", "NOT_READY",
                    "message", "Service is not ready: " + e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * 获取供应商健康状态监控信息
     */
    @GetMapping("/supplier-health")
    public ResponseEntity<Map<String, Object>> getSupplierHealthStatus() {
        try {
            Map<String, Object> healthStatus = new HashMap<>();
            
            // 获取所有供应商配置
            List<SupplierConfig> suppliers = supplierConfigRepository.findAll();
            List<Map<String, Object>> supplierHealthList = new ArrayList<>();
            
            for (SupplierConfig supplier : suppliers) {
                Map<String, Object> supplierHealth = new HashMap<>();
                supplierHealth.put("supplierId", supplier.getId());
                supplierHealth.put("supplierName", supplier.getSupplierName());
                supplierHealth.put("supplierCode", supplier.getSupplierCode());
                supplierHealth.put("enabled", supplier.getIsActive());
                
                // 获取最近的健康检查日志
                List<SupplierHealthLog> recentHealthLogs = supplierHealthLogRepository
                    .findRecentLogsBySupplierId(supplier.getId(), PageRequest.of(0, 1));
                
                if (!recentHealthLogs.isEmpty()) {
                    SupplierHealthLog latestLog = recentHealthLogs.get(0);
                    supplierHealth.put("lastCheckTime", latestLog.getCreatedAt());
                    supplierHealth.put("healthStatus", latestLog.getHealthStatus());
                    supplierHealth.put("responseTime", latestLog.getResponseTimeMs());
                    supplierHealth.put("errorMessage", latestLog.getErrorMessage());
                } else {
                    supplierHealth.put("lastCheckTime", null);
                    supplierHealth.put("healthStatus", "UNKNOWN");
                    supplierHealth.put("responseTime", null);
                    supplierHealth.put("errorMessage", "No health check data available");
                }
                
                // 获取最近24小时的API调用统计
                LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
                LocalDateTime now = LocalDateTime.now();
                
                Long totalCalls = apiCallLogRepository.countCallsBySupplierId(supplier.getId());
                Long successCalls = apiCallLogRepository.countSuccessfulCallsBetween(yesterday, now);
                Long failedCalls = apiCallLogRepository.countFailedCallsBetween(yesterday, now);
                Double avgResponseTime = apiCallLogRepository.calculateAverageResponseTime(yesterday, now);
                
                supplierHealth.put("totalCalls", totalCalls != null ? totalCalls : 0);
                supplierHealth.put("successCalls24h", successCalls != null ? successCalls : 0);
                supplierHealth.put("failedCalls24h", failedCalls != null ? failedCalls : 0);
                supplierHealth.put("avgResponseTime24h", avgResponseTime != null ? avgResponseTime : 0.0);
                
                // 计算成功率
                if (totalCalls != null && totalCalls > 0) {
                    double successRate = (successCalls != null ? successCalls : 0) * 100.0 / totalCalls;
                    supplierHealth.put("successRate", Math.round(successRate * 100.0) / 100.0);
                } else {
                    supplierHealth.put("successRate", 0.0);
                }
                
                supplierHealthList.add(supplierHealth);
            }
            
            healthStatus.put("suppliers", supplierHealthList);
            healthStatus.put("totalSuppliers", suppliers.size());
            healthStatus.put("enabledSuppliers", suppliers.stream().mapToLong(s -> s.getIsActive() ? 1 : 0).sum());
            healthStatus.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            logger.error("Failed to get supplier health status", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to retrieve supplier health status",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取指定供应商的详细健康状态
     */
    @GetMapping("/supplier-health/{supplierId}")
    public ResponseEntity<Map<String, Object>> getSupplierHealthDetail(@PathVariable Long supplierId) {
        try {
            Optional<SupplierConfig> supplierOpt = supplierConfigRepository.findById(supplierId);
            if (supplierOpt.isEmpty()) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "Supplier not found",
                        "supplierId", supplierId,
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            SupplierConfig supplier = supplierOpt.get();
            Map<String, Object> healthDetail = new HashMap<>();
            
            // 基本信息
            healthDetail.put("supplierId", supplier.getId());
            healthDetail.put("supplierName", supplier.getSupplierName());
            healthDetail.put("supplierCode", supplier.getSupplierCode());
            healthDetail.put("enabled", supplier.getIsActive());
            healthDetail.put("baseUrl", supplier.getApiBaseUrl());
            healthDetail.put("maxConcurrentRequests", supplier.getMaxConcurrentRequests());
            healthDetail.put("rateLimitPerSecond", supplier.getRateLimitPerSecond());
            
            // 最近的健康检查日志（最近10条）
            List<SupplierHealthLog> recentHealthLogs = supplierHealthLogRepository
                .findRecentLogsBySupplierId(supplierId, PageRequest.of(0, 10));
            healthDetail.put("recentHealthLogs", recentHealthLogs);
            
            // 最近的API调用日志（最近20条）
            List<ApiCallLog> recentApiCalls = apiCallLogRepository
                .findRecentCallsBySupplierId(supplierId).stream()
                .limit(20)
                .collect(Collectors.toList());
            healthDetail.put("recentApiCalls", recentApiCalls);
            
            // 统计信息
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastWeek = LocalDateTime.now().minusDays(7);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalCalls", apiCallLogRepository.countCallsBySupplierId(supplierId));
            statistics.put("successCalls24h", apiCallLogRepository.countSuccessfulCallsBetween(yesterday, now));
            statistics.put("failedCalls24h", apiCallLogRepository.countFailedCallsBetween(yesterday, now));
            statistics.put("avgResponseTime24h", apiCallLogRepository.calculateAverageResponseTime(yesterday, now));
            statistics.put("successCalls7d", apiCallLogRepository.countSuccessfulCallsBetween(lastWeek, now));
            statistics.put("failedCalls7d", apiCallLogRepository.countFailedCallsBetween(lastWeek, now));
            statistics.put("avgResponseTime7d", apiCallLogRepository.calculateAverageResponseTime(lastWeek, now));
            
            healthDetail.put("statistics", statistics);
            healthDetail.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(healthDetail);
            
        } catch (Exception e) {
            logger.error("Failed to get supplier health detail for supplier: " + supplierId, e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to retrieve supplier health detail",
                    "supplierId", supplierId,
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取供应商健康检查历史记录
     */
    @GetMapping("/supplier-health/{supplierId}/history")
    public ResponseEntity<Map<String, Object>> getSupplierHealthHistory(
            @PathVariable Long supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String checkType) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SupplierHealthLog> healthLogs;
            
            if (status != null && checkType != null) {
                SupplierHealthLog.HealthStatus healthStatus = SupplierHealthLog.HealthStatus.valueOf(status.toUpperCase());
                SupplierHealthLog.CheckType checkTypeEnum = SupplierHealthLog.CheckType.valueOf(checkType.toUpperCase());
                healthLogs = supplierHealthLogRepository.findBySupplierIdAndHealthStatusAndCheckType(
                    supplierId, healthStatus, checkTypeEnum, pageable);
            } else if (status != null) {
                SupplierHealthLog.HealthStatus healthStatus = SupplierHealthLog.HealthStatus.valueOf(status.toUpperCase());
                healthLogs = supplierHealthLogRepository.findBySupplierIdAndHealthStatus(
                    supplierId, healthStatus, pageable);
            } else if (checkType != null) {
                SupplierHealthLog.CheckType checkTypeEnum = SupplierHealthLog.CheckType.valueOf(checkType.toUpperCase());
                healthLogs = supplierHealthLogRepository.findBySupplierIdAndCheckType(
                    supplierId, checkTypeEnum, pageable);
            } else {
                healthLogs = supplierHealthLogRepository.findBySupplierId(supplierId, pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", healthLogs.getContent());
            response.put("totalElements", healthLogs.getTotalElements());
            response.put("totalPages", healthLogs.getTotalPages());
            response.put("currentPage", healthLogs.getNumber());
            response.put("size", healthLogs.getSize());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = Map.of(
                    "error", "Invalid parameter value",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            logger.error("Failed to get supplier health history for supplier: " + supplierId, e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to retrieve supplier health history",
                    "supplierId", supplierId,
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    



    /**
     * Spring Boot Actuator 健康指标实现
     *
     * @return Health 指标
     */
    @Override
    public Health health() {
        try {
            // 检查数据库
            apiCallLogRepository.count();
            
            // 检查供应商
            boolean hasEnabledSuppliers = !supplierAdapterManager.getEnabledSuppliers().isEmpty();
            
            if (hasEnabledSuppliers) {
                return Health.up()
                        .withDetail("database", "UP")
                        .withDetail("suppliers", supplierAdapterManager.getEnabledSuppliers().size())
                        .build();
            } else {
                return Health.down()
                        .withDetail("database", "UP")
                        .withDetail("suppliers", "No enabled suppliers")
                        .build();
            }
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
    
    // ==================== 开发环境工具方法 ====================
    
    /**
     * 生成API认证头部信息（仅用于开发环境）
     *
     *
     * @param customTimestamp 可选的自定义时间戳，如果不提供则使用当前时间
     * @return 包含认证头部信息的响应
     */
    @GetMapping("/gen-auth")
    public ResponseEntity<Map<String, Object>> generateAuthHeaders(@RequestParam(required = false) String customTimestamp) {
        try {
            // 使用当前时间戳或自定义时间戳
            String timestamp = StrUtil.isNotBlank(customTimestamp) ?
                    customTimestamp.trim() : String.valueOf(System.currentTimeMillis() / 1000);

            String appId = config.getAuthorization().getAppId();
            String secretKey = config.getAuthorization().getSecretKey();

            // 生成MD5签名
            String signature =  SignUtil.generateSignature(appId, timestamp, secretKey);
            
            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            
            // 认证头部信息
            Map<String, String> headers = new HashMap<>();
            headers.put("X-App-Id", appId);
            headers.put("X-Timestamp", timestamp);
            headers.put("X-Signature", signature);
            
            response.put("headers", headers);
            response.put("appId", appId);
            response.put("timestamp", timestamp);
            response.put("signature", signature);
            response.put("signatureAlgorithm", "MD5(appId + timestamp + secretKey)");
            response.put("generatedAt", LocalDateTime.now());
            
            // 使用示例
            Map<String, Object> example = new HashMap<>();
            example.put("description", "使用这些头部信息调用需要认证的API");
            String curlCommand = String.format("curl --location --request GET 'http://localhost:8080/monitor/gen-auth' --header 'X-App-Id: %s' --header 'X-Timestamp: %s' --header 'X-Signature: %s'", appId, timestamp, signature);
            example.put("curlCommand", curlCommand);

            
            Map<String, String> postmanHeaders = new HashMap<>();
            postmanHeaders.put("X-App-Id", appId);
            postmanHeaders.put("X-Timestamp", timestamp);
            postmanHeaders.put("X-Signature", signature);
            postmanHeaders.put("Content-Type", "application/json");
            
            example.put("postmanHeaders", postmanHeaders);
            response.put("usage", example);
            
            // 签名生成说明
            Map<String, Object> signatureInfo = new HashMap<>();
            signatureInfo.put("algorithm", "MD5");
            signatureInfo.put("inputFormat", "appId + timestamp + secretKey");
            signatureInfo.put("inputExample", appId + timestamp + secretKey);
            signatureInfo.put("outputFormat", "32位小写十六进制字符串");
            signatureInfo.put("timestampFormat", "Unix时间戳（秒）");
            signatureInfo.put("timestampTolerance", "±300秒（5分钟）");
            
            response.put("signatureInfo", signatureInfo);
            
            logger.info("Generated auth headers for development - AppId: {}, Timestamp: {}", appId, timestamp);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to generate auth headers", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to generate auth headers",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    public static void main(String[] args) {
        String curlCommand = "curl --location --request GET 'http://localhost:8080/monitor/gen-auth' \\\n" +
                "  --header 'X-App-Id: heytrip_supplier_integration_pax' \\\n" +
                "  --header 'X-Timestamp: 1757408351' \\\n" +
                "  --header 'X-Signature: 147541c82d73b94843ecf442bf75f260'";

        System.out.println(curlCommand);
    }
    
    /**
     * 验证认证头部信息（仅用于开发环境）
     *
     * @param appIdParam 应用ID
     * @param timestampParam 时间戳
     * @param signatureParam 签名
     * @return 验证结果
     */
    @GetMapping("/validate-auth")
    public ResponseEntity<Map<String, Object>> validateAuthHeaders(@RequestParam("appId") String appIdParam, @RequestParam("timestamp") String timestampParam, @RequestParam("signature") String signatureParam) {
        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> validation = new HashMap<>();

            // 获取配置的AppId和SecretKey
            String appId = config.getAuthorization().getAppId();
            String secretKey = config.getAuthorization().getSecretKey();
            
            // 验证AppId
            boolean appIdValid = appId.equals(appIdParam);
            validation.put("appIdValid", appIdValid);
            validation.put("expectedAppId", appId);
            validation.put("providedAppId", appIdParam);
            
            // 验证时间戳格式和有效性
            boolean timestampValid = false;
            boolean timestampInRange = false;
            try {
                long timestamp = Long.parseLong(timestampParam);
                timestampValid = true;
                
                long currentTime = System.currentTimeMillis() / 1000;
                long timeDiff = Math.abs(currentTime - timestamp);
                timestampInRange = timeDiff <= 300; // 5分钟容差
                
                validation.put("timestampDiff", timeDiff);
                validation.put("maxAllowedDiff", 300);
            } catch (NumberFormatException e) {
                validation.put("timestampFormatError", "时间戳必须是数字格式");
            }
            
            validation.put("timestampValid", timestampValid);
            validation.put("timestampInRange", timestampInRange);
            
            // 验证签名
            boolean signatureValid = false;
            String expectedSignature = null;
            if (timestampValid) {
                expectedSignature = SignUtil.generateSignature(appIdParam, timestampParam, secretKey);
                signatureValid = expectedSignature.equalsIgnoreCase(signatureParam);
            }
            
            validation.put("signatureValid", signatureValid);
            validation.put("expectedSignature", expectedSignature);
            validation.put("providedSignature", signatureParam);
            
            // 整体验证结果
            boolean overallValid = appIdValid && timestampValid && timestampInRange && signatureValid;
            validation.put("overallValid", overallValid);
            
            response.put("validation", validation);
            response.put("timestamp", LocalDateTime.now());
            
            if (overallValid) {
                response.put("message", "认证头部验证通过");
                response.put("status", "VALID");
            } else {
                response.put("message", "认证头部验证失败");
                response.put("status", "INVALID");
                
                List<String> errors = new ArrayList<>();
                if (!appIdValid) errors.add("AppId不匹配");
                if (!timestampValid) errors.add("时间戳格式无效");
                if (!timestampInRange) errors.add("时间戳超出允许范围");
                if (!signatureValid) errors.add("签名验证失败");
                
                response.put("errors", errors);
            }
            
            logger.info("Validated auth headers - AppId: {}, Valid: {}", appIdParam, overallValid);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to validate auth headers", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to validate auth headers",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    


}
