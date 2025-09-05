package com.heytrip.hotel.supplier.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房间信息DTO
 * 
 * @author  Pax
 */
@Data
public class RoomInfo {
    
    private String roomId;
    
    private String supplierRoomId;
    
    private String roomName;
    
    private String roomType;
    
    private String bedType;
    
    private Integer maxOccupancy;
    
    private Integer roomSize;
    
    private Boolean hasWindow;
    
    private Boolean hasPrivateBathroom;
    
    private Boolean smokingAllowed;
    
    private List<String> roomAmenities;
    
    private List<String> roomImages;
    
    private String description;
    
    private List<RatePlan> ratePlans;
    
    private Boolean isActive;
    
    // 默认构造函数
    public RoomInfo() {}
    
    // 带参构造函数
    public RoomInfo(String roomId, String roomName, String roomType) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomType = roomType;
    }
    

}
