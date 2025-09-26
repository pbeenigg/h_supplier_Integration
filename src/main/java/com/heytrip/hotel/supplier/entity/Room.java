package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 静态数据-房型
 * 约束： (supplier_id, supplier_code, room_code) 唯一
 * 参考标准实体：com.heytrip.common.response.basic.XRoom
 */
@Data
@Entity
@Table(name = "room",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_supplier_code", columnNames = {"supplier_id", "supplier_code","hotel_code","room_code"})
        },
        indexes = {
                @Index(name = "idx_room_supplier", columnList = "supplier_id,supplier_code"),
                @Index(name = "idx_room_code", columnList = "room_code"),
                @Index(name = "idx_room_hotel", columnList = "hotel_id"),
                @Index(name = "idx_room_code_md5", columnList = "room_code_md5"),
        }
)
public class Room {

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


    @Column(name = "hotel_id", length = 64, nullable = false)
    @Comment("酒店ID")
    private Long hotelId;

    @Column(name = "hotel_code", length = 200, nullable = false)
    @Comment("酒店编码")
    private String hotelCode;

    @Column(name = "room_code", length = 200)
    @Comment("物理房型编码")
    private String roomCode;

    @Column(name = "room_code_md5", length = 64, nullable = false)
    @Comment("房型编码MD5")
    private String roomCodeMd5;

    @Column(name = "room_name", length = 300)
    @Comment("物理房型名称")
    private String roomName;

    @Column(name = "room_name_en", length = 300)
    @Comment("物理房型名称(英文)")
    private String roomNameEn;

    @Column(name = "description", columnDefinition = "TEXT")
    @Comment("描述")
    private String description;

    @Column(name = "room_quantity")
    @Comment("房间数量")
    private Integer roomQuantity;


    @Column(name = "ext", columnDefinition = "TEXT")
    @Comment("扩展字段")
    private String ext;

    @Column(name = "max_occupancy")
    @Comment("最大入住人数")
    private Integer maxOccupancy;

    @Column(name = "max_occupancy_info", columnDefinition = "TEXT")
    @Comment("最大入住人数具体描述(JSON)")
    private String maxOccupancyInfo;

    @Column(name = "bed_type", length = 100)
    @Comment("床型")
    private String bedType;

    @Column(name = "bed_type_desc", length = 300)
    @Comment("床型描述")
    private String bedTypeDesc;

    @Column(name = "bed_type_desc_en", length = 300)
    @Comment("床型描述(英文)")
    private String bedTypeDescEn;

    @Column(name = "bed_rooms", columnDefinition = "TEXT")
    @Comment("卧室床型明细(JSON)")
    private String bedRooms;


    @Column(name = "floor", length = 100)
    @Comment("楼层")
    private String floor;

    @Column(name = "area", length = 100)
    @Comment("面积")
    private String area;

    @Column(name = "views", length = 300)
    @Comment("房型景观-国际")
    private String views;

    @Column(name = "bed_width", length = 100)
    @Comment("床宽")
    private String bedWidth;

    @Column(name = "no_smoking", length = 50)
    @Comment("禁烟信息")
    private String noSmoking;

    @Column(name = "images", columnDefinition = "TEXT")
    @Comment("图片(JSON)")
    private String images;

    @Column(name = "min_price", precision = 10, scale = 2)
    @Comment("最低价格")
    private BigDecimal minPrice;

    @Column(name = "min_base_price", precision = 10, scale = 2)
    @Comment("最低基础价格")
    private BigDecimal minBasePrice;

    @Column(name = "facilities", columnDefinition = "TEXT")
    @Comment("房型设施(JSON)")
    private String facilities;

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
