package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 静态数据-酒店
 * 约束： (supplier_id, supplier_code, hotel_code) 唯一
 * 参考标准实体：com.heytrip.common.response.basic.XHotel
 */
@Data
@Entity
@Table(name = "hotel",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hotel_supplier_code", columnNames = {"supplier_id", "supplier_code", "hotel_code"})
        },
        indexes = {
                @Index(name = "idx_hotel_supplier", columnList = "supplier_id,supplier_code"),
                @Index(name = "idx_hotel_code", columnList = "hotel_code"),
                @Index(name = "idx_hotel_city", columnList = "city_code"),
                @Index(name = "idx_hotel_country", columnList = "country_code"),
                @Index(name = "idx_hotel_code_md5", columnList = "hotel_code_md5"),
                @Index(name = "idx_supplier_id_supplier_code_is_bookable", columnList = "supplier_id,supplier_code,is_bookable")
        }
)
public class Hotel {

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
    @Comment("酒店编码/ID")
    private String hotelCode;

    @Column(name = "hotel_code_md5", length = 64, nullable = false)
    @Comment("酒店编码Md5")
    private String hotelCodeMd5;

    @Column(name = "name", length = 300)
    @Comment("酒店名称")
    private String name;

    @Column(name = "city_code", length = 50)
    @Comment("城市代码")
    private String cityCode;

    @Column(name = "city_name", length = 200)
    @Comment("城市名称")
    private String cityName;

    @Column(name = "country_code", length = 20)
    @Comment("国家代码")
    private String countryCode;

    @Column(name = "main_image", length = 500)
    @Comment("主图URL")
    private String mainImage;

    @Column(name = "short_desc", columnDefinition = "TEXT")
    @Comment("简介")
    private String shortDesc;

    @Column(name = "long_desc", columnDefinition = "LONGTEXT")
    @Comment("详细描述")
    private String longDesc;

    @Column(name = "latitude")
    @Comment("纬度")
    private Double latitude;

    @Column(name = "longitude")
    @Comment("经度")
    private Double longitude;

    @Column(name = "rating")
    @Comment("评分/星级")
    private Double rating;

    @Column(name = "address", length = 500)
    @Comment("地址")
    private String address;

    @Column(name = "phone", length = 100)
    @Comment("电话")
    private String phone;

    @Column(name = "website", length = 300)
    @Comment("官网")
    private String website;

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


    @Column(name = "is_bookable", nullable = false, columnDefinition = "TINYINT(1)")
    @Comment("是否可预定：0-否，1-是")
    private Boolean isBookable = false;
}
