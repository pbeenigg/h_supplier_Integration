package com.heytrip.hotel.supplier.entity.supplier;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.heytrip.hotel.supplier.entity.primary.SupplierConfig;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分销商订单日志实体
 * 对应distribution_orders_log表
 *
 * @author Pax
 * @since 1.0.0
 */
@Entity
@Table(name = "distribution_orders_log", indexes = {
    @Index(name = "idx_supplier_id", columnList = "supplier_id"),
    @Index(name = "idx_is_success", columnList = "is_success"),
    @Index(name = "idx_business_type", columnList = "business_type"),
    @Index(name = "idx_trace_id", columnList = "trace_id"),
    @Index(name = "idx_distribution_orders_key", columnList = "distribution_orders_key"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Comment("分销商订单日志表")
@Data
public class DistributionOrdersLog {

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

    @Column(name = "hotel_key", length = 100)
    @Comment("酒店标识")
    private String hotelKey;

    @Column(name = "check_in_key", length = 100)
    @Comment("入住标识")
    private String checkInKey;

    @Column(name = "check_out_key", length = 100)
    @Comment("离店标识")
    private String checkOutKey;

    @Column(name = "room_key", length = 100)
    @Comment("房型标识")
    private String roomKey;

    @Column(name = "rate_key", length = 100)
    @Comment("房价标识")
    private String rateKey;

    @Column(name = "nights")
    @Comment("入住晚数")
    private Integer nights;

    @Column(name = "guests")
    @Comment("入住人数")
    private Integer guests;

    @Column(name = "rooms")
    @Comment("预订房间数")
    private Integer rooms;

    @Column(name = "occupancy", length = 50)
    @Comment("入住人信息 2-5-3代表2成人2个儿童（1个5岁，1个3岁） 多间房下滑线_分割")
    private String occupancy;

    @Column(name = "currency", length = 10)
    @Comment("货币代码，如CNY、USD等")
    private String currency;

    @Column(name = "national", length = 10)
    @Comment("国家代码，如CN、US等")
    private String national;

    @Column(name = "total_amount", precision = 10, scale = 2)
    @Comment("总订单金额")
    private BigDecimal totalAmount;

    @Column(name = "sale_amount", precision = 10, scale = 2)
    @Comment("销售总金额")
    private BigDecimal saleAmount;

    @Column(name = "cancel_amount", precision = 10, scale = 2)
    @Comment("取消预定金额")
    private BigDecimal cancelAmount;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    @Comment("退款金额")
    private BigDecimal refundAmount;

    @Column(name = "refundable")
    @Comment("是否可退款 0-可退款 1-可退款")
    private Boolean refundable = false;

    @Column(name = "distribution_orders_key", length = 100)
    @Comment("分销商订单号标识")
    private String distributionOrdersKey;

    @Column(name = "supplier_booking_key", length = 100)
    @Comment("供应商预定标识")
    private String supplierBookingKey;

    @Column(name = "booking_status", length = 50)
    @Comment("预定状态")
    private String bookingStatus;

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
    @Comment("业务类型：createOrder、cancelOrder、orderCheck、modifyOrde等")
    private String businessType;

    @Column(name = "orginal_request", columnDefinition = "LONGTEXT")
    @Comment("原始请求内容")
    private String originalRequest;

    @Column(name = "orginal_response", columnDefinition = "LONGTEXT")
    @Comment("原始响应内容")
    private String originalResponse;


    @Column(name = "request_body_compressed")
    @Comment("请求体是否压缩：1-已压缩，0-未压缩")
    private Boolean requestBodyCompressed = false;

    @Column(name = "response_body_compressed")
    @Comment("响应体是否压缩：1-已压缩，0-未压缩")
    private Boolean responseBodyCompressed = false;


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
