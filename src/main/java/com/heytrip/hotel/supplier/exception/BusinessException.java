package com.heytrip.hotel.supplier.exception;

import com.heytrip.common.enums.ResultCode;
import com.heytrip.common.enums.XiwanSupplierBusinessCode;

/**
 * 业务异常类
 * 用于处理业务逻辑相关的异常
 * 
 * @author  Pax
 */
public class BusinessException extends RuntimeException {
    
    private final int code;
    
    private final int bizCode;

    private final Object  data;


    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ErrorUnknow.getCode();
        this.bizCode = XiwanSupplierBusinessCode.UNKNOWN_ERROR.getCode();
        this.data = null;
    }

    public BusinessException(int code, int bizCode, String message, Object data) {
        super(message);
        this.code = code;
        this.bizCode = bizCode;
        this.data = data;
    }

    public BusinessException(int code, int bizCode, String message) {
        super(message);
        this.code = code;
        this.bizCode = bizCode;
        this.data = null;
    }
    
    public BusinessException(int code, int bizCode, String message, Throwable cause) {
        super(message, cause);
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


    public static BusinessException of(int code, int bizCode, String message,Object data) {
        return new BusinessException(code, bizCode, message,data);
    }
    public static BusinessException of(int code, int bizCode, String message) {
        return new BusinessException(code, bizCode, message);
    }
    

    // 参数错误
    public static BusinessException invalidParameter(String message) {
        return new BusinessException(ResultCode.ErrorParameter.getCode(), XiwanSupplierBusinessCode.PARAMETER_ERROR.getCode(), message);
    }
    public static BusinessException invalidParameter(String message, Object data) {
        return new BusinessException(ResultCode.ErrorParameter.getCode(), XiwanSupplierBusinessCode.PARAMETER_ERROR.getCode(), message, data);
    }

    // 系统繁忙
    public static BusinessException errorBusy(String message) {
        return new BusinessException(ResultCode.ErrorBusy.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static BusinessException errorBusy(String message, Object data) {
        return new BusinessException(ResultCode.ErrorBusy.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }


    // 未知错误
    public static BusinessException errorUnknow(String message) {
        return new BusinessException(ResultCode.ErrorUnknow.getCode(), XiwanSupplierBusinessCode.UNKNOWN_ERROR.getCode(), message);
    }
    public static BusinessException errorUnknow(String message, Object data) {
        return new BusinessException(ResultCode.ErrorUnknow.getCode(), XiwanSupplierBusinessCode.UNKNOWN_ERROR.getCode(), message, data);
    }

    // 缺少必要参数
    public static BusinessException missingParameter(String message) {
        return new BusinessException(ResultCode.MissingParameter.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static BusinessException missingParameter(String message, Object data) {
        return new BusinessException(ResultCode.MissingParameter.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }

    // 接口凭证错误
    public static BusinessException tokenError(String message) {
        return new BusinessException(ResultCode.TokenError.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static BusinessException tokenError(String message, Object data) {
        return new BusinessException(ResultCode.TokenError.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }

    // 接口凭证过期
    public static BusinessException tokenExpire(String message) {
        return new BusinessException(ResultCode.TokenExpire.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static BusinessException tokenExpire(String message, Object data) {
        return new BusinessException(ResultCode.TokenExpire.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }

    // 签名错误
    public static BusinessException signError(String message) {
        return new BusinessException(ResultCode.SignError.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static BusinessException signError(String message, Object data) {
        return new BusinessException(ResultCode.SignError.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }


    // 签名过期
    public static BusinessException signExpire(String message) {
        return new BusinessException(ResultCode.SignExpire.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static BusinessException signExpire(String message, Object data) {
        return new BusinessException(ResultCode.SignExpire.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }


    // 资源未找到
    public static BusinessException notFound(String message) {
        return new BusinessException(ResultCode.Forbidden.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static BusinessException notFound(String message, Object data) {
        return new BusinessException(ResultCode.Forbidden.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }

    // 权限不足
    public static BusinessException unauthorized(String message) {
        return new BusinessException(ResultCode.Forbidden.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message);
    }
    public static BusinessException unauthorized(String message, Object data) {
        return new BusinessException(ResultCode.Forbidden.getCode(), XiwanSupplierBusinessCode.FAIL.getCode(), message, data);
    }




    // 内部错误
    public static BusinessException internalError(String message) {
        return new BusinessException(ResultCode.BizError.getCode(), XiwanSupplierBusinessCode.UNKNOWN_ERROR.getCode(), message);
    }
    public static BusinessException internalError(String message, Object data) {
        return new BusinessException(ResultCode.BizError.getCode(), XiwanSupplierBusinessCode.UNKNOWN_ERROR.getCode(), message, data);
    }

    /// 成功 (查询成功，操作成功，但是无数据)
    public static BusinessException success(String message, Object data) {
        return new BusinessException(ResultCode.Ok.getCode(), XiwanSupplierBusinessCode.SUCCESS.getCode(), message, data);
    }
    public static BusinessException success(String message) {
        return new BusinessException(ResultCode.Ok.getCode(), XiwanSupplierBusinessCode.SUCCESS.getCode(), message);
    }



}
