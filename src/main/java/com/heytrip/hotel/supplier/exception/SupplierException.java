package com.heytrip.hotel.supplier.exception;

/**
 * 供应商异常类
 * 用于处理供应商相关的异常
 * 
 * @author  Pax
 */
public class SupplierException extends RuntimeException {
    
    private final int code;
    private final String supplierName;
    
    public SupplierException(int code, String supplierName, String message) {
        super(message);
        this.code = code;
        this.supplierName = supplierName;
    }
    
    public SupplierException(int code, String supplierName, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.supplierName = supplierName;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getSupplierName() {
        return supplierName;
    }
    
    // 常用的供应商异常静态方法
    public static SupplierException connectionFailed(String supplierName, String message) {
        return new SupplierException(502, supplierName, "Connection failed: " + message);
    }
    
    public static SupplierException timeout(String supplierName, String message) {
        return new SupplierException(504, supplierName, "Request timeout: " + message);
    }
    
    public static SupplierException authenticationFailed(String supplierName, String message) {
        return new SupplierException(401, supplierName, "Authentication failed: " + message);
    }
    
    public static SupplierException serviceUnavailable(String supplierName, String message) {
        return new SupplierException(503, supplierName, "Service unavailable: " + message);
    }
    
    public static SupplierException invalidResponse(String supplierName, String message) {
        return new SupplierException(502, supplierName, "Invalid response: " + message);
    }
}
