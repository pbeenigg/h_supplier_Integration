package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 酒店信息实体类
 * 存储从各个供应商同步的酒店基础信息
 * 
 * @author  Pax
 */
@Entity
@Table(name = "hotel_info", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"supplier_id", "supplier_hotel_id"}))
@Data
public class Hotel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;
    
    @Column(name = "supplier_hotel_id", nullable = false, length = 100)
    private String supplierHotelId;
    
    @Column(name = "hotel_name", nullable = false, length = 200)
    private String hotelName;
    
    @Column(name = "hotel_address", columnDefinition = "TEXT")
    private String hotelAddress;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "country", length = 100)
    private String country;
    
    @Column(name = "star_rating", precision = 2, scale = 1)
    private BigDecimal starRating;
    
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "amenities", columnDefinition = "JSON")
    private String amenities;
    
    @Column(name = "images", columnDefinition = "JSON")
    private String images;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 关联供应商配置
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private SupplierConfig supplierConfig;
    
    // 默认构造函数
    public Hotel() {}
    
    // 带参构造函数
    public Hotel(Long supplierId, String supplierHotelId, String hotelName) {
        this.supplierId = supplierId;
        this.supplierHotelId = supplierHotelId;
        this.hotelName = hotelName;
    }
}
