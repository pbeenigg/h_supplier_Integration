package com.heytrip.hotel.supplier.controller;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.client.HttpClientService;
import com.heytrip.hotel.supplier.config.Config;
import com.heytrip.hotel.supplier.entity.ApiCallLog;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.entity.SupplierHealthLog;
import com.heytrip.hotel.supplier.repository.ApiCallLogRepository;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import com.heytrip.hotel.supplier.repository.SupplierHealthLogRepository;
import com.heytrip.hotel.supplier.repository.SystemConfigRepository;
import com.heytrip.hotel.supplier.service.SystemConfigService;
import com.heytrip.hotel.supplier.utils.HeyUtil;
import com.heytrip.hotel.supplier.utils.SignUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Monitoring
 * 监控模块
 * 提供系统监控、健康检查和统计信息API
 * 
 * @author  Pax
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitorController.class);
    
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

    @Autowired
    private HttpClientService httpClientService;



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
                .findRecentCallsBySupplierId(supplierId, PageRequest.of(0, 20));
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
        ///  生产环境禁用此接口   dev:开发环境可用  prod：生产环境禁用
        if (!HeyUtil.isDebugEnvironment()) {
            Map<String, Object> errorResponse = Map.of(
                    "error", "此接口仅在开发环境中可用",
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(403).body(errorResponse);
        }

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
            headers.put("app", appId);
            headers.put("timestamp", timestamp);
            headers.put("sign", signature);
            
            response.put("headers", headers);
            response.put("appId", appId);
            response.put("timestamp", timestamp);
            response.put("signature", signature);
            response.put("signatureAlgorithm", "MD5(appId + timestamp + secretKey)");
            response.put("generatedAt", LocalDateTime.now());
            
            // 使用示例
            Map<String, Object> example = new HashMap<>();
            example.put("description", "使用这些头部信息调用需要认证的API");
            String curlCommand = String.format("curl --location --request GET 'http://localhost:8080/monitor/gen-auth' --header 'app: %s' --header 'timestamp: %s' --header 'sign: %s'", appId, timestamp, signature);
            example.put("curlCommand", curlCommand);

            
            Map<String, String> postmanHeaders = new HashMap<>();
            postmanHeaders.put("app", appId);
            postmanHeaders.put("timestamp", timestamp);
            postmanHeaders.put("sign", signature);
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
        ///  生产环境禁用此接口   dev:开发环境可用  prod：生产环境禁用
        if (!HeyUtil.isDebugEnvironment()) {
            Map<String, Object> errorResponse = Map.of(
                    "error", "此接口仅在开发环境中可用",
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(403).body(errorResponse);
        }
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




    /**
     * 获取HTTP客户端监控指标
     */
    @GetMapping("/httpClient/metrics")
    public Map<String, Object> httpClientMetrics() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "HTTP客户端监控指标");
        result.put("data", httpClientService.getMetrics());
        result.put("availablePermits", httpClientService.getAvailablePermits());
        result.put("healthy", httpClientService.isHealthy());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * HTTP客户端重置监控指标
     */
    @PostMapping("/httpClient/reset")
    public Map<String, Object> httpClientResetMetrics() {
        httpClientService.resetMetrics();
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "监控指标已重置");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * HTTP客户端健康检查
     */
    @GetMapping("/httpClient/health")
    public Map<String, Object> httpClientHealthCheck() {
        boolean healthy = httpClientService.isHealthy();
        int availablePermits = httpClientService.getAvailablePermits();

        Map<String, Object> result = new HashMap<>();
        result.put("status", healthy ? "healthy" : "unhealthy");
        result.put("message", healthy ? "HTTP客户端运行正常" : "HTTP客户端可能存在问题");
        result.put("availablePermits", availablePermits);
        result.put("maxPermits", 50);
        result.put("utilizationRate", String.format("%.2f%%", (50 - availablePermits) / 50.0 * 100));
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

}
