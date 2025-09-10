package com.heytrip.hotel.supplier.dto.qtech;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * QTECH取消费用响应DTO
 * 
 * @author Pax
 */
@Data
public class QTechCancellationChargesResponse {
    
    /**
     * 状态
     */
    @JsonProperty("Status")
    private String status;
    
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
     * 取消费用
     */
    @JsonProperty("CancellationCharges")
    private BigDecimal cancellationCharges;
    
    /**
     * 退款金额
     */
    @JsonProperty("RefundAmount")
    private BigDecimal refundAmount;
    
    /**
     * 免费取消时间（小时）
     */
    @JsonProperty("CancellationHours")
    private Integer cancellationHours;
    
    /**
     * 合同备注
     */
    @JsonProperty("ContractComment")
    private String contractComment;
    
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
}
