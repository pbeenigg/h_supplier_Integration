package com.heytrip.hotel.supplier.dto.qtech.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * QTECH取消费用响应DTO
 * 根据API文档 get_cancellation_charges 接口响应格式定义
 * 
 * @author Pax
 */
@Data
public class QTechCancellationChargesResponse extends QTechBaseResponse {


    /**
     * 状态
     */
    @JsonProperty("Status")
    private String status;

    /**
     * 是否允许取消（yes/no）
     */
    @JsonProperty("AllowCancel")
    private String allowCancel;
    
    /**
     * 说明信息
     */
    @JsonProperty("MessageInfo")
    private String messageInfo;
    
    /**
     * 取消费用
     */
    @JsonProperty("CancellationCharge")
    private BigDecimal cancellationCharge;
    
    /**
     * 显示币种代码
     */
    @JsonProperty("DisplayCurrencyCode")
    private String displayCurrencyCode;
    

}
