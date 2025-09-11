package com.heytrip.hotel.supplier.dto.qtech.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QTECH获得取消费用请求DTO
 * 
 * @author Pax
 */
@Data
public class QTechGetCancellationChargesRequest {
    
    /**
     * 接口名称
     */
    private String action = "get_cancellation_charges";
    
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
     * 预定号
     */
    @JsonProperty("booking_reference")
    private String  bookingReference;


    /**
     * 响应压缩
     */
    private String gzip = "no";
}
