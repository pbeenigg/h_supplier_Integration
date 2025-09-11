package com.heytrip.hotel.supplier.dto.qtech.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QTECH酒店详情请求DTO
 * 
 * @author Pax
 */
@Data
public class QTechHotelDetailRequest {
    
    /**
     * 接口名称
     */
    private String action = "hotel_detail";
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 酒店ID
     */
    @JsonProperty("hotel_id")
    private String hotelId;
    
    /**
     * 响应压缩
     */
    private String gzip = "no";
}
