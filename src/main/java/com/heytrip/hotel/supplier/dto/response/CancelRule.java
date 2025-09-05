package com.heytrip.hotel.supplier.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 取消规则DTO
 * 
 * @author  Pax
 */
@Data
public class CancelRule {
    
    private String desc;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCancelTime;
    
    private Integer hours;
    
    private Boolean isAfter;
    
    private Integer deductType; // 1: 固定金额, 2: 固定房晚, 3: 百分比, 4: 免费取消
    
    private BigDecimal deductValue;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    private String startTimeOrig;
    
    private String endTimeOrig;
    
    private String descOrig;
    
    // 默认构造函数
    public CancelRule() {}
    
    // 带参构造函数
    public CancelRule(String desc, Integer deductType) {
        this.desc = desc;
        this.deductType = deductType;
    }
    
    // Getters and Setters
    public String getDesc() {
        return desc;
    }
    
    public void setDesc(String desc) {
        this.desc = desc;
    }
    
    public LocalDateTime getLastCancelTime() {
        return lastCancelTime;
    }
    
    public void setLastCancelTime(LocalDateTime lastCancelTime) {
        this.lastCancelTime = lastCancelTime;
    }
    
    public Integer getHours() {
        return hours;
    }
    
    public void setHours(Integer hours) {
        this.hours = hours;
    }
    
    public Boolean getIsAfter() {
        return isAfter;
    }
    
    public void setIsAfter(Boolean isAfter) {
        this.isAfter = isAfter;
    }
    
    public Integer getDeductType() {
        return deductType;
    }
    
    public void setDeductType(Integer deductType) {
        this.deductType = deductType;
    }
    
    public BigDecimal getDeductValue() {
        return deductValue;
    }
    
    public void setDeductValue(BigDecimal deductValue) {
        this.deductValue = deductValue;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public String getStartTimeOrig() {
        return startTimeOrig;
    }
    
    public void setStartTimeOrig(String startTimeOrig) {
        this.startTimeOrig = startTimeOrig;
    }
    
    public String getEndTimeOrig() {
        return endTimeOrig;
    }
    
    public void setEndTimeOrig(String endTimeOrig) {
        this.endTimeOrig = endTimeOrig;
    }
    
    public String getDescOrig() {
        return descOrig;
    }
    
    public void setDescOrig(String descOrig) {
        this.descOrig = descOrig;
    }
    
    @Override
    public String toString() {
        return "CancelRule{" +
                "desc='" + desc + '\'' +
                ", deductType=" + deductType +
                ", deductValue=" + deductValue +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
