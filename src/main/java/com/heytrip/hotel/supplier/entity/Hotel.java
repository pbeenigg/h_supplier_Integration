package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 静态数据-酒店
 * 约束： (supplier_id, supplier_code, hotel_id) 唯一
 * 参考标准实体：com.heytrip.common.response.basic.XHotel
 */
@Data
@Entity
@Table(name = "hotel",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hotel_supplier_code", columnNames = {"supplier_id", "supplier_code", "hotel_code","city_code"})
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
    @Comment("酒店编号")
    private String hotelCode;

    @Column(name = "hotel_code_md5", length = 64, nullable = false)
    @Comment("酒店编码Md5")
    private String hotelCodeMd5;

    @Column(name = "name", length = 300)
    @Comment("酒店名称")
    private String hotelName;

    @Column(name = "locale_name", length = 300)
    @Comment("酒店本地化名称(国际)")
    private String localeName;

    @Column(name = "country_code", length = 20)
    @Comment("国家代码")
    private String countryCode;

    @Column(name = "country_id")
    @Comment("国家编号")
    private Integer countryId;

    @Column(name = "country", length = 100)
    @Comment("国家")
    private String country;

    @Column(name = "city_code", length = 50)
    @Comment("城市代码")
    private String cityCode;

    @Column(name = "city_id")
    @Comment("城市编号")
    private Integer cityId;

    @Column(name = "city", length = 200)
    @Comment("城市")
    private String city;

    @Column(name = "area_id")
    @Comment("所属区域/附近区域编号")
    private Integer areaId;

    @Column(name = "area", length = 200)
    @Comment("所属区域/附近区域")
    private String area;

    @Column(name = "address", length = 500)
    @Comment("地址")
    private String address;

    @Column(name = "address_locale", length = 500)
    @Comment("地址本地化名称(国际)")
    private String addressLocale;

    @Column(name = "phone", length = 100)
    @Comment("电话")
    private String phone;

    @Column(name = "latitude")
    @Comment("纬度")
    private String latitude;

    @Column(name = "longitude")
    @Comment("经度")
    private String longitude;

    @Column(name = "rating")
    @Comment("星级")
    private String  rating;

    @Column(name = "hotel_type", length = 100)
    @Comment("酒店类型")
    private String hotelType;

    @Column(name = "brand", length = 200)
    @Comment("品牌")
    private String brand;

    @Column(name = "description", columnDefinition = "TEXT")
    @Comment("简介/描述")
    private String description;

    @Column(name = "long_desc", columnDefinition = "LONGTEXT")
    @Comment("详细描述")
    private String longDesc;

    @Column(name = "min_price", precision = 10, scale = 2)
    @Comment("最低价格")
    private BigDecimal minPrice;

    @Column(name = "status")
    @Comment("状态：1在线，0下线，-1黑名单")
    private Integer status = 1;

    @Column(name = "number_of_rooms", length = 50)
    @Comment("客房总数")
    private String numberOfRooms;

    @Column(name = "year_property_opened", length = 10)
    @Comment("建成年份")
    private String yearPropertyOpened;

    @Column(name = "most_recent_renovation", length = 10)
    @Comment("最近装修年份")
    private String mostRecentRenovation;

    @Column(name = "check_in_from", length = 20)
    @Comment("入住办理起始时间")
    private String checkInFrom;

    @Column(name = "check_out_util", length = 20)
    @Comment("退房办理截止时间")
    private String checkOutUtil;

    @Column(name = "postal_code", length = 20)
    @Comment("邮编")
    private String postalCode;

    @Column(name = "hero_img", length = 500)
    @Comment("首图（中等尺寸）")
    private String heroImg;

    @Column(name = "email", length = 200)
    @Comment("邮箱")
    private String email;

    @Column(name = "website", length = 300)
    @Comment("官网")
    private String website;

    @Column(name = "ext", columnDefinition = "TEXT")
    @Comment("扩展信息")
    private String ext;

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
