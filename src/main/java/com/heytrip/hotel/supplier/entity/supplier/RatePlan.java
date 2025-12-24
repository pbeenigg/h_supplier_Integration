package com.heytrip.hotel.supplier.entity.supplier;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 静态数据-价格计划
 * 约束： (supplier_id, supplier_code, rate_plan_code) 唯一
 * 参考标准实体：com.heytrip.common.response.basic.XRatePlan
 */
@Data
@Entity
@Table(name = "rate_plan",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rateplan_supplier_code", columnNames = {"supplier_id", "supplier_code", "rate_plan_code"})
        },
        indexes = {
                @Index(name = "idx_rateplan_supplier", columnList = "supplier_id,supplier_code"),
                @Index(name = "idx_rateplan_code", columnList = "rate_plan_code"),
                @Index(name = "idx_rateplan_hotel", columnList = "hotel_code"),
                @Index(name = "idx_rate_plan_code_md5", columnList = "rate_plan_code_md5")
        }
)
public class RatePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID")
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    @Comment("供应商ID")
    private Long supplierId;

    @Column(name = "supplier_code", length = 100, nullable = false)
    @Comment("供应商代码")
    private String supplierCode;

    @Column(name = "rate_plan_code", length = 200, nullable = false)
    @Comment("价格计划编码")
    private String ratePlanCode;

    @Column(name = "rate_plan_code_md5", length = 64, nullable = false)
    @Comment("价格计划编码MD5")
    private String ratePlanCodeMd5;

    @Column(name = "hotel_id", length = 64, nullable = false)
    @Comment("酒店ID")
    private Long hotelId;

    @Column(name = "hotel_code", length = 200, nullable = false)
    @Comment("酒店编码")
    private String hotelCode;

    @Column(name = "room_code", length = 64)
    @Comment("所属房型编码")
    private String roomCode;

    @Column(name = "name", length = 300)
    @Comment("价格计划名称")
    private String name;

    @Column(name = "meal", length = 100)
    @Comment("餐食类型")
    private String meal;

    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    @Comment("取消政策")
    private String cancellationPolicy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "TINYINT(1)")
    @Comment("逻辑删除：0-否，1-是")
    private Boolean isDeleted = false;
}
