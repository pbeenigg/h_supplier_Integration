package com.heytrip.hotel.supplier.exception;

/**
 * 业务异常类
 * 用于处理业务逻辑相关的异常
 * 
 * @author  Pax
 */
public class BusinessException extends RuntimeException {
    
    private final int code;
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    // 常用的业务异常静态方法
    public static BusinessException invalidParameter(String message) {
        return new BusinessException(400, message);
    }
    
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }
    
    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }
    
    public static BusinessException internalError(String message) {
        return new BusinessException(500, message);
    }
}
