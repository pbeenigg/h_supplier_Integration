package com.heytrip.hotel.supplier.adapter.capability;

import com.heytrip.common.request.XCancelOrderRequest;
import com.heytrip.common.request.XCreateOrderRequest;
import com.heytrip.common.response.other.XCancelOrderResponse;
import com.heytrip.common.response.other.XCreateOrderResponse;
import com.heytrip.common.response.other.XQueryOrderResponse;

/**
 * 订单能力接口（接口隔离：仅负责订单相关的桥接定义）
 */
public interface OrderBridge {
    /**
     * 创建订单
     */
    XCreateOrderResponse createOrder(XCreateOrderRequest input);

    /**
     * 取消订单
     */
    XCancelOrderResponse cancelOrder(XCancelOrderRequest input);

    /**
     * 查询订单
     */
    XQueryOrderResponse queryOrder(String distributorOrderId, String supplierOrderId, String ext);
}
