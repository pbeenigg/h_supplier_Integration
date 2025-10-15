package com.heytrip.hotel.supplier.adapter.capability;

/**
 * 酒店基础能力接口（接口隔离：仅负责酒店基础接口的桥接定义）
 */
public interface StaticBridge {


    /**
     * 获取供应商酒店房型基础信息(原文)
     * @param supplierType
     * @param hotelId
     * @param language
     * @param ext
     * @return
     */
     Object getHotelRoomOrigContent(String supplierType, String hotelId, String language, String ext);
}
