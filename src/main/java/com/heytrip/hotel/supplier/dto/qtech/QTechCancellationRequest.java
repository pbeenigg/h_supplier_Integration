package com.heytrip.hotel.supplier.dto.qtech;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QTECH取消预订请求DTO
 * 
 * @author Pax
 */
@Data
public class QTechCancellationRequest {
    
    /**
     * 接口名称
     */
    private String action = "cancel_the_booking";
    
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
     * 取消原因
     */
    @JsonProperty("cancel_reason")
    private String cancelReason;
    
    /**
     * 响应压缩
     */
    private String gzip = "no";
}
