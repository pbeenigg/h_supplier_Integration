package com.heytrip.hotel.supplier.dto.qtech.req;

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
     * 代理参考号（必须唯一）
     */
    @JsonProperty("agent_ref_no")
    private String agentRefNo;


    /**
     * 响应压缩
     */
    private String gzip = "no";
}
