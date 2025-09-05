package com.heytrip.hotel.supplier.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单响应DTO
 * 
 * @author  Pax
 */
@Data
public class CreateOrderResponse {
    
    private Integer code;
    
    private Integer bizCode;
    
    private String message;
    
    private String bookingReference;
    
    private String supplierBookingId;
    
    private String distributorOrderId;
    
    private Integer status; // 订单状态: 1-待确认, 2-已确认, 7-已拒单, 8-申请取消中, 9-已取消
    
    private String statusDesc;
    
    private BigDecimal totalAmount;
    
    private String currency;
    
    private String hotelName;
    
    private String roomType;
    
    private String guestName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private String cancellationPolicy;
    
    private Boolean hasError;
    
    private String errorMessage;
    
    // 默认构造函数
    public CreateOrderResponse() {}
    
    // 成功响应构造函数
    public CreateOrderResponse(String bookingReference, String supplierBookingId, Integer status) {
        this.code = 0;
        this.bookingReference = bookingReference;
        this.supplierBookingId = supplierBookingId;
        this.status = status;
        this.hasError = false;
        this.createdAt = LocalDateTime.now();
    }
    
    // 错误响应构造函数
    public CreateOrderResponse(Integer code, Integer bizCode, String message) {
        this.code = code;
        this.bizCode = bizCode;
        this.message = message;
        this.hasError = true;
        this.createdAt = LocalDateTime.now();
    }
    

}
