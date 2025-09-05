package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * API调用日志实体类
 * 记录所有对外部供应商API的调用情况，用于监控和问题排查
 * 
 * @author  Pax
 */
@Entity
@Table(name = "api_call_log", 
       indexes = {
           @Index(name = "idx_supplier_created", columnList = "supplier_id, created_at"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
@Data
public class ApiCallLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "supplier_id", nullable = false)
    private String supplierId;
    
    @Column(name = "api_endpoint", nullable = false, length = 500)
    private String apiEndpoint;
    
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;
    
    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;
    
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;
    
    @Column(name = "response_status")
    private Integer responseStatus;
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "channel", length = 50)
    private String channel;
    
    @Column(name = "channel_company", length = 100)
    private String channelCompany;
    
    @Column(name = "client_ip", length = 45)
    private String clientIp;
    
    @Column(name = "app_id", length = 100)
    private String appId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // 关联供应商配置
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private SupplierConfig supplierConfig;
    
    // 默认构造函数
    public ApiCallLog() {}

    // 带参构造函数
    public ApiCallLog(String supplierId, String apiEndpoint, String httpMethod) {
        this.supplierId = supplierId;
        this.apiEndpoint = apiEndpoint;
        this.httpMethod = httpMethod;
    }
    

}
