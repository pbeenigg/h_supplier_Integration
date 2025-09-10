package com.heytrip.hotel.supplier.dto.qtech;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QTECH预订详情请求DTO
 * 
 * @author Pax
 */
@Data
public class QTechBookingDetailRequest {
    
    /**
     * 接口名称
     */
    private String action = "booking_detail";
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 预订ID
     */
    @JsonProperty("booking_id")
    private String bookingId;
    
    /**
     * 响应压缩
     */
    private String gzip = "no";
}
