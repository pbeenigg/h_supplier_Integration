package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 静态数据-国籍
 * 约束： (supplier_id, supplier_code, nationality_code) 唯一
 * 文档字段：id, nationality_code, nationality, iso_code
 */
@Data
@Entity
@Table(name = "nationality",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_nat_supplier_code", columnNames = {"supplier_id", "supplier_code", "nationality_code"})
        },
        indexes = {
                @Index(name = "idx_nat_supplier", columnList = "supplier_id,supplier_code"),
                @Index(name = "idx_nat_code", columnList = "nationality_code")
        }
)
public class Nationality {

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

    @Column(name = "nationality_code", length = 50, nullable = false)
    @Comment("国籍代码")
    private String nationalityCode;

    @Column(name = "nationality", length = 200)
    @Comment("国籍名称")
    private String nationality;

    @Column(name = "iso_code", length = 10)
    @Comment("ISO代码")
    private String isoCode;

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
