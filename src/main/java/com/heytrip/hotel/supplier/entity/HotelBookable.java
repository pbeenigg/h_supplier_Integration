package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可预定酒店表
 * 存储有价格的可预定酒店信息，与静态酒店表分离
 * 约束：(supplier_id, supplier_code, hotel_code) 唯一
 */
@Data
@Entity
@Table(name = "hotel_bookable",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hotel_bookable_supplier_code", 
                    columnNames = {"supplier_id", "supplier_code", "hotel_code"})
        },
        indexes = {
                @Index(name = "idx_hotel_bookable_supplier", columnList = "supplier_id,supplier_code"),
                @Index(name = "idx_hotel_bookable_code", columnList = "hotel_code"),
                @Index(name = "idx_hotel_bookable_code_md5", columnList = "hotel_code_md5"),
                @Index(name = "idx_hotel_bookable_hotel_id", columnList = "hotel_id"),
                @Index(name = "idx_hotel_bookable_bookable", columnList = "is_bookable"),
        }
)
public class HotelBookable {

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

    @Column(name = "hotel_code", length = 200, nullable = false)
    @Comment("酒店代码")
    private String hotelCode;

    @Column(name = "hotel_code_md5", length = 64, nullable = false)
    @Comment("酒店代码MD5")
    private String hotelCodeMd5;

    @Column(name = "hotel_id")
    @Comment("酒店表主键ID（关联Hotel表）")
    private Long hotelId;

    @Column(name = "name", length = 500)
    @Comment("酒店名称")
    private String name;

    @Column(name = "min_price", precision = 10, scale = 2)
    @Comment("最低价格")
    private BigDecimal minPrice;

    @Column(name = "is_bookable", nullable = false)
    @Comment("是否可预定")
    private Boolean isBookable = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Comment("更新时间")
    private LocalDateTime updatedAt;
}
