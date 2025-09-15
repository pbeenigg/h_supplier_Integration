package com.heytrip.hotel.supplier.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.entity.SystemConfig;
import com.heytrip.hotel.supplier.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * 系统配置管理服务
 * 负责系统配置的增删改查、缓存管理和加密解密
 * 
 * @author Pax
 */
@Service
@Transactional
public class SystemConfigService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemConfigService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 用于加密的密钥
    private static final String ENCRYPTION_KEY = "HeyTripSupplier2024ConfigKey123";
    private static final String ENCRYPTION_ALGORITHM = "AES";
    
    @Autowired
    private SystemConfigRepository systemConfigRepository;
    
    /**
     * 获取所有系统配置（分页）
     */
    @Transactional(readOnly = true)
    public Page<SystemConfig> getAllConfigs(Pageable pageable) {
        try {
            return systemConfigRepository.findAll(pageable);
        } catch (Exception e) {
            logger.error("获取系统配置列表失败", e);
            throw new RuntimeException("获取系统配置列表失败", e);
        }
    }
    
    /**
     * 根据配置类型获取配置（分页）
     */
    @Transactional(readOnly = true)
    public Page<SystemConfig> getConfigsByType(SystemConfig.ConfigType configType, Pageable pageable) {
        try {
            return systemConfigRepository.findByConfigType(configType, pageable);
        } catch (Exception e) {
            logger.error("根据类型获取系统配置失败: {}", configType, e);
            throw new RuntimeException("根据类型获取系统配置失败", e);
        }
    }
    
    
    /**
     * 根据配置键获取配置（带缓存）
     */
    @Cacheable(value = "systemConfig", key = "#configKey")
    @Transactional(readOnly = true)
    public Optional<SystemConfig> getConfigByKey(String configKey) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(configKey);
            
            // 如果配置存在且已加密，则解密配置值
            if (configOpt.isPresent() && configOpt.get().getIsEncrypted()) {
                SystemConfig config = configOpt.get();
                String decryptedValue = decryptValue(config.getConfigValue());
                config.setConfigValue(decryptedValue);
            }
            
            return configOpt;
        } catch (Exception e) {
            logger.error("根据配置键获取系统配置失败: {}", configKey, e);
            throw new RuntimeException("根据配置键获取系统配置失败", e);
        }
    }
    
    /**
     * 获取配置值（字符串类型）
     */
    @Cacheable(value = "systemConfigValue", key = "#configKey")
    @Transactional(readOnly = true)
    public String getConfigValue(String configKey) {
        return getConfigByKey(configKey)
            .map(SystemConfig::getConfigValue)
            .orElse(null);
    }
    
    /**
     * 获取配置值（字符串类型，带默认值）
     */
    @Cacheable(value = "systemConfigValue", key = "#configKey + '_' + #defaultValue")
    @Transactional(readOnly = true)
    public String getConfigValue(String configKey, String defaultValue) {
        return getConfigByKey(configKey)
            .map(SystemConfig::getConfigValue)
            .orElse(defaultValue);
    }
    
    /**
     * 获取配置值（整数类型）
     */
    @Cacheable(value = "systemConfigValue", key = "#configKey + '_int'")
    @Transactional(readOnly = true)
    public Integer getConfigValueAsInt(String configKey) {
        try {
            String value = getConfigValue(configKey);
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            logger.warn("配置值无法转换为整数: {} = {}", configKey, getConfigValue(configKey));
            return null;
        }
    }
    
    /**
     * 获取配置值（整数类型，带默认值）
     */
    @Cacheable(value = "systemConfigValue", key = "#configKey + '_int_' + #defaultValue")
    @Transactional(readOnly = true)
    public Integer getConfigValueAsInt(String configKey, Integer defaultValue) {
        Integer value = getConfigValueAsInt(configKey);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取配置值（布尔类型）
     */
    @Cacheable(value = "systemConfigValue", key = "#configKey + '_bool'")
    @Transactional(readOnly = true)
    public Boolean getConfigValueAsBoolean(String configKey) {
        String value = getConfigValue(configKey);
        if (value == null) {
            return null;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }
    
    /**
     * 获取配置值（布尔类型，带默认值）
     */
    @Cacheable(value = "systemConfigValue", key = "#configKey + '_bool_' + #defaultValue")
    @Transactional(readOnly = true)
    public Boolean getConfigValueAsBoolean(String configKey, Boolean defaultValue) {
        Boolean value = getConfigValueAsBoolean(configKey);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 创建新的系统配置
     */
    @CacheEvict(value = {"systemConfig", "systemConfigValue"}, allEntries = true)
    public SystemConfig createConfig(SystemConfig config) {
        try {
            // 验证配置键是否已存在
            if (systemConfigRepository.findByConfigKey(config.getConfigKey()).isPresent()) {
                throw new IllegalArgumentException("配置键已存在: " + config.getConfigKey());
            }
            
            // 设置创建和更新时间
            LocalDateTime now = LocalDateTime.now();
            config.setCreatedAt(now);
            config.setUpdatedAt(now);
            
            // 如果需要加密，则加密配置值
            if (config.getIsEncrypted() != null && config.getIsEncrypted()) {
                String encryptedValue = encryptValue(config.getConfigValue());
                config.setConfigValue(encryptedValue);
            }
            
            SystemConfig savedConfig = systemConfigRepository.save(config);
            logger.info("创建系统配置成功: {}", config.getConfigKey());
            
            return savedConfig;
            
        } catch (Exception e) {
            logger.error("创建系统配置失败: {}", config.getConfigKey(), e);
            throw new RuntimeException("创建系统配置失败", e);
        }
    }
    
    /**
     * 更新系统配置
     */
    @CacheEvict(value = {"systemConfig", "systemConfigValue"}, allEntries = true)
    public SystemConfig updateConfig(Long configId, SystemConfig configUpdate) {
        try {
            Optional<SystemConfig> existingConfigOpt = systemConfigRepository.findById(configId);
            if (existingConfigOpt.isEmpty()) {
                throw new IllegalArgumentException("系统配置不存在: " + configId);
            }
            
            SystemConfig existingConfig = existingConfigOpt.get();
            
            // 更新字段
            if (StringUtils.hasText(configUpdate.getConfigValue())) {
                String newValue = configUpdate.getConfigValue();
                
                // 如果需要加密，则加密新值
                if (existingConfig.getIsEncrypted() != null && existingConfig.getIsEncrypted()) {
                    newValue = encryptValue(newValue);
                }
                
                existingConfig.setConfigValue(newValue);
            }
            
            if (StringUtils.hasText(configUpdate.getDescription())) {
                existingConfig.setDescription(configUpdate.getDescription());
            }
            
            if (configUpdate.getIsActive() != null) {
                existingConfig.setIsActive(configUpdate.getIsActive());
            }
            
            if (configUpdate.getIsEncrypted() != null) {
                // 如果加密状态发生变化，需要重新处理配置值
                boolean wasEncrypted = existingConfig.getIsEncrypted() != null && existingConfig.getIsEncrypted();
                boolean willBeEncrypted = configUpdate.getIsEncrypted();
                
                if (!wasEncrypted && willBeEncrypted) {
                    // 从未加密变为加密
                    String encryptedValue = encryptValue(existingConfig.getConfigValue());
                    existingConfig.setConfigValue(encryptedValue);
                } else if (wasEncrypted && !willBeEncrypted) {
                    // 从加密变为未加密
                    String decryptedValue = decryptValue(existingConfig.getConfigValue());
                    existingConfig.setConfigValue(decryptedValue);
                }
                
                existingConfig.setIsEncrypted(willBeEncrypted);
            }
            
            existingConfig.setUpdatedAt(LocalDateTime.now());
            
            SystemConfig savedConfig = systemConfigRepository.save(existingConfig);
            logger.info("更新系统配置成功: {}", existingConfig.getConfigKey());
            
            return savedConfig;
            
        } catch (Exception e) {
            logger.error("更新系统配置失败: {}", configId, e);
            throw new RuntimeException("更新系统配置失败", e);
        }
    }
    
    /**
     * 删除系统配置
     */
    @CacheEvict(value = {"systemConfig", "systemConfigValue"}, allEntries = true)
    public void deleteConfig(Long configId) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findById(configId);
            if (configOpt.isEmpty()) {
                throw new IllegalArgumentException("系统配置不存在: " + configId);
            }
            
            SystemConfig config = configOpt.get();
            systemConfigRepository.deleteById(configId);
            
            logger.info("删除系统配置成功: {}", config.getConfigKey());
            
        } catch (Exception e) {
            logger.error("删除系统配置失败: {}", configId, e);
            throw new RuntimeException("删除系统配置失败", e);
        }
    }
    
    /**
     * 根据配置键删除配置
     */
    @CacheEvict(value = {"systemConfig", "systemConfigValue"}, allEntries = true)
    public void deleteConfigByKey(String configKey) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(configKey);
            if (configOpt.isEmpty()) {
                throw new IllegalArgumentException("系统配置不存在: " + configKey);
            }
            
            systemConfigRepository.delete(configOpt.get());
            logger.info("根据配置键删除系统配置成功: {}", configKey);
            
        } catch (Exception e) {
            logger.error("根据配置键删除系统配置失败: {}", configKey, e);
            throw new RuntimeException("根据配置键删除系统配置失败", e);
        }
    }
    
    /**
     * 批量更新配置
     */
    @CacheEvict(value = {"systemConfig", "systemConfigValue"}, allEntries = true)
    public List<SystemConfig> batchUpdateConfigs(List<SystemConfig> configs) {
        try {
            List<SystemConfig> updatedConfigs = configs.stream()
                .map(config -> {
                    if (config.getId() != null) {
                        return updateConfig(config.getId(), config);
                    } else {
                        return createConfig(config);
                    }
                })
                .toList();
            
            logger.info("批量更新系统配置成功，共{}个配置", configs.size());
            return updatedConfigs;
            
        } catch (Exception e) {
            logger.error("批量更新系统配置失败", e);
            throw new RuntimeException("批量更新系统配置失败", e);
        }
    }
    
    /**
     * 清除配置缓存
     */
    @CacheEvict(value = {"systemConfig", "systemConfigValue"}, allEntries = true)
    public void clearCache() {
        logger.info("清除系统配置缓存");
    }
    
    /**
     * 获取启用的配置列表
     */
    @Transactional(readOnly = true)
    public List<SystemConfig> getEnabledConfigs() {
        try {
            return systemConfigRepository.findByIsActiveTrueOrderByConfigKey();
        } catch (Exception e) {
            logger.error("获取启用的系统配置失败", e);
            throw new RuntimeException("获取启用的系统配置失败", e);
        }
    }
    
    /**
     * 获取配置统计信息
     */
    @Transactional(readOnly = true)
    public ConfigStats getConfigStats() {
        try {
            ConfigStats stats = new ConfigStats();
            
            stats.totalConfigs = systemConfigRepository.count();
            stats.enabledConfigs = systemConfigRepository.countByIsActiveTrue();
            stats.disabledConfigs = systemConfigRepository.countByIsActiveFalse();
            stats.encryptedConfigs = systemConfigRepository.countByIsEncryptedTrue();
            stats.unencryptedConfigs = systemConfigRepository.countByIsEncryptedFalse();
            
            // 按类型统计
            for (SystemConfig.ConfigType type : SystemConfig.ConfigType.values()) {
                long count = systemConfigRepository.countByConfigType(type);
                stats.configsByType.put(type.name(), count);
            }
            
            return stats;
            
        } catch (Exception e) {
            logger.error("获取配置统计信息失败", e);
            throw new RuntimeException("获取配置统计信息失败", e);
        }
    }
    
    /**
     * 加密配置值
     */
    private String encryptValue(String value) {
        try {
            if (value == null || value.isEmpty()) {
                return value;
            }
            
            SecretKeySpec secretKey = new SecretKeySpec(
                ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), ENCRYPTION_ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
            
        } catch (Exception e) {
            logger.error("加密配置值失败", e);
            throw new RuntimeException("加密配置值失败", e);
        }
    }
    
    /**
     * 解密配置值
     */
    private String decryptValue(String encryptedValue) {
        try {
            if (encryptedValue == null || encryptedValue.isEmpty()) {
                return encryptedValue;
            }
            
            SecretKeySpec secretKey = new SecretKeySpec(
                ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), ENCRYPTION_ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("解密配置值失败", e);
            throw new RuntimeException("解密配置值失败", e);
        }
    }
    
    // ==================== 特殊配置类型处理方法 ====================
    
    /**
     * 获取JSON类型配置并转换为指定对象
     * 
     * @param configKey 配置键
     * @param clazz 目标类型
     * @return 转换后的对象
     */
    public <T> Optional<T> getJsonConfig(String configKey, Class<T> clazz) {
        try {
            Optional<SystemConfig> configOpt = getConfigByKey(configKey);
            if (configOpt.isPresent()) {
                SystemConfig config = configOpt.get();
                if (SystemConfig.ConfigType.JSON.name().equals(config.getConfigType())) {
                    T result = objectMapper.readValue(config.getConfigValue(), clazz);
                    return Optional.of(result);
                } else {
                    logger.warn("配置{}不是JSON类型，实际类型: {}", configKey, config.getConfigType());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("解析JSON配置失败: {}", configKey, e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取JSON类型配置并转换为指定泛型类型
     * 
     * @param configKey 配置键
     * @param typeReference 类型引用
     * @return 转换后的对象
     */
    public <T> Optional<T> getJsonConfig(String configKey, TypeReference<T> typeReference) {
        try {
            Optional<SystemConfig> configOpt = getConfigByKey(configKey);
            if (configOpt.isPresent()) {
                SystemConfig config = configOpt.get();
                if (SystemConfig.ConfigType.JSON.name().equals(config.getConfigType())) {
                    T result = objectMapper.readValue(config.getConfigValue(), typeReference);
                    return Optional.of(result);
                } else {
                    logger.warn("配置{}不是JSON类型，实际类型: {}", configKey, config.getConfigType());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("解析JSON配置失败: {}", configKey, e);
            return Optional.empty();
        }
    }
    
    /**
     * 设置JSON类型配置
     * 
     * @param configKey 配置键
     * @param value 要保存的对象
     * @param description 配置描述
     * @return 保存的配置
     */
    public SystemConfig setJsonConfig(String configKey, Object value, String description) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            
            SystemConfig config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(jsonValue);
            config.setConfigType(SystemConfig.ConfigType.JSON.name());
            config.setDescription(description);
            config.setIsActive(true);
            config.setIsEncrypted(false);
            
            return createConfig(config);
        } catch (Exception e) {
            logger.error("保存JSON配置失败: {}", configKey, e);
            throw new RuntimeException("保存JSON配置失败", e);
        }
    }
    
    /**
     * 获取LIST类型配置
     * 
     * @param configKey 配置键
     * @return 字符串列表
     */
    public Optional<List<String>> getListConfig(String configKey) {
        try {
            Optional<SystemConfig> configOpt = getConfigByKey(configKey);
            if (configOpt.isPresent()) {
                SystemConfig config = configOpt.get();
                if (SystemConfig.ConfigType.LIST.name().equals(config.getConfigType())) {
                    List<String> result = objectMapper.readValue(config.getConfigValue(), 
                        new TypeReference<List<String>>() {});
                    return Optional.of(result);
                } else {
                    logger.warn("配置{}不是LIST类型，实际类型: {}", configKey, config.getConfigType());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("解析LIST配置失败: {}", configKey, e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取LIST类型配置并转换为指定类型的列表
     * 
     * @param configKey 配置键
     * @param elementClass 列表元素类型
     * @return 指定类型的列表
     */
    public <T> Optional<List<T>> getListConfig(String configKey, Class<T> elementClass) {
        try {
            Optional<SystemConfig> configOpt = getConfigByKey(configKey);
            if (configOpt.isPresent()) {
                SystemConfig config = configOpt.get();
                if (SystemConfig.ConfigType.LIST.name().equals(config.getConfigType())) {
                    TypeReference<List<T>> typeRef = new TypeReference<List<T>>() {};
                    List<T> result = objectMapper.readValue(config.getConfigValue(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
                    return Optional.of(result);
                } else {
                    logger.warn("配置{}不是LIST类型，实际类型: {}", configKey, config.getConfigType());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("解析LIST配置失败: {}", configKey, e);
            return Optional.empty();
        }
    }
    
    /**
     * 设置LIST类型配置
     * 
     * @param configKey 配置键
     * @param list 要保存的列表
     * @param description 配置描述
     * @return 保存的配置
     */
    public SystemConfig setListConfig(String configKey, List<?> list, String description) {
        try {
            String jsonValue = objectMapper.writeValueAsString(list);
            
            SystemConfig config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(jsonValue);
            config.setConfigType(SystemConfig.ConfigType.LIST.name());
            config.setDescription(description);
            config.setIsActive(true);
            config.setIsEncrypted(false);
            
            return createConfig(config);
        } catch (Exception e) {
            logger.error("保存LIST配置失败: {}", configKey, e);
            throw new RuntimeException("保存LIST配置失败", e);
        }
    }
    
    /**
     * 获取ENCRYPTED类型配置（自动解密）
     * 
     * @param configKey 配置键
     * @return 解密后的配置值
     */
    public Optional<String> getEncryptedConfig(String configKey) {
        try {
            Optional<SystemConfig> configOpt = getConfigByKey(configKey);
            if (configOpt.isPresent()) {
                SystemConfig config = configOpt.get();
                if (SystemConfig.ConfigType.ENCRYPTED.name().equals(config.getConfigType())) {
                    // ENCRYPTED类型的配置值已经在getConfigByKey中自动解密了
                    return Optional.of(config.getConfigValue());
                } else {
                    logger.warn("配置{}不是ENCRYPTED类型，实际类型: {}", configKey, config.getConfigType());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("获取ENCRYPTED配置失败: {}", configKey, e);
            return Optional.empty();
        }
    }
    
    /**
     * 设置ENCRYPTED类型配置（自动加密）
     * 
     * @param configKey 配置键
     * @param value 要加密保存的值
     * @param description 配置描述
     * @return 保存的配置
     */
    public SystemConfig setEncryptedConfig(String configKey, String value, String description) {
        try {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(value);
            config.setConfigType(SystemConfig.ConfigType.ENCRYPTED.name());
            config.setDescription(description);
            config.setIsActive(true);
            config.setIsEncrypted(true); // 标记为需要加密
            
            return createConfig(config);
        } catch (Exception e) {
            logger.error("保存ENCRYPTED配置失败: {}", configKey, e);
            throw new RuntimeException("保存ENCRYPTED配置失败", e);
        }
    }
    
    /**
     * 验证配置值是否符合指定类型
     * 
     * @param configValue 配置值
     * @param configType 配置类型
     * @return 是否有效
     */
    public boolean validateConfigValue(String configValue, SystemConfig.ConfigType configType) {
        if (configValue == null || configValue.trim().isEmpty()) {
            return false;
        }
        
        try {
            switch (configType) {
                case STRING:
                    return true; // 字符串类型总是有效的
                    
                case NUMBER:
                    try {
                        Double.parseDouble(configValue.trim());
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                    
                case BOOLEAN:
                    String lowerValue = configValue.trim().toLowerCase();
                    return "true".equals(lowerValue) || "false".equals(lowerValue) ||
                           "1".equals(lowerValue) || "0".equals(lowerValue) ||
                           "yes".equals(lowerValue) || "no".equals(lowerValue);
                           
                case JSON:
                    try {
                        objectMapper.readTree(configValue);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                    
                case LIST:
                    try {
                        objectMapper.readValue(configValue, List.class);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                    
                case ENCRYPTED:
                    return true; // 加密类型的值在存储前会被加密，所以总是有效的
                    
                default:
                    return false;
            }
        } catch (Exception e) {
            logger.error("验证配置值失败: {}", configValue, e);
            return false;
        }
    }
    
    /**
     * 转换配置值为指定类型
     * 
     * @param configValue 配置值
     * @param configType 配置类型
     * @param targetClass 目标类型
     * @return 转换后的值
     */
    @SuppressWarnings("unchecked")
    public <T> T convertConfigValue(String configValue, SystemConfig.ConfigType configType, Class<T> targetClass) {
        if (configValue == null) {
            return null;
        }
        
        try {
            switch (configType) {
                case STRING:
                    if (targetClass == String.class) {
                        return (T) configValue;
                    }
                    break;
                    
                case NUMBER:
                    if (targetClass == Integer.class) {
                        return (T) Integer.valueOf(configValue.trim());
                    } else if (targetClass == Long.class) {
                        return (T) Long.valueOf(configValue.trim());
                    } else if (targetClass == Double.class) {
                        return (T) Double.valueOf(configValue.trim());
                    } else if (targetClass == Float.class) {
                        return (T) Float.valueOf(configValue.trim());
                    }
                    break;
                    
                case BOOLEAN:
                    if (targetClass == Boolean.class) {
                        String lowerValue = configValue.trim().toLowerCase();
                        boolean result = "true".equals(lowerValue) || "1".equals(lowerValue) || "yes".equals(lowerValue);
                        return (T) Boolean.valueOf(result);
                    }
                    break;
                    
                case JSON:
                    return objectMapper.readValue(configValue, targetClass);
                    
                case LIST:
                    if (List.class.isAssignableFrom(targetClass)) {
                        return objectMapper.readValue(configValue, targetClass);
                    }
                    break;
                    
                case ENCRYPTED:
                    if (targetClass == String.class) {
                        return (T) configValue; // 已经解密的值
                    }
                    break;
            }
            
            throw new IllegalArgumentException("无法将配置类型 " + configType + " 转换为 " + targetClass.getSimpleName());
            
        } catch (Exception e) {
            logger.error("转换配置值失败: {} -> {}", configType, targetClass.getSimpleName(), e);
            throw new RuntimeException("转换配置值失败", e);
        }
    }
    
    /**
     * 配置统计信息类
     */
    public static class ConfigStats {
        public long totalConfigs;
        public long enabledConfigs;
        public long disabledConfigs;
        public long encryptedConfigs;
        public long unencryptedConfigs;
        public java.util.Map<String, Long> configsByType = new java.util.HashMap<>();
    }
}
