package com.heytrip.hotel.supplier.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单日志数据传输对象
 * 用于记录订单相关的详细信息
 *
 * @author Pax
 * @since 1.0.0
 */
@Data
public class OrderLogData {

    /**
     * 酒店标识，酒店的唯一识别码
     */
    private String hotelKey;

    /**
     * 入住日期标识，格式通常为 yyyy-MM-dd
     */
    private String checkInKey;

    /**
     * 离店日期标识，格式通常为 yyyy-MM-dd
     */
    private String checkOutKey;

    /**
     * 房型标识，房间类型的唯一识别码
     */
    private String roomKey;

    /**
     * 房价标识，价格方案的唯一识别码
     */
    private String rateKey;

    /**
     * 入住晚数，总住宿天数
     */
    private Integer nights;

    /**
     * 入住人数，总客人数量
     */
    private Integer guests;

    /**
     * 预订房间数量
     */
    private Integer rooms;

    /**
     * 货币代码，如CNY、USD、EUR等
     */
    private String currency;

    /**
     * 国家代码，如CN、US、GB等
     */
    private String national;

    /**
     * 入住人信息，格式：2-5-3代表2成人2个儿童（1个5岁，1个3岁）
     * 多间房用下划线分割，如：2-5_1-3_2-4-6
     */
    private String occupancy;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 销售总金额
     */
    private BigDecimal saleAmount;

    /**
     * 分销商订单号标识，分销商系统的订单唯一标识
     */
    private String distributionOrdersKey;

    /**
     * 供应商预定标识，供应商系统返回的预订号
     */
    private String supplierBookingKey;

    /**
     * 预定状态，如：CONFIRMED、CANCELLED、PENDING等
     */
    private String bookingStatus;

    /**
     * 原始请求内容，可能已压缩
     */
    private String originalRequest;

    /**
     * 原始响应内容，可能已压缩
     */
    private String originalResponse;


}

