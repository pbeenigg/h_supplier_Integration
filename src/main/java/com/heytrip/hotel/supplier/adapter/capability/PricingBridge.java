package com.heytrip.hotel.supplier.adapter.capability;

import com.heytrip.common.request.XSupplierCheckRequest;
import com.heytrip.common.request.XSupplierPriceRequest;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.XOrderCheckResponse;

import java.util.List;
import java.util.Map;

/**
 * 报价能力接口（接口隔离：仅负责报价相关的桥接定义）
 */
public interface PricingBridge {
    
    // ================= 基础报价接口 =================
    
    /**
     * 单酒店报价（标准接口）
     * @param input 报价请求参数
     * @return 房型价格列表
     */
    List<XRoom> getPrice(XSupplierPriceRequest input);

    /**
     * 多酒店报价（可选实现）
     * @param input 报价请求参数
     * @return 酒店ID -> 房型价格列表的映射
     */
    default Map<String, List<XRoom>> getPrices(XSupplierPriceRequest input) {
        throw new UnsupportedOperationException("[PricingBridge.getPrices] 多酒店报价,方法未实现");
    }
    
    // ================= 原始报价接口 =================
    
    /**
     * 获取原始单酒店报价（供应商原始数据格式）
     * @param input 报价请求参数
     * @return 供应商原始报价数据（JSON字符串或对象）
     */
    default Object getPriceOrig(XSupplierPriceRequest input) {
        throw new UnsupportedOperationException("[PricingBridge.getPriceOrig] 获取原始单酒店报价 ,方法未实现");
    }
    
    /**
     * 获取原始多酒店报价（供应商原始数据格式）
     * @param input 报价请求参数
     * @return 供应商原始报价数据（JSON字符串或对象）
     */
    default Object getPricesOrg(XSupplierPriceRequest input) {
        throw new UnsupportedOperationException("[PricingBridge.getPricesOrg] 获取原始多酒店报价,方法未实现");
    }
    


    /**
     * 订单前置校验（标准格式）
     * 说明：在正式下单前校验房型可售性、价格变化等
     * @param input 校验请求参数
     * @return 校验结果（房型列表，包含最新价格和可售状态）
     */
    default XOrderCheckResponse orderCheck(XSupplierCheckRequest input) {
        throw new UnsupportedOperationException("[PricingBridge.orderCheck] 订单前置校验（标准格式）,方法未实现");
    }
    
    /**
     * 订单前置校验（供应商原始格式）
     * 说明：返回供应商原始校验数据，用于调试或特殊业务场景
     * @param input 校验请求参数
     * @return 供应商原始校验数据
     */
    default Object orderCheckOrg(XSupplierCheckRequest input) {
        throw new UnsupportedOperationException("[PricingBridge.orderCheckOrg] 订单前置校验（供应商原始格式）,方法未实现");
    }
}
