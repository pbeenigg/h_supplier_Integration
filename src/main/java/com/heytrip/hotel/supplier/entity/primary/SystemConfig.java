package com.heytrip.hotel.supplier.entity.primary;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 系统配置实体类
 * 存储系统级别的配置参数，支持动态配置管理
 * 
 * @author  Pax
 */
@Entity
@Table(name = "system_config", indexes = {
    @Index(name = "idx_config_key", columnList = "configKey"),
    @Index(name = "idx_config_type", columnList = "configType"),
    @Index(name = "idx_is_active", columnList = "isActive")
})
@Data
public class SystemConfig {
    
    /**
     * 系统配置ID，主键自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("系统配置ID，主键自增")
    private Long id;
    
    /**
     * 配置键名，唯一标识
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 200)
    @Comment("配置键名，唯一标识")
    private String configKey;
    
    /**
     * 配置值
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    @Comment("配置值")
    private String configValue;
    
    /**
     * 配置类型：STRING、NUMBER、BOOLEAN、JSON等
     */
    @Column(name = "config_type", nullable = false, length = 50)
    @Comment("配置类型：STRING、NUMBER、BOOLEAN、JSON等")
    private String configType = "STRING";
    
    /**
     * 配置描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @Comment("配置描述")
    private String description;
    
    /**
     * 是否加密存储：1-是，0-否
     */
    @Column(name = "is_encrypted", nullable = false)
    @Comment("是否加密存储：1-是，0-否")
    private Boolean isEncrypted = false;
    
    /**
     * 是否启用：1-启用，0-禁用
     */
    @Column(name = "is_active", nullable = false)
    @Comment("是否启用：1-启用，0-禁用")
    private Boolean isActive = true;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Comment("更新时间")
    private LocalDateTime updatedAt;
    
    /**
     * 创建人
     */
    @Column(name = "created_by", length = 100)
    @Comment("创建人")
    private String createdBy = "admin";
    
    /**
     * 更新人
     */
    @Column(name = "updated_by", length = 100)
    @Comment("更新人")
    private String updatedBy = "admin";
    
    // 默认构造函数
    public SystemConfig() {}
    
    // 带参构造函数
    public SystemConfig(String configKey, String configValue, String configType) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
    }
    
    // 完整构造函数
    public SystemConfig(String configKey, String configValue, String configType, 
                       String description, Boolean isEncrypted, Boolean isActive) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.description = description;
        this.isEncrypted = isEncrypted;
        this.isActive = isActive;
    }
    
    /**
     * 配置类型枚举
     */
    public enum ConfigType {
        STRING("STRING", "字符串类型"),
        NUMBER("NUMBER", "数字类型"),
        BOOLEAN("BOOLEAN", "布尔类型"),
        JSON("JSON", "JSON对象类型"),
        LIST("LIST", "列表类型"),
        ENCRYPTED("ENCRYPTED", "加密字符串类型");
        
        private final String code;
        private final String description;
        
        ConfigType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 获取配置值并转换为指定类型
     */
    public <T> T getValueAs(Class<T> type) {
        if (configValue == null) {
            return null;
        }
        
        try {
            if (type == String.class) {
                return type.cast(configValue);
            } else if (type == Integer.class) {
                return type.cast(Integer.valueOf(configValue));
            } else if (type == Long.class) {
                return type.cast(Long.valueOf(configValue));
            } else if (type == Boolean.class) {
                return type.cast(Boolean.valueOf(configValue));
            } else if (type == Double.class) {
                return type.cast(Double.valueOf(configValue));
            }
        } catch (Exception e) {
            // 转换失败时返回null
            return null;
        }
        
        return null;
    }
    
    /**
     * 获取字符串类型配置值
     */
    public String getStringValue() {
        return configValue;
    }
    
    /**
     * 获取整数类型配置值
     */
    public Integer getIntValue() {
        return getValueAs(Integer.class);
    }
    
    /**
     * 获取长整数类型配置值
     */
    public Long getLongValue() {
        return getValueAs(Long.class);
    }
    
    /**
     * 获取布尔类型配置值
     */
    public Boolean getBooleanValue() {
        return getValueAs(Boolean.class);
    }
    
    /**
     * 获取双精度浮点数类型配置值
     */
    public Double getDoubleValue() {
        return getValueAs(Double.class);
    }
}
