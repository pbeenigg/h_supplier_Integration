package com.heytrip.hotel.supplier.controller;

import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.entity.SystemConfig;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import com.heytrip.hotel.supplier.repository.SystemConfigRepository;
import com.heytrip.hotel.supplier.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Config
 * 系统配置控制器
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
    public ResponseEntity<Map<String, Object>> getSystemConfigs(
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

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = Map.of(
                    "error", "Invalid parameter value",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            logger.error("Failed to get system configs", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to retrieve system configs",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取指定系统配置详情
     */
    @GetMapping("/system-config/{configId}")
    public ResponseEntity<Map<String, Object>> getSystemConfigDetail(@PathVariable Long configId) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findById(configId);
            if (configOpt.isEmpty()) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "System config not found",
                        "configId", configId,
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(404).body(errorResponse);
            }

            SystemConfig config = configOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("config", config);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get system config detail for config: " + configId, e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to retrieve system config detail",
                    "configId", configId,
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 根据配置键获取系统配置
     */
    @GetMapping("/system-config/key/{configKey}")
    public ResponseEntity<Map<String, Object>> getSystemConfigByKey(@PathVariable String configKey) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(configKey);
            if (configOpt.isEmpty()) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "System config not found",
                        "configKey", configKey,
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(404).body(errorResponse);
            }

            SystemConfig config = configOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("config", config);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get system config by key: " + configKey, e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to retrieve system config by key",
                    "configKey", configKey,
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 创建新的系统配置
     */
    @PostMapping("/system-config")
    public ResponseEntity<Map<String, Object>> createSystemConfig(@RequestBody SystemConfig config) {
        try {
            // 检查配置键是否已存在
            Optional<SystemConfig> existingConfig = systemConfigRepository.findByConfigKey(config.getConfigKey());
            if (existingConfig.isPresent()) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "Config key already exists",
                        "configKey", config.getConfigKey(),
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(409).body(errorResponse);
            }

            // 设置创建时间
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());

            SystemConfig savedConfig = systemConfigRepository.save(config);

            Map<String, Object> response = new HashMap<>();
            response.put("config", savedConfig);
            response.put("message", "System config created successfully");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(201).body(response);

        } catch (Exception e) {
            logger.error("Failed to create system config", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to create system config",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 更新系统配置
     */
    @PutMapping("/system-config/{configId}")
    public ResponseEntity<Map<String, Object>> updateSystemConfig(
            @PathVariable Long configId,
            @RequestBody SystemConfig configUpdate) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findById(configId);
            if (configOpt.isEmpty()) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "System config not found",
                        "configId", configId,
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(404).body(errorResponse);
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

            Map<String, Object> response = new HashMap<>();
            response.put("config", savedConfig);
            response.put("message", "System config updated successfully");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to update system config: " + configId, e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to update system config",
                    "configId", configId,
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 删除系统配置
     */
    @DeleteMapping("/system-config/{configId}")
    public ResponseEntity<Map<String, Object>> deleteSystemConfig(@PathVariable Long configId) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findById(configId);
            if (configOpt.isEmpty()) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "System config not found",
                        "configId", configId,
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(404).body(errorResponse);
            }

            systemConfigRepository.deleteById(configId);

            Map<String, Object> response = Map.of(
                    "message", "System config deleted successfully",
                    "configId", configId,
                    "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to delete system config: " + configId, e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to delete system config",
                    "configId", configId,
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }



    /**
     * 获取系统配置统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemConfigStats() {
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

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Failed to get system config stats", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to retrieve system config stats",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }


    /**
     * 获取JSON类型配置
     */
    @GetMapping("/json/{configKey}")
    public ResponseEntity<?> getJsonConfig(@PathVariable String configKey) {
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
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> errorResponse = Map.of(
                            "error", "配置类型不匹配",
                            "message", "配置" + configKey + "不是JSON类型，实际类型: " + config.getConfigType(),
                            "timestamp", LocalDateTime.now()
                    );
                    return ResponseEntity.status(400).body(errorResponse);
                }
            } else {
                Map<String, Object> errorResponse = Map.of(
                        "error", "配置不存在",
                        "message", "未找到配置: " + configKey,
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(404).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("获取JSON配置失败: {}", configKey, e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "获取JSON配置失败",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 设置JSON类型配置
     */
    @PostMapping("/json")
    public ResponseEntity<?> setJsonConfig(@RequestBody Map<String, Object> request) {
        try {
            String configKey = (String) request.get("configKey");
            Object configValue = request.get("configValue");
            String description = (String) request.get("description");

            if (configKey == null || configValue == null) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "参数缺失",
                        "message", "configKey和configValue不能为空",
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(400).body(errorResponse);
            }

            SystemConfig savedConfig = systemConfigService.setJsonConfig(configKey, configValue, description);

            Map<String, Object> response = new HashMap<>();
            response.put("config", savedConfig);
            response.put("message", "JSON配置保存成功");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("设置JSON配置失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "设置JSON配置失败",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取LIST类型配置
     */
    @GetMapping("/list/{configKey}")
    public ResponseEntity<?> getListConfig(@PathVariable String configKey) {
        try {
            Optional<List<String>> listOpt = systemConfigService.getListConfig(configKey);
            if (listOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("configKey", configKey);
                response.put("configType", "LIST");
                response.put("configValue", listOpt.get());
                response.put("timestamp", LocalDateTime.now());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = Map.of(
                        "error", "配置不存在或类型不匹配",
                        "message", "未找到LIST类型配置: " + configKey,
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(404).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("获取LIST配置失败: {}", configKey, e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "获取LIST配置失败",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 设置LIST类型配置
     *
     *
     */
    @PostMapping("/list")
    public ResponseEntity<?> setListConfig(@RequestBody Map<String, Object> request) {
        try {
            String configKey = (String) request.get("configKey");
            @SuppressWarnings("unchecked")
            List<Object> configValue = (List<Object>) request.get("configValue");
            String description = (String) request.get("description");

            if (configKey == null || configValue == null) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "参数缺失",
                        "message", "configKey和configValue不能为空",
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(400).body(errorResponse);
            }

            SystemConfig savedConfig = systemConfigService.setListConfig(configKey, configValue, description);

            Map<String, Object> response = new HashMap<>();
            response.put("config", savedConfig);
            response.put("message", "LIST配置保存成功");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("设置LIST配置失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "设置LIST配置失败",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取ENCRYPTED类型配置（自动解密）
     */
    @GetMapping("/encrypted/{configKey}")
    public ResponseEntity<?> getEncryptedConfig(@PathVariable String configKey) {
        try {
            Optional<String> encryptedValueOpt = systemConfigService.getEncryptedConfig(configKey);
            if (encryptedValueOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("configKey", configKey);
                response.put("configType", "ENCRYPTED");
                response.put("configValue", encryptedValueOpt.get());
                response.put("message", "配置值已自动解密");
                response.put("timestamp", LocalDateTime.now());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = Map.of(
                        "error", "配置不存在或类型不匹配",
                        "message", "未找到ENCRYPTED类型配置: " + configKey,
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(404).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("获取ENCRYPTED配置失败: {}", configKey, e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "获取ENCRYPTED配置失败",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 设置ENCRYPTED类型配置（自动加密）
     */
    @PostMapping("/encrypted")
    public ResponseEntity<?> setEncryptedConfig(@RequestBody Map<String, Object> request) {
        try {
            String configKey = (String) request.get("configKey");
            String configValue = (String) request.get("configValue");
            String description = (String) request.get("description");

            if (configKey == null || configValue == null) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "参数缺失",
                        "message", "configKey和configValue不能为空",
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(400).body(errorResponse);
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

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("设置ENCRYPTED配置失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "设置ENCRYPTED配置失败",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 验证配置值是否符合指定类型
     *
     * @param request 包含configValue和configType的请求体
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateConfigValue(@RequestBody Map<String, Object> request) {
        try {
            String configValue = (String) request.get("configValue");
            String configTypeStr = (String) request.get("configType");

            if (configValue == null || configTypeStr == null) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "参数缺失",
                        "message", "configValue和configType不能为空",
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(400).body(errorResponse);
            }

            SystemConfig.ConfigType configType;
            try {
                configType = SystemConfig.ConfigType.valueOf(configTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "无效的配置类型",
                        "message", "不支持的配置类型: " + configTypeStr,
                        "availableTypes", Arrays.stream(SystemConfig.ConfigType.values())
                                .map(SystemConfig.ConfigType::name)
                                .collect(Collectors.toList()),
                        "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(400).body(errorResponse);
            }

            boolean isValid = systemConfigService.validateConfigValue(configValue, configType);

            Map<String, Object> response = new HashMap<>();
            response.put("configValue", configValue);
            response.put("configType", configType);
            response.put("isValid", isValid);
            response.put("message", isValid ? "配置值格式正确" : "配置值格式不正确");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("验证配置值失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "验证配置值失败",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取所有支持的配置类型
     *
     * @return 支持的配置类型列表
     */
    @GetMapping("/types")
    public ResponseEntity<?> getConfigTypes() {
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

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取配置类型失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "error", "获取配置类型失败",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
