package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 供应商配置实体类
 * 存储供应商基础配置信息，包括API连接、认证、限流等配置
 * 
 * @author  Pax
 */
@Entity
@Table(name = "supplier_config", indexes = {
    @Index(name = "idx_supplier_name", columnList = "supplierName"),
    @Index(name = "idx_supplier_code", columnList = "supplierCode"),
    @Index(name = "idx_is_active", columnList = "isActive"),
    @Index(name = "idx_priority", columnList = "priority"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
public class SupplierConfig {

    /**
     * 供应商配置ID，主键自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("供应商配置ID，主键自增")
    private Long id;

    /**
     * 供应商名称，唯一标识
     */
    @Column(name = "supplier_name", nullable = false, unique = true, length = 100)
    @Comment("供应商名称，唯一标识")
    private String supplierName;

    /**
     * 供应商代码，用于系统内部标识
     */
    @Column(name = "supplier_code", nullable = false, unique = true, length = 50)
    @Comment("供应商代码，用于系统内部标识")
    private String supplierCode;

    /**
     * 供应商API基础URL地址
     */
    @Column(name = "api_base_url", nullable = false, length = 500)
    @Comment("供应商API基础URL地址")
    private String apiBaseUrl;

    /**
     * 认证类型：MD5、SHA256、JWT、OAUTH等
     */
    @Column(name = "auth_type", nullable = false, length = 50)
    @Comment("认证类型：MD5、SHA256、JWT、OAUTH等")
    private String authType = "MD5";

    /**
     * 认证配置信息，JSON格式存储appId、secretKey等
     */
    @Column(name = "auth_config", columnDefinition = "JSON")
    @Comment("认证配置信息，JSON格式存储appId、secretKey等")
    private String authConfig;

    /**
     * API调用超时时间，单位毫秒
     */
    @Column(name = "timeout_ms", nullable = false)
    @Comment("API调用超时时间，单位毫秒")
    private Long timeoutMs = 30000L;

    /**
     * API调用失败重试次数
     */
    @Column(name = "retry_count", nullable = false)
    @Comment("API调用失败重试次数")
    private Integer retryCount = 3;

    /**
     * 最大并发请求数
     */
    @Column(name = "max_concurrent_requests", nullable = false)
    @Comment("最大并发请求数")
    private Integer maxConcurrentRequests = 10;

    /**
     * 每秒请求限制数
     */
    @Column(name = "rate_limit_per_second", nullable = false)
    @Comment("每秒请求限制数")
    private Integer rateLimitPerSecond = 100;

    /**
     * 是否启用：1-启用，0-禁用
     */
    @Column(name = "is_active", nullable = false)
    @Comment("是否启用：1-启用，0-禁用")
    private Boolean isActive = true;

    /**
     * 优先级，数值越小优先级越高
     */
    @Column(name = "priority", nullable = false)
    @Comment("优先级，数值越小优先级越高")
    private Integer priority = 100;

    /**
     * 供应商描述信息
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @Comment("供应商描述信息")
    private String description;

    /**
     * 联系信息，包含邮箱、电话、联系人等，JSON格式
     */
    @Column(name = "contact_info", columnDefinition = "JSON")
    @Comment("联系信息，包含邮箱、电话、联系人等")
    private String contactInfo;

    /**
     * 支持的国家列表，JSON数组格式
     */
    @Column(name = "supported_countries", columnDefinition = "JSON")
    @Comment("支持的国家列表，JSON数组格式")
    private String supportedCountries;

    /**
     * 支持的城市列表，JSON数组格式
     */
    @Column(name = "supported_cities", columnDefinition = "JSON")
    @Comment("支持的城市列表，JSON数组格式")
    private String supportedCities;

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
    private String createdBy = "system";

    /**
     * 更新人
     */
    @Column(name = "updated_by", length = 100)
    @Comment("更新人")
    private String updatedBy = "system";

    // 默认构造函数
    public SupplierConfig() {}

    // 带参构造函数
    public SupplierConfig(String supplierName, String supplierCode, String apiBaseUrl, String authType) {
        this.supplierName = supplierName;
        this.supplierCode = supplierCode;
        this.apiBaseUrl = apiBaseUrl;
        this.authType = authType;
    }

}
