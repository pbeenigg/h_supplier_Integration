package com.heytrip.hotel.supplier.controller;

import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Suppliers
 * 供应商控制器
 * 提供供应商配置、健康检查等相关API
 * 
 * @author  Pax
 */
@RestController
@Validated
@RequestMapping("/")
public class SuppliersController {
    
    private static final Logger logger = LoggerFactory.getLogger(SuppliersController.class);
    
    @Autowired
    private SupplierAdapterManager supplierAdapterManager;
    
    @Autowired
    private SupplierConfigRepository supplierConfigRepository;
    
    /**
     * 获取所有启用的供应商列表
     *
     * @return 启用的供应商列表
     */
    @GetMapping("/suppliers")
    public ResponseEntity<Map<String, Object>> getEnabledSuppliers() {
        logger.info("Getting enabled suppliers list");
        
        try {
            List<String> suppliers = supplierAdapterManager.getEnabledSuppliers();
            Map<String, Object> response = Map.of(
                    "suppliers", suppliers,
                    "count", suppliers.size(),
                    "message", "Success"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get enabled suppliers", e);
            Map<String, Object> errorResponse = Map.of(
                    "suppliers", List.of(),
                    "count", 0,
                    "message", "Failed to get suppliers: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取供应商配置信息
     *
     * @param supplierName 供应商名称
     * @return 供应商配置信息
     */
    @GetMapping("/suppliers/{supplierName}/config")
    public ResponseEntity<SupplierConfig> getSupplierConfig(@PathVariable String supplierName) {
        logger.info("Getting config for supplier: {}", supplierName);
        
        try {
            return supplierConfigRepository.findBySupplierNameAndIsActive(supplierName, true)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Failed to get supplier config for: {}", supplierName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查供应商健康状态
     *
     * @param supplierName 供应商名称
     * @return 供应商健康状态
     */
    @GetMapping("/suppliers/{supplierName}/health")
    public Mono<ResponseEntity<Map<String, Object>>> checkSupplierHealth(@PathVariable String supplierName) {
        logger.info("Checking health for supplier: {}", supplierName);
        
        return supplierAdapterManager.checkSupplierHealth(supplierName)
                .map(healthy -> {
                    Map<String, Object> response = Map.of(
                            "supplierName", supplierName,
                            "healthy", healthy,
                            "status", healthy ? "UP" : "DOWN",
                            "timestamp", System.currentTimeMillis()
                    );
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    logger.error("Health check failed for supplier: {}", supplierName, error);
                    Map<String, Object> errorResponse = Map.of(
                            "supplierName", supplierName,
                            "healthy", false,
                            "status", "ERROR",
                            "error", error.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    );
                    return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
                });
    }
    
    /**
     * 检查所有供应商健康状态
     *
     * @return 各供应商健康状态列表
     */
    @GetMapping("/suppliers/health")
    public Mono<ResponseEntity<Map<String, Object>>> checkAllSuppliersHealth() {
        logger.info("Checking health for all suppliers");
        
        return supplierAdapterManager.checkAllSuppliersHealth()
                .map(healthStatuses -> {
                    long healthyCount = healthStatuses.stream()
                            .mapToLong(status -> status.isHealthy() ? 1 : 0)
                            .sum();
                    
                    Map<String, Object> response = Map.of(
                            "suppliers", healthStatuses,
                            "totalCount", healthStatuses.size(),
                            "healthyCount", healthyCount,
                            "unhealthyCount", healthStatuses.size() - healthyCount,
                            "timestamp", System.currentTimeMillis()
                    );
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    logger.error("Health check failed for all suppliers", error);
                    Map<String, Object> errorResponse = Map.of(
                            "suppliers", List.of(),
                            "totalCount", 0,
                            "healthyCount", 0,
                            "unhealthyCount", 0,
                            "error", error.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    );
                    return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
                });
    }


    /**
     * 获取API版本信息
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getApiVersion() {
        logger.info("Getting API version info");

        Map<String, Object> response = Map.of(
                "apiVersion", "1.0.0",
                "serviceName", "Hotel Supplier Integration Service",
                "buildTime", LocalDateTime.now(),
                "environment", "dev",
                "supportedFormats", List.of("JSON"),
                "documentation", "/swagger-ui.html"
        );
        return ResponseEntity.ok(response);
    }

}
