package com.heytrip.hotel.supplier.adapter.standard;

import com.heytrip.common.apiservice.ISupplierApiService;
import com.heytrip.common.request.*;
import com.heytrip.common.response.base.XHotel;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.*;
import com.heytrip.common.result.Result;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


/**
 * HeyTrip 内部供应商对接标准接口实现
 *
 * @author Pax
 */

@Service
public class SupplierApiService implements ISupplierApiService {



    /**
     * 获取城市信息 (国际供应商要实现)
     *
     * @param supplierType 供应商类型
     * @param countryId    国家ID
     * @param language     语言
     * @return 城市列表
     */
    @Override
    public Result<List<XCityResponse>> getCities(String supplierType, String countryId, String language) {
        return null;
    }

    /**
     * 获取国家信息 (国际供应商要实现)
     *
     * @param supplierType 供应商类型
     * @param language     语言
     * @return 国家列表
     */
    @Override
    public Result<List<XCountryResponse>> getCountries(String supplierType, String language) {
        return null;
    }


    /**
     * 获取酒店可售列表
     *
     * @param supplierType 供应商类型
     * @param pageIndex    页码
     * @param pageSize     每页大小
     * @param ext          扩展参数
     * @return 酒店ID列表
     */
    @Override
    public Result<List<String>> getBookableHotelIds(String supplierType, int pageIndex, int pageSize, String ext) {
        return null;
    }


    /**
     * 获取酒店信息
     *
     * @param supplierType 供应商类型
     * @param hotelId      酒店ID
     * @param language     语言
     * @param ext          扩展参数
     * @return 酒店信息
     */
    @Override
    public Result<XHotel> getHotel(String supplierType, String hotelId, String language, String ext) {
        return null;
    }

    /**
     * 获取酒店房型信息
     *
     * @param supplierType 供应商类型
     * @param hotelId      酒店ID
     * @param language     语言
     * @param ext          扩展参数
     * @return 房型列表
     */
    @Override
    public Result<List<XRoom>> getRooms(String supplierType, String hotelId, String language, String ext) {
        return null;
    }

    /**
     * 获取酒店增量信息
     *
     * @param supplierType 供应商类型
     * @param maxId        最大ID
     * @param query        查询参数
     * @return 酒店增量信息
     */
    @Override
    public Result<XHotelIncrement> getHotelIncrement(String supplierType, long maxId, String query) {
        return null;
    }


    /**
     * 获取房型增量信息
     *
     * @param supplierType 供应商类型
     * @param maxId        最大ID
     * @param query        查询参数
     * @return 房型增量信息
     */
    @Override
    public Result<XRoomIncrement> getRoomIncrement(String supplierType, long maxId, String query) {
        return null;
    }


    /**
     * 获取供应商酒店房型基础信息(原文)
     *
     * @param supplierType 供应商类型
     * @param hotelId      酒店ID
     * @param language     语言
     * @param ext          扩展参数
     * @return             原文数据
     */
    @Override
    public Object getHotelRoomOrigContent(String supplierType, String hotelId, String language, String ext) {
        return null;
    }



    /**
     * 获取报价(单酒店)
     *
     * @param input 供应商报价请求
     * @return 房型列表
     */
    @Override
    public Result<List<XRoom>> getPrice(XSupplierPriceRequest input) {
        return null;
    }


    /**
     * 获取报价(多酒店)
     *
     * @param input 供应商报价请求
     * @return 酒店ID到房型列表的映射
     */
    @Override
    public Result<Map<String, List<XRoom>>> getPrices(XSupplierPriceRequest input) {
        return null;
    }


    /**
     * 获取价格增量信息
     *
     * @param supplierType      供应商类型
     * @param maxId             最大ID
     * @param minTime           最小更新时间
     * @param includeChangeDate 是否包含变更日期
     * @param query             查询参数
     * @return                  价格增量信息
     */
    @Override
    public Result<XPriceCacheIncrementResponse> GetPriceCacheIncrement(String supplierType, long maxId, Long minTime, Boolean includeChangeDate, String query) {
        return null;
    }


    /**
     * 验单
     *
     * @param input 供应商验单请求
     * @return 验单响应
     */
    @Override
    public Result<XOrderCheckResponse> orderCheck(XSupplierCheckRequest input) {
        return null;
    }


    /**
     * 创建订单
     *
     * @param input 创建订单请求
     * @return 订单创建响应
     */
    @Override
    public Result<XCreateOrderResponse> createOrder(XCreateOrderRequest input) {
        return null;
    }


    /**
     * 取消订单
     *
     * @param input 取消订单请求
     * @return 取消订单响应
     */
    @Override
    public Result<XCancelOrderResponse> cancelOrder(XCancelOrderRequest input) {
        return null;
    }


    /**
     * 查询订单
     *
     * @param supplierType       供应商类型
     * @param distributorOrderId 分销商订单号
     * @param supplierOrderId    供应商订单号
     * @param ext                扩展参数
     * @return 订单查询响应
     */
    @Override
    public Result<XQueryOrderResponse> queryOrder(String supplierType, String distributorOrderId, String supplierOrderId, String ext) {
        return null;
    }


    /**
     * 修改订单 （一般供应商没有这个功能，一般不会影响订单价格修改信息才给修改）
     *
     * @param request 修改订单请求
     * @return 修改订单响应
     */
    @Override
    public Result<XModifyOrderResponse> modifyOrder(XModifyOrderRequest request) {
        return null;
    }


    /**
     * 获取报价(单酒店)原文
     *
     * @param input 报价请求
     * @return 原文响应
     */
    @Override
    public Object getPriceOrig(XSupplierPriceRequest input) {
        return null;
    }


    /**
     * 获取报价(多酒店)原文
     *
     * @param input 报价请求
     * @return 原文响应
     */
    @Override
    public Object getPricesOrg(XSupplierPriceRequest input) {
        return null;
    }


    /**
     * 验单原文
     *
     * @param input 验单请求
     * @return 原文响应
     */
    @Override
    public Object orderCheckOrg(XSupplierCheckRequest input) {
        return null;
    }


}
