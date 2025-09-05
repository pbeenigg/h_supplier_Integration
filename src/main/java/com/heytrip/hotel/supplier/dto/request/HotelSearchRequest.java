package com.heytrip.hotel.supplier.dto.request;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 酒店搜索请求DTO
 * 
 * @author  Pax
 */
@Data
public class HotelSearchRequest {
    
    @NotBlank(message = "城市不能为空")
    @Size(max = 100, message = "城市名称长度不能超过100个字符")
    private String city;
    
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
    @Max(value = 10, message = "房间数不能超过10")
    private Integer roomCount;
    
    @NotNull(message = "客人数不能为空")
    @Min(value = 1, message = "客人数必须大于0")
    @Max(value = 20, message = "客人数不能超过20")
    private Integer guestCount;
    
    private String country;
    
    private String currency = "CNY";
    
    private String nationality = "CN";
    
    private String channel;
    
    private String channelCompany;
    
    private Integer mode = 1; // 1: 价格日历模式, 2: 非价格日历模式
    
    private String query; // JSON格式的查询参数
    
    // 默认构造函数
    public HotelSearchRequest() {}
    
    // 带参构造函数
    public HotelSearchRequest(String city, LocalDate checkInDate, LocalDate checkOutDate, 
                             Integer roomCount, Integer guestCount) {
        this.city = city;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomCount = roomCount;
        this.guestCount = guestCount;
    }
}
