package com.heytrip.hotel.supplier.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/**
 * 统一错误响应类
 * 
 * @author  Pax
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ErrorResponse {
    
    private Integer code;
    private String msg;
    private String supplierName;
    private Map<String, String> details;
    private Long timestamp;
    
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
        
        public Builder msg(String msg) {
            errorResponse.msg = msg;
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
        
        public Builder timestamp(Long timestamp) {
            errorResponse.timestamp = timestamp;
            return this;
        }
        
        public ErrorResponse build() {
            return errorResponse;
        }
    }
    

}
