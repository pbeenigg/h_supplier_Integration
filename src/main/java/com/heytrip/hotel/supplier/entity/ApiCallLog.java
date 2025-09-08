package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * API调用日志实体类
 * 记录所有API调用的详细信息，用于监控、性能分析和问题排查
 * 
 * @author  Pax
 */
@Entity
@Table(name = "api_call_log", indexes = {
    @Index(name = "idx_supplier_id", columnList = "supplierId"),
    @Index(name = "idx_trace_id", columnList = "traceId"),
    @Index(name = "idx_api_endpoint", columnList = "apiEndpoint"),
    @Index(name = "idx_http_method", columnList = "httpMethod"),
    @Index(name = "idx_response_status", columnList = "responseStatus"),
    @Index(name = "idx_response_time_ms", columnList = "responseTimeMs"),
    @Index(name = "idx_is_success", columnList = "isSuccess"),
    @Index(name = "idx_business_type", columnList = "businessType"),
    @Index(name = "idx_channel", columnList = "channel"),
    @Index(name = "idx_app_id", columnList = "appId"),
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_supplier_endpoint_time", columnList = "supplierId, apiEndpoint, createdAt"),
    @Index(name = "idx_status_time", columnList = "responseStatus, createdAt")
})
@Data
public class ApiCallLog {
    
    /**
     * API调用日志ID，主键自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("API调用日志ID，主键自增")
    private Long id;
    
    /**
     * 供应商ID，关联supplier_config表
     */
    @Column(name = "supplier_id")
    @Comment("供应商ID，关联supplier_config表")
    private Long supplierId;
    
    /**
     * 链路追踪ID，用于分布式追踪
     */
    @Column(name = "trace_id", length = 100)
    @Comment("链路追踪ID，用于分布式追踪")
    private String traceId;
    
    /**
     * API端点路径
     */
    @Column(name = "api_endpoint", nullable = false, length = 500)
    @Comment("API端点路径")
    private String apiEndpoint;
    
    /**
     * HTTP请求方法：GET、POST、PUT、DELETE等
     */
    @Column(name = "http_method", nullable = false, length = 10)
    @Comment("HTTP请求方法：GET、POST、PUT、DELETE等")
    private String httpMethod;
    
    /**
     * 请求头信息，JSON格式
     */
    @Column(name = "request_headers", columnDefinition = "JSON")
    @Comment("请求头信息，JSON格式")
    private String requestHeaders;
    
    /**
     * 请求参数，JSON格式
     */
    @Column(name = "request_params", columnDefinition = "JSON")
    @Comment("请求参数，JSON格式")
    private String requestParams;
    
    /**
     * 请求体内容
     */
    @Column(name = "request_body", columnDefinition = "LONGTEXT")
    @Comment("请求体内容")
    private String requestBody;
    
    /**
     * 响应头信息，JSON格式
     */
    @Column(name = "response_headers", columnDefinition = "JSON")
    @Comment("响应头信息，JSON格式")
    private String responseHeaders;
    
    /**
     * 响应体内容
     */
    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    @Comment("响应体内容")
    private String responseBody;
    
    /**
     * HTTP响应状态码
     */
    @Column(name = "response_status")
    @Comment("HTTP响应状态码")
    private Integer responseStatus;
    
    /**
     * 响应时间，单位毫秒
     */
    @Column(name = "response_time_ms")
    @Comment("响应时间，单位毫秒")
    private Long responseTimeMs;
    
    /**
     * 错误代码
     */
    @Column(name = "error_code", length = 50)
    @Comment("错误代码")
    private String errorCode;
    
    /**
     * 错误信息详情
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    @Comment("错误信息详情")
    private String errorMessage;
    
    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    @Comment("重试次数")
    private Integer retryCount = 0;
    
    /**
     * 是否成功：1-成功，0-失败
     */
    @Column(name = "is_success")
    @Comment("是否成功：1-成功，0-失败")
    private Boolean isSuccess = false;
    
    /**
     * 业务类型：search、booking、cancel等
     */
    @Column(name = "business_type", length = 100)
    @Comment("业务类型：search、booking、cancel等")
    private String businessType;
    
    /**
     * 调用渠道：API、WEB、MOBILE等
     */
    @Column(name = "channel", length = 50)
    @Comment("调用渠道：API、WEB、MOBILE等")
    private String channel;
    
    /**
     * 客户端IP地址，支持IPv6
     */
    @Column(name = "client_ip", length = 45)
    @Comment("客户端IP地址，支持IPv6")
    private String clientIp;
    
    /**
     * 用户代理信息
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    @Comment("用户代理信息")
    private String userAgent;
    
    /**
     * 应用ID标识
     */
    @Column(name = "app_id", length = 100)
    @Comment("应用ID标识")
    private String appId;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", length = 100)
    @Comment("用户ID")
    private String userId;
    
    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 100)
    @Comment("会话ID")
    private String sessionId;
    
    /**
     * 请求大小，单位字节
     */
    @Column(name = "request_size_bytes")
    @Comment("请求大小，单位字节")
    private Long requestSizeBytes;
    
    /**
     * 响应大小，单位字节
     */
    @Column(name = "response_size_bytes")
    @Comment("响应大小，单位字节")
    private Long responseSizeBytes;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;
    
    // 关联供应商配置
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private SupplierConfig supplierConfig;
    
    // 默认构造函数
    public ApiCallLog() {}

    // 带参构造函数
    public ApiCallLog(Long supplierId, String apiEndpoint, String httpMethod) {
        this.supplierId = supplierId;
        this.apiEndpoint = apiEndpoint;
        this.httpMethod = httpMethod;
    }
    
    // 带链路追踪ID的构造函数
    public ApiCallLog(Long supplierId, String traceId, String apiEndpoint, String httpMethod, String businessType) {
        this.supplierId = supplierId;
        this.traceId = traceId;
        this.apiEndpoint = apiEndpoint;
        this.httpMethod = httpMethod;
        this.businessType = businessType;
    }

}
