package com.heytrip.hotel.supplier.dto.qtech.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * QTECH取消预订响应DTO
 * 
 * @author Pax
 */
@Data
public class QTechCancellationResponse  extends QTechBaseResponse{
    

    
    /**
     * 取消详情
     */
    @JsonProperty("CancellationDetail")
    private CancellationDetail cancellationDetail;

    
    /**
     * 取消详情
     */
    @Data
    public static class CancellationDetail {
        /**
         * 预订ID
         */
        @JsonProperty("BookingId")
        private String bookingId;
        
        /**
         * 预订参考号
         */
        @JsonProperty("BookingReference")
        private String bookingReference;
        
        /**
         * 取消日期
         */
        @JsonProperty("CancellationDate")
        private String cancellationDate;
        
        /**
         * 取消状态
         */
        @JsonProperty("CancellationStatus")
        private String cancellationStatus;
        
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
         * 币种
         */
        @JsonProperty("CurrencyCode")
        private String currencyCode;
        
        /**
         * 取消原因
         */
        @JsonProperty("CancellationReason")
        private String cancellationReason;
        
        /**
         * 备注
         */
        @JsonProperty("Remarks")
        private String remarks;
    }
}
