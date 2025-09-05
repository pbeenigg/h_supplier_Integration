package com.heytrip.hotel.supplier.adapter;

import com.heytrip.hotel.supplier.dto.request.HotelSearchRequest;
import com.heytrip.hotel.supplier.dto.request.CreateOrderRequest;
import com.heytrip.hotel.supplier.dto.response.HotelSearchResponse;
import com.heytrip.hotel.supplier.dto.response.CreateOrderResponse;
import reactor.core.publisher.Mono;

/**
 * 供应商适配器接口
 * 定义所有供应商必须实现的核心方法
 * 
 * @author  Pax
 */
public interface SupplierAdapter {
    
    /**
     * 获取供应商名称
     * @return 供应商名称
     */
    String getSupplierName();
    
    /**
     * 检查供应商是否支持指定城市
     * @param city 城市名称
     * @return 是否支持
     */
    boolean supportsCity(String city);
    
    /**
     * 酒店搜索
     * @param request 搜索请求
     * @return 搜索结果
     */
    Mono<HotelSearchResponse> searchHotels(HotelSearchRequest request);
    
    /**
     * 创建订单
     * @param request 订单请求
     * @return 订单响应
     */
    Mono<CreateOrderResponse> createOrder(CreateOrderRequest request);
    
    /**
     * 取消订单
     * @param bookingReference 订单号
     * @param reason 取消原因
     * @return 取消结果
     */
    Mono<Boolean> cancelOrder(String bookingReference, String reason);
    
    /**
     * 查询订单状态
     * @param bookingReference 订单号
     * @return 订单状态
     */
    Mono<Integer> getOrderStatus(String bookingReference);
    
    /**
     * 健康检查
     * @return 是否健康
     */
    Mono<Boolean> healthCheck();
    
    /**
     * 获取供应商优先级
     * 数值越小优先级越高
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * 是否启用该供应商
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * 获取供应商超时时间（毫秒）
     * @return 超时时间
     */
    default long getTimeoutMs() {
        return 30000L; // 默认30秒
    }
    
    /**
     * 获取重试次数
     * @return 重试次数
     */
    default int getRetryCount() {
        return 3;
    }
}
