package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 静态数据-GIATA酒店映射
 * 约束： (city_code, supplier_code, hotel_code, giata_id) 唯一
 * 文档字段：id, hotel_code, giata_id, name, city_code, city_name, country_code, long_desc, latitude, longitude, rating, address, main_image
 */
@Data
@Entity
@Table(name = "hotel_giata",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_giata_supplier_hotel", columnNames = {"city_code", "supplier_code", "hotel_code", "giata_id"})
        },
        indexes = {
                @Index(name = "idx_giata_supplier", columnList = "supplier_id,supplier_code"),
                @Index(name = "idx_giata_hotel", columnList = "hotel_code"),
                @Index(name = "idx_giata_id", columnList = "giata_id")
        }
)
public class HotelGiata {

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

    @Column(name = "hotel_code", length = 64, nullable = false)
    @Comment("酒店编码/ID")
    private String hotelCode;

    @Column(name = "giata_id", length = 64, nullable = false)
    @Comment("GIATA 编码")
    private String giataId;

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

    @Column(name = "long_desc", columnDefinition = "LONGTEXT")
    @Comment("描述")
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

    @Column(name = "main_image", length = 500)
    @Comment("主图URL")
    private String mainImage;

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
