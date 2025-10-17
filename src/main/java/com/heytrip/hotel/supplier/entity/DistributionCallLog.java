package com.heytrip.hotel.supplier.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 分销商API调用日志实体
 * 对应distribution_call_log表
 *
 * @author Pax
 * @since 1.0.0
 */
@Entity
@Table(name = "distribution_call_log", indexes = {
        @Index(name = "idx_supplier_id", columnList = "supplier_id"),
        @Index(name = "idx_is_success", columnList = "is_success"),
        @Index(name = "idx_business_type", columnList = "business_type"),
        @Index(name = "idx_trace_id", columnList = "trace_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Comment("分销商调用日志表")
@Data
public class DistributionCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("API调用日志ID，主键自增")
    private Long id;

    @Column(name = "app_id", length = 100)
    @Comment("应用ID标识")
    private String appId;

    @Column(name = "supplier_id")
    @Comment("供应商ID，关联supplier_config表")
    private Long supplierId;


    @Transient
    @Comment("供应商编码，仅用于数据转换，不持久化到数据库")
    private String supplierCode;


    @Column(name = "trace_id", length = 100)
    @Comment("链路追踪ID，用于追踪请求链路")
    private String traceId;

    @Column(name = "api_endpoint", columnDefinition = "TEXT")
    @Comment("API端点路径")
    private String apiEndpoint;

    @Column(name = "http_method", length = 10, nullable = false)
    @Comment("HTTP请求方法：GET、POST、PUT、DELETE等")
    private String httpMethod;

    @Column(name = "request_headers", columnDefinition = "JSON")
    @Comment("请求头信息，JSON格式")
    private String requestHeaders;

    @Column(name = "request_params", columnDefinition = "JSON")
    @Comment("请求参数，JSON格式")
    private String requestParams;

    @Column(name = "request_body", columnDefinition = "LONGTEXT")
    @Comment("请求体内容")
    private String requestBody;

    @Column(name = "request_body_compressed")
    @Comment("请求体是否压缩：1-已压缩，0-未压缩")
    private Boolean requestBodyCompressed = false;

    @Column(name = "response_body_compressed")
    @Comment("响应体是否压缩：1-已压缩，0-未压缩")
    private Boolean responseBodyCompressed = false;

    @Column(name = "response_headers", columnDefinition = "JSON")
    @Comment("响应头信息，JSON格式")
    private String responseHeaders;

    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    @Comment("响应体内容")
    private String responseBody;


    @Column(name = "response_status")
    @Comment("HTTP响应状态码")
    private Integer responseStatus;

    @Column(name = "response_time_ms")
    @Comment("响应时间，单位毫秒")
    private Long responseTimeMs;

    @Column(name = "error_code", length = 50)
    @Comment("错误代码")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    @Comment("错误信息详情")
    private String errorMessage;

    @Column(name = "is_success")
    @Comment("是否成功：1-成功，0-失败")
    private Boolean isSuccess = false;

    @Column(name = "business_type", length = 100)
    @Comment("业务类型：queryOrder、createOrder、cancelOrder、getPrice、getPrices、getHotel、orderCheck、modifyOrder等")
    private String businessType;

    @Column(name = "hotel_key", length = 100)
    @Comment("酒店标识")
    private String hotelKey;

    @Column(name = "check_in_key", length = 100)
    @Comment("入住标识")
    private String checkInKey;

    @Column(name = "check_out_key", length = 100)
    @Comment("离店标识")
    private String checkOutKey;

    @Column(name = "distribution_orders_key", length = 100)
    @Comment("分销商订单号标识")
    private String distributionOrdersKey;

    @Column(name = "client_ip", length = 45)
    @Comment("客户端IP地址，支持IPv6")
    private String clientIp;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    @Comment("用户代理信息")
    private String userAgent;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Comment("创建时间")
    private LocalDateTime createdAt;



    // 关联供应商配置
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    @JsonIgnore
    private SupplierConfig supplierConfig;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


}
