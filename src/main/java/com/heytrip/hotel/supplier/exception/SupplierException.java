package com.heytrip.hotel.supplier.exception;

import com.heytrip.common.enums.ResultCode;
import com.heytrip.common.enums.XiwanSupplierBusinessCode;

/**
 * 业务异常类
 * 用于处理业务逻辑相关的异常
 * 
 * @author  Pax
 */
public class SupplierException extends RuntimeException {
    
    private final int code;
    
    private final int bizCode;

    private String supplierName;

    private final Object  data;


    public SupplierException(String message) {
        super(message);
        this.code = ResultCode.ErrorUnknow.getCode();
        this.bizCode = XiwanSupplierBusinessCode.UNKNOWN_ERROR.getCode();
        this.supplierName = "";
        this.data = null;
    }

    public SupplierException(String supplierName,int code, int bizCode, String message, Object data) {
        super(message);
        this.supplierName = supplierName;
        this.code = code;
        this.bizCode = bizCode;
        this.data = data;
    }


    public SupplierException(int code, int bizCode, String message) {
        super(message);
        this.supplierName = "";
        this.code = code;
        this.bizCode = bizCode;
        this.data = null;
    }

    public SupplierException(String supplierName,int code, int bizCode, String message) {
        super(message);
        this.supplierName = supplierName;
        this.code = code;
        this.bizCode = bizCode;
        this.data = null;
    }
    
    public SupplierException(String supplierName,int code, int bizCode, String message, Throwable cause) {
        super(message, cause);
        this.supplierName = supplierName;
        this.code = code;
        this.bizCode = bizCode;
        this.data = null;
    }
    
    public int getCode() {
        return code;
    }
    public int getBizCode() {
        return bizCode;
    }
    public Object getData() {
        return data;
    }
    public String getSupplierName() {
        return supplierName;
    }


    public static SupplierException of(String supplierName,int code, int bizCode, String message, Object data) {
        return new SupplierException(supplierName,code, bizCode, message,data);
    }
    public static SupplierException of(String supplierName,int code, int bizCode, String message) {
        return new SupplierException(supplierName,code, bizCode, message);
    }
    

    // 参数错误
    public static SupplierException invalidParameter(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.ErrorParameter.getCode(), XiwanSupplierBusinessCode.PARAMETER_ERROR.getCode(), message);
    }
    public static SupplierException invalidParameter(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.ErrorParameter.getCode(), XiwanSupplierBusinessCode.PARAMETER_ERROR.getCode(), message, data);
    }

    // 系统繁忙
    public static SupplierException errorBusy(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.ErrorBusy.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static SupplierException errorBusy(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.ErrorBusy.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }


    // 未知错误
    public static SupplierException errorUnknow(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.ErrorUnknow.getCode(), XiwanSupplierBusinessCode.UNKNOWN_ERROR.getCode(), message);
    }
    public static SupplierException errorUnknow(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.ErrorUnknow.getCode(), XiwanSupplierBusinessCode.UNKNOWN_ERROR.getCode(), message, data);
    }

    // 缺少必要参数
    public static SupplierException missingParameter(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.MissingParameter.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static SupplierException missingParameter(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.MissingParameter.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }

    // 接口凭证错误
    public static SupplierException tokenError(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.TokenError.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static SupplierException tokenError(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.TokenError.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }

    // 接口凭证过期
    public static SupplierException tokenExpire(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.TokenExpire.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static SupplierException tokenExpire(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.TokenExpire.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }


    // 签名过期
    public static SupplierException signExpire(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.SignExpire.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static SupplierException signExpire(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.SignExpire.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }


    // 资源未找到
    public static SupplierException notFound(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.Forbidden.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static SupplierException notFound(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.Forbidden.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }

    // 权限不足
    public static SupplierException unauthorized(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.Forbidden.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static SupplierException unauthorized(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.Forbidden.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }




    // 内部错误
    public static SupplierException internalError(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.BizError.getCode(), XiwanSupplierBusinessCode.UNKNOWN_ERROR.getCode(), message);
    }
    public static SupplierException internalError(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.BizError.getCode(), XiwanSupplierBusinessCode.UNKNOWN_ERROR.getCode(), message, data);
    }

    /// 成功 (查询成功，操作成功，但是无数据)
    public static SupplierException success(String supplierName,String message, Object data) {
        return new SupplierException(supplierName,ResultCode.Ok.getCode(), XiwanSupplierBusinessCode.SUCCESS.getCode(), message, data);
    }
    public static SupplierException success(String supplierName,String message) {
        return new SupplierException(supplierName,ResultCode.Ok.getCode(), XiwanSupplierBusinessCode.SUCCESS.getCode(), message);
    }



}
