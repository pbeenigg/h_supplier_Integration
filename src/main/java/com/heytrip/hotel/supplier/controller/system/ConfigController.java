package com.heytrip.hotel.supplier.controller.system;

import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.dto.R;
import com.heytrip.hotel.supplier.entity.primary.SystemConfig;
import com.heytrip.hotel.supplier.repository.primary.SupplierConfigRepository;
import com.heytrip.hotel.supplier.repository.primary.SystemConfigRepository;
import com.heytrip.hotel.supplier.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Config
 * 系统配置模块
 *
 * @author  Pax
 */
@RestController
@Validated
@RequestMapping("/config")
public class ConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);
    
    @Autowired
    private SupplierAdapterManager supplierAdapterManager;
    
    @Autowired
    private SupplierConfigRepository supplierConfigRepository;


    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private SystemConfigService systemConfigService;


    /**
     * 获取系统配置列表
     */
    @GetMapping("/system-config")
    public R<Map<String, Object>> getSystemConfigs(
            @RequestParam(required = false) String configType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SystemConfig> configs;

            if (configType != null) {
                SystemConfig.ConfigType type = SystemConfig.ConfigType.valueOf(configType.toUpperCase());
                configs = systemConfigRepository.findByConfigType(type, pageable);
            } else {
                configs = systemConfigRepository.findAll(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("content", configs.getContent());
            response.put("totalElements", configs.getTotalElements());
            response.put("totalPages", configs.getTotalPages());
            response.put("currentPage", configs.getNumber());
            response.put("size", configs.getSize());
            response.put("timestamp", LocalDateTime.now());

            return R.ok("系统配置查询成功", response);

        } catch (IllegalArgumentException e) {
            return R.fail("参数值无效: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to get system configs", e);
            return R.fail("获取系统配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定系统配置详情
     */
    @GetMapping("/system-config/{configId}")
    public R<SystemConfig> getSystemConfigDetail(@PathVariable Long configId) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findById(configId);
            if (configOpt.isEmpty()) {
                return R.fail("系统配置不存在，ID: " + configId);
            }

            SystemConfig config = configOpt.get();
            return R.ok("系统配置详情查询成功", config);

        } catch (Exception e) {
            logger.error("Failed to get system config detail for config: " + configId, e);
            return R.fail("获取系统配置详情失败: " + e.getMessage());
        }
    }

    /**
     * 根据配置键获取系统配置
     */
    @GetMapping("/system-config/key/{configKey}")
    public R<SystemConfig> getSystemConfigByKey(@PathVariable String configKey) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(configKey);
            if (configOpt.isEmpty()) {
                return R.fail("系统配置不存在，配置键: " + configKey);
            }

            SystemConfig config = configOpt.get();
            return R.ok("系统配置查询成功", config);

        } catch (Exception e) {
            logger.error("Failed to get system config by key: " + configKey, e);
            return R.fail("根据配置键获取系统配置失败: " + e.getMessage());
        }
    }

    /**
     * 创建新的系统配置
     */
    @PostMapping("/system-config")
    public R<SystemConfig> createSystemConfig(@RequestBody SystemConfig config) {
        try {
            // 检查配置键是否已存在
            Optional<SystemConfig> existingConfig = systemConfigRepository.findByConfigKey(config.getConfigKey());
            if (existingConfig.isPresent()) {
                return R.fail("配置键已存在: " + config.getConfigKey());
            }

            // 设置创建时间
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());

            SystemConfig savedConfig = systemConfigRepository.save(config);
            return R.ok("系统配置创建成功", savedConfig);

        } catch (Exception e) {
            logger.error("Failed to create system config", e);
            return R.fail("创建系统配置失败: " + e.getMessage());
        }
    }

    /**
     * 更新系统配置
     */
    @PutMapping("/system-config/{configId}")
    public R<SystemConfig> updateSystemConfig(
            @PathVariable Long configId,
            @RequestBody SystemConfig configUpdate) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findById(configId);
            if (configOpt.isEmpty()) {
                return R.fail("系统配置不存在，ID: " + configId);
            }

            SystemConfig existingConfig = configOpt.get();

            // 更新字段
            if (configUpdate.getConfigValue() != null) {
                existingConfig.setConfigValue(configUpdate.getConfigValue());
            }
            if (configUpdate.getDescription() != null) {
                existingConfig.setDescription(configUpdate.getDescription());
            }
            if (configUpdate.getIsActive() != null) {
                existingConfig.setIsActive(configUpdate.getIsActive());
            }
            if (configUpdate.getIsEncrypted() != null) {
                existingConfig.setIsEncrypted(configUpdate.getIsEncrypted());
            }

            existingConfig.setUpdatedAt(LocalDateTime.now());

            SystemConfig savedConfig = systemConfigRepository.save(existingConfig);
            return R.ok("系统配置更新成功", savedConfig);

        } catch (Exception e) {
            logger.error("Failed to update system config: " + configId, e);
            return R.fail("更新系统配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除系统配置
     */
    @DeleteMapping("/system-config/{configId}")
    public R<Void> deleteSystemConfig(@PathVariable Long configId) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findById(configId);
            if (configOpt.isEmpty()) {
                return R.fail("系统配置不存在，ID: " + configId);
            }

            systemConfigRepository.deleteById(configId);
            return R.ok("系统配置删除成功");

        } catch (Exception e) {
            logger.error("Failed to delete system config: " + configId, e);
            return R.fail("删除系统配置失败: " + e.getMessage());
        }
    }



    /**
     * 获取系统配置统计信息
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> getSystemConfigStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 总配置数量
            long totalConfigs = systemConfigRepository.count();
            stats.put("totalConfigs", totalConfigs);

            // 按类型统计
            Map<String, Long> typeStats = new HashMap<>();
            for (SystemConfig.ConfigType type : SystemConfig.ConfigType.values()) {
                Long count = systemConfigRepository.countByConfigType(type);
                typeStats.put(type.name(), count != null ? count : 0L);
            }
            stats.put("configsByType", typeStats);

            // 启用/禁用统计
            long enabledConfigs = systemConfigRepository.countByIsActiveTrue();
            long disabledConfigs = systemConfigRepository.countByIsActiveFalse();
            stats.put("enabledConfigs", enabledConfigs);
            stats.put("disabledConfigs", disabledConfigs);

            // 加密配置统计
            long encryptedConfigs = systemConfigRepository.countByIsEncryptedTrue();
            long unencryptedConfigs = systemConfigRepository.countByIsEncryptedFalse();
            stats.put("encryptedConfigs", encryptedConfigs);
            stats.put("unencryptedConfigs", unencryptedConfigs);

            stats.put("timestamp", LocalDateTime.now());

            return R.ok("系统配置统计信息获取成功", stats);

        } catch (Exception e) {
            logger.error("Failed to get system config stats", e);
            return R.fail("获取系统配置统计信息失败: " + e.getMessage());
        }
    }


    /**
     * 获取JSON类型配置
     */
    @GetMapping("/json/{configKey}")
    public R<Map<String, Object>> getJsonConfig(@PathVariable String configKey) {
        try {
            Optional<SystemConfig> configOpt = systemConfigService.getConfigByKey(configKey);
            if (configOpt.isPresent()) {
                SystemConfig config = configOpt.get();
                if (SystemConfig.ConfigType.JSON.name().equals(config.getConfigType())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("configKey", configKey);
                    response.put("configType", "JSON");
                    response.put("configValue", config.getConfigValue());
                    response.put("description", config.getDescription());
                    response.put("isActive", config.getIsActive());
                    response.put("timestamp", LocalDateTime.now());
                    return R.ok("JSON配置获取成功", response);
                } else {
                    return R.fail("配置类型不匹配，配置" + configKey + "不是JSON类型，实际类型: " + config.getConfigType());
                }
            } else {
                return R.fail("配置不存在，未找到配置: " + configKey);
            }
        } catch (Exception e) {
            logger.error("获取JSON配置失败: {}", configKey, e);
            return R.fail("获取JSON配置失败: " + e.getMessage());
        }
    }

    /**
     * 设置JSON类型配置
     */
    @PostMapping("/json")
    public R<SystemConfig> setJsonConfig(@RequestBody Map<String, Object> request) {
        try {
            String configKey = (String) request.get("configKey");
            Object configValue = request.get("configValue");
            String description = (String) request.get("description");

            if (configKey == null || configValue == null) {
                return R.fail("configKey和configValue不能为空");
            }

            SystemConfig savedConfig = systemConfigService.setJsonConfig(configKey, configValue, description);
            return R.ok("JSON配置保存成功", savedConfig);

        } catch (Exception e) {
            logger.error("设置JSON配置失败", e);
            return R.fail("设置JSON配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取LIST类型配置
     */
    @GetMapping("/list/{configKey}")
    public R<Map<String, Object>> getListConfig(@PathVariable String configKey) {
        try {
            Optional<List<String>> listOpt = systemConfigService.getListConfig(configKey);
            if (listOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("configKey", configKey);
                response.put("configType", "LIST");
                response.put("configValue", listOpt.get());
                response.put("timestamp", LocalDateTime.now());
                return R.ok("LIST配置获取成功", response);
            } else {
                return R.fail("未找到LIST类型配置: " + configKey);
            }
        } catch (Exception e) {
            logger.error("获取LIST配置失败: {}", configKey, e);
            return R.fail("获取LIST配置失败: " + e.getMessage());
        }
    }

    /**
     * 设置LIST类型配置
     */
    @PostMapping("/list")
    public R<SystemConfig> setListConfig(@RequestBody Map<String, Object> request) {
        try {
            String configKey = (String) request.get("configKey");
            @SuppressWarnings("unchecked")
            List<Object> configValue = (List<Object>) request.get("configValue");
            String description = (String) request.get("description");

            if (configKey == null || configValue == null) {
                return R.fail("configKey和configValue不能为空");
            }

            SystemConfig savedConfig = systemConfigService.setListConfig(configKey, configValue, description);
            return R.ok("LIST配置保存成功", savedConfig);

        } catch (Exception e) {
            logger.error("设置LIST配置失败", e);
            return R.fail("设置LIST配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取ENCRYPTED类型配置（自动解密）
     */
    @GetMapping("/encrypted/{configKey}")
    public R<Map<String, Object>> getEncryptedConfig(@PathVariable String configKey) {
        try {
            Optional<String> encryptedValueOpt = systemConfigService.getEncryptedConfig(configKey);
            if (encryptedValueOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("configKey", configKey);
                response.put("configType", "ENCRYPTED");
                response.put("configValue", encryptedValueOpt.get());
                response.put("message", "配置值已自动解密");
                response.put("timestamp", LocalDateTime.now());
                return R.ok("加密配置获取成功", response);
            } else {
                return R.fail("未找到ENCRYPTED类型配置: " + configKey);
            }
        } catch (Exception e) {
            logger.error("获取ENCRYPTED配置失败: {}", configKey, e);
            return R.fail("获取ENCRYPTED配置失败: " + e.getMessage());
        }
    }

    /**
     * 设置ENCRYPTED类型配置（自动加密）
     */
    @PostMapping("/encrypted")
    public R<Map<String, Object>> setEncryptedConfig(@RequestBody Map<String, Object> request) {
        try {
            String configKey = (String) request.get("configKey");
            String configValue = (String) request.get("configValue");
            String description = (String) request.get("description");

            if (configKey == null || configValue == null) {
                return R.fail("configKey和configValue不能为空");
            }

            SystemConfig savedConfig = systemConfigService.setEncryptedConfig(configKey, configValue, description);

            Map<String, Object> response = new HashMap<>();
            response.put("config", Map.of(
                    "id", savedConfig.getId(),
                    "configKey", savedConfig.getConfigKey(),
                    "configType", savedConfig.getConfigType(),
                    "description", savedConfig.getDescription(),
                    "isActive", savedConfig.getIsActive(),
                    "isEncrypted", savedConfig.getIsEncrypted(),
                    "createdAt", savedConfig.getCreatedAt(),
                    "updatedAt", savedConfig.getUpdatedAt()
            ));
            response.put("message", "ENCRYPTED配置保存成功，配置值已自动加密");
            response.put("timestamp", LocalDateTime.now());

            return R.ok("加密配置保存成功", response);

        } catch (Exception e) {
            logger.error("设置ENCRYPTED配置失败", e);
            return R.fail("设置ENCRYPTED配置失败: " + e.getMessage());
        }
    }

    /**
     * 验证配置值是否符合指定类型
     *
     * @param request 包含configValue和configType的请求体
     * @return 验证结果
     */
    @PostMapping("/validate")
    public R<Map<String, Object>> validateConfigValue(@RequestBody Map<String, Object> request) {
        try {
            String configValue = (String) request.get("configValue");
            String configTypeStr = (String) request.get("configType");

            if (configValue == null || configTypeStr == null) {
                return R.fail("configValue和configType不能为空");
            }

            SystemConfig.ConfigType configType;
            try {
                configType = SystemConfig.ConfigType.valueOf(configTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("message", "不支持的配置类型: " + configTypeStr);
                errorData.put("availableTypes", Arrays.stream(SystemConfig.ConfigType.values())
                        .map(SystemConfig.ConfigType::name)
                        .collect(Collectors.toList()));
                return R.fail("无效的配置类型", errorData);
            }

            boolean isValid = systemConfigService.validateConfigValue(configValue, configType);

            Map<String, Object> response = new HashMap<>();
            response.put("configValue", configValue);
            response.put("configType", configType);
            response.put("isValid", isValid);
            response.put("message", isValid ? "配置值格式正确" : "配置值格式不正确");
            response.put("timestamp", LocalDateTime.now());

            return R.ok("配置值验证完成", response);

        } catch (Exception e) {
            logger.error("验证配置值失败", e);
            return R.fail("验证配置值失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有支持的配置类型
     *
     * @return 支持的配置类型列表
     */
    @GetMapping("/types")
    public R<Map<String, Object>> getConfigTypes() {
        try {
            List<Map<String, String>> configTypes = Arrays.stream(SystemConfig.ConfigType.values())
                    .map(type -> Map.of(
                            "code", type.getCode(),
                            "name", type.name(),
                            "description", type.getDescription()
                    ))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("configTypes", configTypes);
            response.put("totalTypes", configTypes.size());
            response.put("timestamp", LocalDateTime.now());

            return R.ok("配置类型获取成功", response);

        } catch (Exception e) {
            logger.error("获取配置类型失败", e);
            return R.fail("获取配置类型失败: " + e.getMessage());
        }
    }
}
