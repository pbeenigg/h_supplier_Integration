package com.heytrip.hotel.supplier.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一错误响应类
 * 
 * @author  Pax
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private Integer code;
    private String message;
    private String supplierName;
    private Map<String, String> details;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    // 默认构造函数
    public ErrorResponse() {}
    
    // Builder模式
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ErrorResponse errorResponse = new ErrorResponse();
        
        public Builder code(Integer code) {
            errorResponse.code = code;
            return this;
        }
        
        public Builder message(String message) {
            errorResponse.message = message;
            return this;
        }
        
        public Builder supplierName(String supplierName) {
            errorResponse.supplierName = supplierName;
            return this;
        }
        
        public Builder details(Map<String, String> details) {
            errorResponse.details = details;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            errorResponse.timestamp = timestamp;
            return this;
        }
        
        public ErrorResponse build() {
            return errorResponse;
        }
    }
    
    // Getters and Setters
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSupplierName() {
        return supplierName;
    }
    
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }
    
    public Map<String, String> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "ErrorResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", supplierName='" + supplierName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
