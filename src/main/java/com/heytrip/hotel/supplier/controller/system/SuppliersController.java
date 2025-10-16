package com.heytrip.hotel.supplier.controller.system;

import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.dto.R;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import com.heytrip.hotel.supplier.utils.AuthHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Suppliers
 * 供应商模块
 * 提供供应商配置、健康检查等相关API
 * 
 * @author  Pax
 */
@RestController
@Validated
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
    public R<Map<String, Object>> getEnabledSuppliers() {
        logger.info("Getting enabled suppliers list");
        
        try {
            List<String> suppliers = supplierAdapterManager.getEnabledSuppliers();
            Map<String, Object> response = Map.of(
                    "suppliers", suppliers,
                    "count", suppliers.size()
            );
            return R.ok("获取启用供应商列表成功", response);
        } catch (Exception e) {
            logger.error("Failed to get enabled suppliers", e);
            return R.fail("获取启用供应商列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取供应商配置信息
     *
     * @param supplierName 供应商名称
     * @return 供应商配置信息
     */
    @GetMapping("/suppliers/{supplierName}/config")
    public R<SupplierConfig> getSupplierConfig(@PathVariable String supplierName) {
        logger.info("Getting config for supplier: {}", supplierName);
        
        try {
            return supplierConfigRepository.findBySupplierNameAndIsActive(supplierName, true)
                    .map(config -> R.ok("获取供应商配置成功", config))
                    .orElse(R.fail("供应商配置不存在: " + supplierName));
        } catch (Exception e) {
            logger.error("Failed to get supplier config for: " + supplierName, e);
            return R.fail("获取供应商配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查供应商健康状态
     *
     * @param supplierName 供应商名称
     * @return 供应商健康状态
     */
    @GetMapping("/suppliers/{supplierName}/health")
    public R<Map<String, Object>> checkSupplierHealth(@PathVariable String supplierName) {
        logger.info("Checking health for supplier: {}", supplierName);
        
        // 记录认证信息（用于调试）
        AuthHelper.logAuthInfo("supplier-health-check-" + supplierName);
        
        try {
            // 同步调用供应商健康检查
            Boolean healthy = supplierAdapterManager.checkSupplierHealth(supplierName).block();
            
            Map<String, Object> response = Map.of(
                    "supplierName", supplierName,
                    "healthy", healthy != null ? healthy : false,
                    "status", (healthy != null && healthy) ? "UP" : "DOWN",
                    "timestamp", System.currentTimeMillis()
            );
            return R.ok("供应商健康检查完成", response);

        } catch (Exception error) {
            logger.error("Health check failed for supplier: {}", supplierName, error);
            return R.fail("供应商健康检查失败: " + error.getMessage());
        }
    }

    /**
     * 检查所有供应商健康状态
     *
     * @return 各供应商健康状态列表
     */
    @GetMapping("/suppliers/health")
    public R<Map<String, Object>> checkAllSuppliersHealth() {
        logger.info("Checking health for all suppliers");

        try {
            // 同步调用所有供应商健康检查
            List<SupplierAdapterManager.SupplierHealthStatus> healthStatuses = supplierAdapterManager.checkAllSuppliersHealth().block();

            if (healthStatuses == null) {
                healthStatuses = List.of();
            }
            long healthyCount = healthStatuses.stream()
                    .mapToLong(status -> {
                        if (status instanceof SupplierAdapterManager.SupplierHealthStatus) {
                            return ((SupplierAdapterManager.SupplierHealthStatus) status).isHealthy() ? 1 : 0;
                        }
                        return 0;
                    })
                    .sum();

            Map<String, Object> response = Map.of(
                    "suppliers", healthStatuses,
                    "totalCount", healthStatuses.size(),
                    "healthyCount", healthyCount,
                    "unhealthyCount", healthStatuses.size() - healthyCount,
                    "timestamp", System.currentTimeMillis()
            );
            return R.ok("所有供应商健康检查完成", response);

        } catch (Exception error) {
            logger.error("Health check failed for all suppliers", error);
            return R.fail("所有供应商健康检查失败: " + error.getMessage());
        }
    }


    /**
     * 获取API版本信息
     */
    @GetMapping("/version")
    public R<Map<String, Object>> getApiVersion() {
        logger.info("Getting API version info");

        Map<String, Object> response = Map.of(
                "apiVersion", "1.0.0",
                "serviceName", "Hotel Supplier Integration Service",
                "buildTime", LocalDateTime.now(),
                "environment", "dev",
                "supportedFormats", List.of("JSON"),
                "documentation", "/swagger-ui.html"
        );
        return R.ok("获取API版本信息成功", response);
    }

}
