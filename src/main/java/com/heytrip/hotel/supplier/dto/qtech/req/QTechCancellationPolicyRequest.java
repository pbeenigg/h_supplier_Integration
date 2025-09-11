package com.heytrip.hotel.supplier.dto.qtech.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QTECH取消规则请求DTO
 * 对应API接口：hotel_cancellation_policy
 * 
 * @author Pax
 */
@Data
public class QTechCancellationPolicyRequest {
    
    /**
     * 接口名称
     */
    private String action = "hotel_cancellation_policy";
    
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
     * 唯一标识（来自search/detail响应）
     */
    @JsonProperty("unique_id")
    private String uniqueId;
    
    /**
     * 房型唯一ID（来自search/detail响应）
     */
    @JsonProperty("section_unique_id")
    private String sectionUniqueId;
    
    /**
     * 响应压缩
     */
    private String gzip = "no";
}
