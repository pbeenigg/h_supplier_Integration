package com.heytrip.hotel.supplier.dto.qtech;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * QTECH取消政策响应DTO
 * 
 * @author Pax
 */
@Data
public class QTechCancellationPolicyResponse {
    
    /**
     * 消息
     */
    @JsonProperty("Message")
    private String message;
    
    /**
     * 取消费用币种
     */
    @JsonProperty("CancellationCurrency")
    private String cancellationCurrency;
    
    /**
     * 预订总额
     */
    @JsonProperty("TotalBookingAmount")
    private BigDecimal totalBookingAmount;
    
    /**
     * 合同备注
     */
    @JsonProperty("ContractComment")
    private String contractComment;
    
    /**
     * 免费取消时间（小时）
     */
    @JsonProperty("CancellationHours")
    private Integer cancellationHours;
    
    /**
     * 逾期取消罚金
     */
    @JsonProperty("AppliedAgentCharges")
    private BigDecimal appliedAgentCharges;
    
    /**
     * 预订允许信息
     */
    @JsonProperty("BookingAllowedInfo")
    private BookingAllowedInfo bookingAllowedInfo;
    
    /**
     * 开始时间
     */
    @JsonProperty("StartTime")
    private String startTime;
    
    /**
     * 结束时间
     */
    @JsonProperty("EndTime")
    private String endTime;
    
    /**
     * 预订允许信息
     */
    @Data
    public static class BookingAllowedInfo {
        /**
         * 消息
         */
        @JsonProperty("Message")
        private String message;
        
        /**
         * 状态
         */
        @JsonProperty("Status")
        private String status;
        
        /**
         * 是否售罄
         */
        @JsonProperty("SoldOut")
        private String soldOut;
        
        /**
         * 消息信息
         */
        @JsonProperty("MessageInfo")
        private String messageInfo;
        
        /**
         * 是否允许预订
         */
        @JsonProperty("BookingAllowed")
        private String bookingAllowed;
    }
}
