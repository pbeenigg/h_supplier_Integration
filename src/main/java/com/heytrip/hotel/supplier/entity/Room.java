package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
                @UniqueConstraint(name = "uk_room_supplier_code", columnNames = {"supplier_id", "supplier_code", "room_code"})
        },
        indexes = {
                @Index(name = "idx_room_supplier", columnList = "supplier_id,supplier_code"),
                @Index(name = "idx_room_code", columnList = "room_code"),
                @Index(name = "idx_room_hotel", columnList = "hotel_code")
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

    @Column(name = "room_code", length = 64, nullable = false)
    @Comment("房型编码")
    private String roomCode;

    @Column(name = "hotel_code", length = 64)
    @Comment("所属酒店编码")
    private String hotelCode;

    @Column(name = "name", length = 300)
    @Comment("房型名称")
    private String name;

    @Column(name = "bed_type", length = 100)
    @Comment("床型")
    private String bedType;

    @Column(name = "area", length = 100)
    @Comment("面积")
    private String area;

    @Column(name = "occupancy")
    @Comment("最大入住人数")
    private Integer occupancy;

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
