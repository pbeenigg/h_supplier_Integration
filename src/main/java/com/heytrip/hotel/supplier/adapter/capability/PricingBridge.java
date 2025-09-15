package com.heytrip.hotel.supplier.adapter.capability;

import com.heytrip.common.request.XSupplierPriceRequest;
import com.heytrip.common.response.base.XRoom;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 报价能力接口（接口隔离：仅负责报价相关的桥接定义）
 */
public interface PricingBridge {
    /**
     * 单酒店报价
     */
    List<XRoom> getPrice(XSupplierPriceRequest input);

    /**
     * 多酒店报价（可选实现）
     */
    default Map<String, List<XRoom>> getPrices(XSupplierPriceRequest input) {
        return Collections.emptyMap();
    }
}
