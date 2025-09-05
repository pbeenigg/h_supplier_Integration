package com.heytrip.hotel.supplier.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 酒店搜索响应DTO
 * 
 * @author  Pax
 */
@Data
public class HotelSearchResponse {
    
    private List<HotelInfo> hotels;
    
    private Integer totalCount;
    
    private LocalDateTime searchTime;
    
    private String currency;
    
    private String message;
    

}
