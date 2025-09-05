package com.heytrip.hotel.supplier.dto.request;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建订单请求DTO
 * 
 * @author  Pax
 */
@Data
public class CreateOrderRequest {
    
    @NotBlank(message = "酒店ID不能为空")
    private String hotelId;
    
    @NotBlank(message = "房间ID不能为空")
    private String roomId;
    
    @NotBlank(message = "价格计划ID不能为空")
    private String ratePlanId;
    
    @NotNull(message = "入住日期不能为空")
    @Future(message = "入住日期必须是未来日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;
    
    @NotNull(message = "离店日期不能为空")
    @Future(message = "离店日期必须是未来日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;
    
    @NotNull(message = "房间数不能为空")
    @Min(value = 1, message = "房间数必须大于0")
    private Integer roomCount;
    
    @NotNull(message = "客人数不能为空")
    @Min(value = 1, message = "客人数必须大于0")
    private Integer guestCount;
    
    @NotBlank(message = "主客人姓名不能为空")
    @Size(max = 100, message = "客人姓名长度不能超过100个字符")
    private String guestName;
    
    @Email(message = "邮箱格式不正确")
    private String guestEmail;
    
    @Size(max = 50, message = "电话号码长度不能超过50个字符")
    private String guestPhone;
    
    @NotNull(message = "销售价格不能为空")
    @DecimalMin(value = "0.01", message = "销售价格必须大于0")
    private BigDecimal salePrice;
    
    @NotBlank(message = "币种不能为空")
    private String currency;
    
    private String distributorOrderId;
    
    private String channel;
    
    private String channelCompany;
    
    private String specialRequests;
    
    private String nationality = "CN";
    
    private String contactPerson;
    
    private String contactPhone;
    
    private String contactEmail;
    
    // 默认构造函数
    public CreateOrderRequest() {}
    
    // 带参构造函数
    public CreateOrderRequest(String hotelId, String roomId, String ratePlanId, 
                             LocalDate checkInDate, LocalDate checkOutDate,
                             Integer roomCount, Integer guestCount, String guestName, 
                             BigDecimal salePrice, String currency) {
        this.hotelId = hotelId;
        this.roomId = roomId;
        this.ratePlanId = ratePlanId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomCount = roomCount;
        this.guestCount = guestCount;
        this.guestName = guestName;
        this.salePrice = salePrice;
        this.currency = currency;
    }

}
