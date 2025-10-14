package com.heytrip.hotel.supplier.dto;

import lombok.Data;

import java.util.Map;

/**
 * API日志数据传输对象
 * 用于在AOP切面中传递日志相关数据
 *
 * @author Pax
 * @since 1.0.0
 */
@Data
public class ApiLogData {

    
    /**
     * 链路追踪ID，用于跟踪整个请求链路
     */
    private String traceId;

    /**
     * 应用ID标识，标识调用方应用
     */
    private String appId;

    /**
     * 供应商ID，关联supplier_config表
     */
    private Long supplierId;

    /**
     * API端点路径，如：/api/hotel/search
     */
    private String apiEndpoint;

    /**
     * HTTP请求方法：GET、POST、PUT、DELETE等
     */
    private String httpMethod;

    /**
     * 请求头信息，JSON格式字符串
     */
    private String requestHeaders;

    /**
     * 请求参数信息，JSON格式字符串
     */
    private String requestParams;

    /**
     * 请求体内容，可能已压缩
     */
    private String requestBody;

    /**
     * 响应头信息，JSON格式字符串
     */
    private String responseHeaders;

    /**
     * 响应体内容，可能已压缩
     */
    private String responseBody;

    /**
     * HTTP响应状态码，如：200、404、500等
     */
    private Integer responseStatus;

    /**
     * 响应时间，单位毫秒
     */
    private Long responseTimeMs;

    /**
     * 错误代码，业务层面的错误标识
     */
    private String errorCode;

    /**
     * 错误信息详情
     */
    private String errorMessage;

    /**
     * 是否成功：true-成功，false-失败
     */
    private Boolean isSuccess;

    /**
     * 业务类型：search、booking、cancel、getPrice、getHotel等
     */
    private String businessType;

    /**
     * 客户端IP地址，支持IPv4和IPv6
     */
    private String clientIp;

    /**
     * 用户代理信息，浏览器或客户端标识
     */
    private String userAgent;

    // 业务字段提取结果
    private Map<String, Object> extractedFields;

    // 是否需要记录订单详细日志
    private boolean recordOrderDetail;

    // 订单相关数据（当recordOrderDetail为true时使用）
    private OrderLogData orderData;


}
