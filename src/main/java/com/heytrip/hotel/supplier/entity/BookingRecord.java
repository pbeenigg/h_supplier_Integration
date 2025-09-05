package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预订记录实体类
 * 存储预订相关的信息，用于跟踪和管理预订状态
 * 
 * @author  Pax
 */
@Entity
@Table(name = "booking_record")
@Data
public class BookingRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "booking_reference", nullable = false, unique = true, length = 100)
    private String bookingReference;
    
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;
    
    @Column(name = "supplier_booking_id", length = 100)
    private String supplierBookingId;
    
    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;
    
    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;
    
    @Column(name = "guest_email", length = 200)
    private String guestEmail;
    
    @Column(name = "guest_phone", length = 50)
    private String guestPhone;
    
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;
    
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;
    
    @Column(name = "room_type", length = 100)
    private String roomType;
    
    @Column(name = "room_count", nullable = false)
    private Integer roomCount;
    
    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "currency", length = 10)
    private String currency;
    
    @Column(name = "booking_status", nullable = false, length = 50)
    private Integer bookingStatus;
    
    @Column(name = "channel", length = 50)
    private String channel;
    
    @Column(name = "channel_company", length = 100)
    private String channelCompany;
    
    @Column(name = "distributor_order_id", length = 100)
    private String distributorOrderId;
    
    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;
    
    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;
    
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
    
    // 关联酒店信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", insertable = false, updatable = false)
    private Hotel hotel;
    
    // 默认构造函数
    public BookingRecord() {}
    
    // 带参构造函数
    public BookingRecord(String bookingReference, Long supplierId, Long hotelId, String guestName) {
        this.bookingReference = bookingReference;
        this.supplierId = supplierId;
        this.hotelId = hotelId;
        this.guestName = guestName;
    }
}
