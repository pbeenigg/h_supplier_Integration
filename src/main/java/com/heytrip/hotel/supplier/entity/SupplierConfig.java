package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 供应商配置实体类
 * 存储各个供应商的配置信息，包括API端点、认证信息、超时设置等
 * 
 * @author  Pax
 */
@Entity
@Table(name = "supplier_config")
@Data
public class SupplierConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_name", nullable = false, unique = true, length = 100)
    private String supplierName;

    @Column(name = "api_base_url", nullable = false, length = 500)
    private String apiBaseUrl;

    @Column(name = "auth_type", nullable = false, length = 50)
    private String authType;

    @Column(name = "auth_config", columnDefinition = "TEXT")
    private String authConfig;

    @Column(name = "timeout_ms")
    private Integer timeoutMs = 30;

    @Column(name = "retry_count")
    private Integer retryCount = 3;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 默认构造函数
    public SupplierConfig() {}

    // 带参构造函数
    public SupplierConfig(String supplierName, String apiBaseUrl, String authType) {
        this.supplierName = supplierName;
        this.apiBaseUrl = apiBaseUrl;
        this.authType = authType;
    }

}
