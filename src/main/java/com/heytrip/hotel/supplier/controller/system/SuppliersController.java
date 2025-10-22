package com.heytrip.hotel.supplier.controller.system;

import cn.hutool.core.util.NumberUtil;
import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.dto.R;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import com.heytrip.hotel.supplier.utils.AuthHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public R<List<SupplierConfig>> getEnabledSuppliers() {
        logger.info("Getting enabled suppliers list");
        
        try {
            List<SupplierConfig>  supplierConfigs =  supplierConfigRepository.findAll();
            return R.ok("获取启用供应商列表成功", supplierConfigs);
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

    /**
     * 修改供应商信息
     *
     * @param request 供应商信息修改请求
     * @return 修改结果
     */
    @PutMapping("/suppliers")
    public R<String> updateSupplierInfo(@RequestBody Map<String, Object> request) {
        logger.info("Updating supplier info: {}", request);

        try {
            Long supplierId = (Long) request.get("id");
            String supplierName = (String) request.get("supplierName");
            String supplierCode = (String) request.get("supplierCode");
            Boolean isActive = (Boolean) request.get("isActive");
            Boolean isSyncStatic = (Boolean) request.get("isSyncStatic");
            Boolean isSyncHotel = (Boolean) request.get("isSyncHotel");
            String authConfig =   (String) request.get("authConfig");
            String  ftpConfig =   (String) request.get("ftpConfig");
            String  authType =   (String) request.get("authType");
            String  description =   (String) request.get("description");
            String  contactInfo =   (String) request.get("contactInfo");

            String  apiBaseUrl =   (String) request.get("apiBaseUrl");
            Integer retryCount = (Integer) request.get("retryCount");
            Long timeoutMs = (Long) request.get("timeoutMs");
            Integer priority = (Integer) request.get("priority");
            Integer maxConcurrentRequests = (Integer) request.get("maxConcurrentRequests");
            Integer rateLimitPerSecond = (Integer) request.get("rateLimitPerSecond");


            String updateBy = AuthHelper.getCurrentUser(); // 获取当前登录用户作为更新者

            if (supplierId == null ) {
                return R.fail("供应商ID不能为空");
            }

            Optional<SupplierConfig> configOpt = supplierConfigRepository.findById(supplierId);
            if (configOpt.isEmpty()) {
                return R.fail("供应商不存在: " + supplierName);
            }

            SupplierConfig config = configOpt.get();
            if (isActive != null) {
                config.setIsActive(isActive);
            }
            if(supplierCode != null){
                config.setSupplierCode(supplierCode);
            }
            if(supplierName != null){
                config.setSupplierName(supplierName);
            }
            if(isSyncStatic != null){
                config.setIsSyncStatic(isSyncStatic);
            }
            if(isSyncHotel != null){
                config.setIsSyncHotel(isSyncHotel);
            }
            if(authConfig != null){
                config.setAuthConfig(authConfig);
            }
            if(ftpConfig != null){
                config.setFtpConfig(ftpConfig);
            }
            if(authType != null){
                config.setAuthType(authType);
            }
            if(description != null){
                config.setDescription(description);
            }
            if(contactInfo != null){
                config.setContactInfo(contactInfo);
            }
            if(apiBaseUrl != null){
                config.setApiBaseUrl(apiBaseUrl);
            }
            if(retryCount != null){
                config.setRetryCount(retryCount);
            }
            if(timeoutMs != null){
                config.setTimeoutMs(timeoutMs);
            }
            if(priority != null){
                config.setPriority(priority);
            }
            if(maxConcurrentRequests != null){
                config.setMaxConcurrentRequests(maxConcurrentRequests);
            }
            if(rateLimitPerSecond != null){
                config.setRateLimitPerSecond(rateLimitPerSecond);
            }

            // 记录更新操作信息
            logger.info("供应商 {} 状态更新为: {}, 操作用户: {}", supplierName, isActive, updateBy);

            supplierConfigRepository.save(config);

            return R.ok("供应商信息修改成功");
        } catch (Exception e) {
            logger.error("Failed to update supplier info", e);
            return R.fail("修改供应商信息失败: " + e.getMessage());
        }
    }

    /**
     * 修改指定供应商的配置信息
     *
     * @param supplierId 供应商名称
     * @param request 配置修改请求
     * @return 修改结果
     */
    @PutMapping("/suppliers/{supplierId}/config")
    public R<SupplierConfig> updateSupplierConfig(@PathVariable Long supplierId,
                                                   @RequestBody Map<String, Object> request) {
        logger.info("Updating config for supplier: {}, request: {}", supplierId, request);

        try {
            Optional<SupplierConfig> configOpt = supplierConfigRepository.findById(supplierId);
            if (configOpt.isEmpty()) {
                return R.fail("供应商配置不存在: " + supplierId);
            }

            SupplierConfig config = configOpt.get();
            String currentUser = AuthHelper.getCurrentUser(); // 获取当前登录用户作为更新者

            String supplierName = (String) request.get("supplierName");

            // 更新配置参数（使用实际存在的字段）
            if (request.containsKey("authConfig")) {
                config.setAuthConfig((String) request.get("authConfig"));
            }
            if (request.containsKey("ftpConfig")) {
                config.setFtpConfig((String) request.get("ftpConfig"));
            }
            if (request.containsKey("authType")) {
                config.setAuthType((String) request.get("authType"));
            }
            if (request.containsKey("apiBaseUrl")) {
                config.setApiBaseUrl((String) request.get("apiBaseUrl"));
            }
            if (request.containsKey("timeoutMs")) {
                config.setTimeoutMs(NumberUtil.parseLong(request.get("timeoutMs").toString()));
            }
            if (request.containsKey("retryCount")) {
                config.setRetryCount((Integer) request.get("retryCount"));
            }
            if (request.containsKey("maxConcurrentRequests")) {
                config.setMaxConcurrentRequests((Integer) request.get("maxConcurrentRequests"));
            }
            if (request.containsKey("rateLimitPerSecond")) {
                config.setRateLimitPerSecond((Integer) request.get("rateLimitPerSecond"));
            }
            if (request.containsKey("isActive")) {
                config.setIsActive((Boolean) request.get("isActive"));
            }
            if (request.containsKey("isSyncStatic")) {
                config.setIsSyncStatic((Boolean) request.get("isSyncStatic"));
            }
            if (request.containsKey("isSyncHotel")) {
                config.setIsSyncHotel((Boolean) request.get("isSyncHotel"));
            }
            if (request.containsKey("priority")) {
                config.setPriority((Integer) request.get("priority"));
            }
            if (request.containsKey("description")) {
                config.setDescription((String) request.get("description"));
            }
            if (request.containsKey("contactInfo")) {
                config.setContactInfo((String) request.get("contactInfo"));
            }
            if (request.containsKey("supplierName")) {
                config.setSupplierName((String) request.get("supplierName"));
            }
            if (request.containsKey("supplierCode")) {
                config.setSupplierCode((String) request.get("supplierCode"));
            }

            config.setUpdatedBy(currentUser);

            //需要修改的字段名称：
            // authConfig, ftpConfig, authType, apiBaseUrl, timeoutMs, retryCount, maxConcurrentRequests,
            // rateLimitPerSecond, isActive, isSyncStatic, isSyncHotel, priority, description, contactInfo , supplierName, supplierCode,

            // 记录更新操作信息
            logger.info("供应商 {} 配置更新, 操作用户: {}", supplierName, currentUser);

            SupplierConfig savedConfig = supplierConfigRepository.save(config);

            return R.ok("供应商配置修改成功", savedConfig);
        } catch (Exception e) {
            logger.error("Failed to update supplier config for: " + supplierId, e);
            return R.fail("修改供应商配置失败: " + e.getMessage());
        }
    }
}
