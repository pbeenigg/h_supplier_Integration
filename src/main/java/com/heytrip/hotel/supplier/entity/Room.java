package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 房间信息实体类
 * 存储酒店房间类型信息
 * 
 * @author  Pax
 */
@Entity
@Table(name = "room_info", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"hotel_id", "supplier_room_id"}))
@Data
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;
    
    @Column(name = "supplier_room_id", nullable = false, length = 100)
    private String supplierRoomId;
    
    @Column(name = "room_name", nullable = false, length = 200)
    private String roomName;
    
    @Column(name = "room_type", length = 100)
    private String roomType;
    
    @Column(name = "bed_type", length = 50)
    private String bedType;
    
    @Column(name = "max_occupancy")
    private Integer maxOccupancy;
    
    @Column(name = "room_size")
    private Integer roomSize;
    
    @Column(name = "has_window")
    private Boolean hasWindow;
    
    @Column(name = "has_private_bathroom")
    private Boolean hasPrivateBathroom = true;
    
    @Column(name = "smoking_allowed")
    private Boolean smokingAllowed = false;
    
    @Column(name = "room_amenities", columnDefinition = "JSON")
    private String roomAmenities;
    
    @Column(name = "room_images", columnDefinition = "JSON")
    private String roomImages;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 关联酒店信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", insertable = false, updatable = false)
    private Hotel hotel;
    
    // 默认构造函数
    public Room() {}
    
    // 带参构造函数
    public Room(Long hotelId, String supplierRoomId, String roomName) {
        this.hotelId = hotelId;
        this.supplierRoomId = supplierRoomId;
        this.roomName = roomName;
    }
}
