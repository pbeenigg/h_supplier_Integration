package com.heytrip.hotel.supplier.controller;

import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.repository.BookingRecordRepository;
import com.heytrip.hotel.supplier.repository.HotelRepository;
import com.heytrip.hotel.supplier.repository.ApiCallLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
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
    private BookingRecordRepository bookingRecordRepository;
    
    @Autowired
    private HotelRepository hotelRepository;
    
    @Autowired
    private ApiCallLogRepository apiCallLogRepository;
    
    /**
     * 系统健康检查
     * GET /pax/api/xiwanSupplier/supp/health
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
            long hotelCount = hotelRepository.count();
            health.put("database", Map.of(
                    "status", "UP",
                    "hotelCount", hotelCount
            ));
            
            // 检查供应商状态
            health.put("suppliers", supplierAdapterManager.getEnabledSuppliers());
            
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
     * GET /pax/api/xiwanSupplier/supp/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        logger.info("Getting system statistics");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 酒店统计
            long totalHotels = hotelRepository.count();
            long activeHotels = hotelRepository.countByIsActive(true);
            stats.put("hotels", Map.of(
                    "total", totalHotels,
                    "active", activeHotels,
                    "inactive", totalHotels - activeHotels
            ));
            
            // 预订统计
            long totalBookings = bookingRecordRepository.count();
            long confirmedBookings = bookingRecordRepository.countByBookingStatus(2);
            long cancelledBookings = bookingRecordRepository.countByBookingStatus(9);
            stats.put("bookings", Map.of(
                    "total", totalBookings,
                    "confirmed", confirmedBookings,
                    "cancelled", cancelledBookings,
                    "pending", totalBookings - confirmedBookings - cancelledBookings
            ));
            
            // API调用统计
            long totalApiCalls = apiCallLogRepository.count();
            long successfulCalls = apiCallLogRepository.countByResponseStatus(200);
            stats.put("apiCalls", Map.of(
                    "total", totalApiCalls,
                    "successful", successfulCalls,
                    "failed", totalApiCalls - successfulCalls
            ));
            
            // 供应商统计
            stats.put("suppliers", Map.of(
                    "enabled", supplierAdapterManager.getEnabledSuppliers().size(),
                    "list", supplierAdapterManager.getEnabledSuppliers()
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
     * GET /pax/api/xiwanSupplier/supp/metrics/suppliers
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
     * GET /pax/api/xiwanSupplier/supp/metrics/performance
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
     * GET /pax/api/xiwanSupplier/supp/ready
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        logger.info("Performing readiness check");
        
        try {
            // 检查数据库连接
            hotelRepository.count();
            
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
     * Spring Boot Actuator Health Indicator implementation
     */
    @Override
    public Health health() {
        try {
            // 检查数据库
            hotelRepository.count();
            
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
}
