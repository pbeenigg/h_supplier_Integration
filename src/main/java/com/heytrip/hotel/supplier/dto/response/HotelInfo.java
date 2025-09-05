package com.heytrip.hotel.supplier.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 酒店信息DTO
 * 
 * @author  Pax
 */
@Data
public class HotelInfo {
    
    private String hotelId;
    
    private String supplierHotelId;
    
    private String hotelName;
    
    private String hotelAddress;
    
    private String city;
    
    private String country;
    
    private BigDecimal starRating;
    
    private BigDecimal latitude;
    
    private BigDecimal longitude;
    
    private String description;
    
    private List<String> amenities;
    
    private List<String> images;
    
    private BigDecimal lowestPrice;
    
    private String currency;
    
    private String supplierName;
    
    private List<RoomInfo> rooms;
    
    private Boolean isActive;
    
    // 默认构造函数
    public HotelInfo() {}
    
    // 带参构造函数
    public HotelInfo(String hotelId, String hotelName, String city) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.city = city;
    }
    

}
