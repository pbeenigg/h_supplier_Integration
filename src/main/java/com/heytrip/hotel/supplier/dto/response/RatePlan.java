package com.heytrip.hotel.supplier.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 价格计划DTO
 * 
 * @author  Pax
 */
@Data
public class RatePlan {
    
    private String ratePlanId;
    
    private String ratePlanName;
    
    private BigDecimal basePrice;
    
    private BigDecimal price;
    
    private BigDecimal memberPrice;
    
    private String currency;
    
    private Integer mealType; // 1: 早餐, 2: 午餐, 3: 晚餐, 4: 全餐
    
    private Integer breakfastCount;
    
    private Boolean cancelable;
    
    private List<CancelRule> cancelRules;
    
    private Boolean payAtHotel;
    
    private Boolean instantConfirmation;
    
    private String bookingRules;
    
    private List<DailyRate> dailyRates;
    
    private String supplierName;
    
    private String accountType;
    
    // 默认构造函数
    public RatePlan() {}
    
    // 带参构造函数
    public RatePlan(String ratePlanId, String ratePlanName, BigDecimal basePrice, String currency) {
        this.ratePlanId = ratePlanId;
        this.ratePlanName = ratePlanName;
        this.basePrice = basePrice;
        this.currency = currency;
    }
    

}
