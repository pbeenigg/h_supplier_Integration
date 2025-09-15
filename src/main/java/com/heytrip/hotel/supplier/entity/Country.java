package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 静态数据-国家
 * 约束： (supplier_id, supplier_code, country_code) 唯一
 * 参考标准实体：com.heytrip.common.response.other.XCountryResponse
 */
@Data
@Entity
@Table(name = "country",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_country_supplier_code", columnNames = {"supplier_id", "supplier_code", "country_code"})
        },
        indexes = {
                @Index(name = "idx_country_supplier", columnList = "supplier_id,supplier_code"),
                @Index(name = "idx_country_code", columnList = "country_code")
        }
)
public class Country {

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

    @Column(name = "country_code", length = 20, nullable = false)
    @Comment("国家代码")
    private String countryCode;

    @Column(name = "country_name", length = 200)
    @Comment("国家名称")
    private String countryName;

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
