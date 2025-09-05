package com.heytrip.hotel.supplier.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 每日房价DTO
 * 
 * @author  Pax
 */
@Data
public class DailyRate {
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    
    private BigDecimal basePrice;
    
    private BigDecimal price;
    
    private BigDecimal memberPrice;
    
    private String currency;
    
    private Integer mealType;
    
    private Integer breakfastCount;
    
    private Boolean cancelable;
    
    private List<CancelRule> cancelRules;
    
    private String bookingRules;
    
    private Boolean available;
    
    private Integer inventory;
    
    // 默认构造函数
    public DailyRate() {}
    
    // 带参构造函数
    public DailyRate(LocalDate date, BigDecimal basePrice, String currency) {
        this.date = date;
        this.basePrice = basePrice;
        this.currency = currency;
    }
    

}
