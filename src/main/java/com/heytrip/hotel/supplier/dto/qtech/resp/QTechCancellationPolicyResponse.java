package com.heytrip.hotel.supplier.dto.qtech.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * QTECH取消规则响应DTO
 * 
 * @author Pax
 */
@Data
public class QTechCancellationPolicyResponse  extends QTechBaseResponse{
    

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
     * 退款政策文本
     */
    @JsonProperty("RefundPolicyText")
    private String refundPolicyText;
    
    /**
     * 政策信息
     */
    @JsonProperty("Policies")
    private Policies policies;
    
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
     * 政策信息
     */
    @Data
    public static class Policies {
        /**
         * 取消政策列表
         */
        @JsonProperty("CancellationPolicy")
        private List<CancellationPolicy> cancellationPolicy;
        
        /**
         * 修改政策
         */
        @JsonProperty("AmmendmentPolicy")
        private String ammendmentPolicy;
        
        /**
         * 无显示政策
         */
        @JsonProperty("NoShowPolicy")
        private String noShowPolicy;
    }
    
    /**
     * 取消政策详情
     */
    @Data
    public static class CancellationPolicy {
        /**
         * 开始时间
         */
        @JsonProperty("Start")
        private String start;
        
        /**
         * 结束时间
         */
        @JsonProperty("End")
        private String end;
        
        /**
         * 费用
         */
        @JsonProperty("Charges")
        private BigDecimal charges;
        
        /**
         * 备注
         */
        @JsonProperty("Remark")
        private String remark;
    }
    
    /**
     * 预订允许信息
     */
    @Data
    public static class BookingAllowedInfo {
        /**
         * 价格是否变化
         */
        @JsonProperty("PriceChange")
        private String priceChange;
        
        /**
         * 价格差异
         */
        @JsonProperty("PriceDiff")
        private BigDecimal priceDiff;
        
        /**
         * 价格变化列表
         */
        @JsonProperty("RateChanges")
        private List<Object> rateChanges;
        
        /**
         * 是否立即付款
         */
        @JsonProperty("PayNow")
        private String payNow;
        
        /**
         * 是否允许预订
         * yes/no (如果为no, 则查看MessageInfo和Message字段了解详情)
         */
        @JsonProperty("BookingAllowed")
        private String bookingAllowed;
        
        /**
         * 消息信息
         */
        @JsonProperty("MessageInfo")
        private String messageInfo;
        
        /**
         * 消息
         */
        @JsonProperty("Message")
        private String message;
        
        /**
         * 是否售罄
         */
        @JsonProperty("SoldOut")
        private String soldOut;
    }
}
