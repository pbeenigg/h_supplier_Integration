package com.heytrip.hotel.supplier.adapter.service;

import com.heytrip.common.apiservice.ISupplierApiService;
import com.heytrip.common.request.*;
import com.heytrip.common.response.base.XHotel;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.*;
import com.heytrip.common.result.Result;
import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * HeyTrip 内部供应商对接标准接口实现
 *
 * ### 静态数据类接口
 * - getCities
 * - getCountries
 * - getHotel
 * - getRooms
 * - getBookableHotelIds
 * - getHotelIncrement
 * - getRoomIncrement
 * - getHotelRoomOrigContent
 *
 * ### 报价类接口
 * - getPrice
 * - getPrices
 * - GetPriceCacheIncrement
 * - orderCheck
 * - getPriceOrig
 * - getPricesOrg
 * - orderCheckOrg
 *
 * ### 订单类接口
 * - createOrder
 * - cancelOrder
 * - queryOrder
 * - modifyOrder
 *
 * @author Pax
 */
@Service
public class SupplierApiService implements ISupplierApiService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierApiService.class);

    @Autowired
    private SupplierAdapterManager adapterManager;

    @Autowired
    private StaticDataQueryService staticDataQueryService;

    // ================================== 静态数据类查询接口入口 ==================================
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
        try {
            logger.info("[getCities] supplierType={}, countryId={}, language={}", supplierType, countryId, language);
            var adapter = adapterManager.getAdapterByName(supplierType);
            if (adapter == null) {
                logger.warn("[getCities] 未找到供应商适配器, supplierType={}", supplierType);
                return Result.ok(Collections.emptyList());
            }

            Long supplierId = adapter.getSupplierId();
            String supplierName = adapter.getSupplierName(); // 按约定：supplierName 等于 supplierType

            var page = staticDataQueryService.pageCities(supplierId, supplierName, null, countryId, 0, 1000);
            return Result.ok(page.getContent());
        } catch (Exception ex) {
            logger.error("[getCities] 查询失败", ex);
            return Result.ok(Collections.emptyList());
        }
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
        try {
            logger.info("[getCountries] supplierType={}, language={}", supplierType, language);
            var adapter = adapterManager.getAdapterByName(supplierType);
            if (adapter == null) {
                logger.warn("[getCountries] 未找到供应商适配器, supplierType={}", supplierType);
                return Result.ok(Collections.emptyList());
            }

            Long supplierId = adapter.getSupplierId();
            String supplierName = adapter.getSupplierName();

            var page = staticDataQueryService.pageCountries(supplierId, supplierName, null, 0, 1000);
            return Result.ok(page.getContent());
        } catch (Exception ex) {
            logger.error("[getCountries] 查询失败", ex);
            return Result.ok(Collections.emptyList());
        }
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
        try {
            logger.info("[getHotel] supplierType={}, hotelId={}, language={}, ext={}", supplierType, hotelId, language, ext);
            var adapter = adapterManager.getAdapterByName(supplierType);
            if (adapter == null) {
                logger.warn("[getHotel] 未找到供应商适配器, supplierType={}", supplierType);
                return Result.ok(null);
            }

            Long supplierId = adapter.getSupplierId();
            String supplierName = adapter.getSupplierName();

            var page = staticDataQueryService.pageHotels(supplierId, supplierName, hotelId, null,  null, 0, 1);
            XHotel hotel = page.getContent().isEmpty() ? null : page.getContent().get(0);
            return Result.ok(hotel);
        } catch (Exception ex) {
            logger.error("[getHotel] 查询失败", ex);
            return Result.ok(null);
        }
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
        try {
            logger.info("[getRooms] supplierType={}, hotelId={}, language={}, ext={}", supplierType, hotelId, language, ext);
            var adapter = adapterManager.getAdapterByName(supplierType);
            if (adapter == null) {
                logger.warn("[getRooms] 未找到供应商适配器, supplierType={}", supplierType);
                return Result.ok(Collections.emptyList());
            }

            Long supplierId = adapter.getSupplierId();
            String supplierName = adapter.getSupplierName();

            var page = staticDataQueryService.pageRooms(supplierId, supplierName, hotelId, null, null, 0, 1000);
            return Result.ok(page.getContent());
        } catch (Exception ex) {
            logger.error("[getRooms] 查询失败", ex);
            return Result.ok(Collections.emptyList());
        }
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
        // 第1阶段占位返回，后续与可售标识同步流程打通
        logger.info("[getBookableHotelIds] supplierType={}, pageIndex={}, pageSize={}, ext={}", supplierType, pageIndex, pageSize, ext);
        return Result.ok(Collections.emptyList());
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
        // 第1阶段占位返回，后续结合同步日志/增量表完善
        logger.info("[getHotelIncrement] supplierType={}, maxId={}, query={}", supplierType, maxId, query);
        return Result.ok(null);
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
        // 第1阶段占位返回，后续结合同步日志/增量表完善
        logger.info("[getRoomIncrement] supplierType={}, maxId={}, query={}", supplierType, maxId, query);
        return Result.ok(null);
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
        // 第1阶段占位返回，后续在静态数据同步中维护原文快照字段
        logger.info("[getHotelRoomOrigContent] supplierType={}, hotelId={}, language={}, ext={}", supplierType, hotelId, language, ext);
        return Collections.emptyMap();
    }
    // ================================== 静态数据查询接口 ==================================






    // ================================== 报价类接口入口 ==================================
    /**
     * 获取报价(单酒店)
     *
     * @param input 供应商报价请求
     * @return 房型列表
     */
    @Override
    public Result<List<XRoom>> getPrice(XSupplierPriceRequest input) {
        logger.info("[getPrice] input={}", input);
        String supplierType = input.getSupplierType();
        return adapterManager.getPrice(supplierType, input);
    }


    /**
     * 获取报价(多酒店)
     *
     * @param input 供应商报价请求
     * @return 酒店ID到房型列表的映射
     */
    @Override
    public Result<Map<String, List<XRoom>>> getPrices(XSupplierPriceRequest input) {
        logger.info("[getPrices] input={}", input);
        // 多酒店报价委派（后续在适配器补齐具体实现）；暂返回单酒店结构的兼容实现
        Result<List<XRoom>> single = getPrice(input);
        Map<String, List<XRoom>> map = new java.util.HashMap<>();
        if (single != null && single.getData() != null) {
            map.put(input.getHotelIds(), single.getData());
        }
        return Result.ok(map);
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
        logger.info("[GetPriceCacheIncrement] supplierType={}, maxId={}, minTime={}, includeChangeDate={}, query={}", supplierType, maxId, minTime, includeChangeDate, query);
        return Result.ok(null);
    }


    /**
     * 验单
     *
     * @param input 供应商验单请求
     * @return 验单响应
     */
    @Override
    public Result<XOrderCheckResponse> orderCheck(XSupplierCheckRequest input) {
        logger.info("[orderCheck] input={}", input);
        // 占位：可在适配器内结合取消政策或库存校验实现，当前返回空
        return Result.ok(null);
    }




    /**
     * 获取报价(单酒店)原文
     *
     * @param input 报价请求
     * @return 原文响应
     */
    @Override
    public Object getPriceOrig(XSupplierPriceRequest input) {
        logger.info("[getPriceOrig] input={}", input);
        try {
            return adapterManager.getPriceOrig(input);
        } catch (Exception e) {
            logger.error("[getPriceOrig] 获取原始报价失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取报价(多酒店)原文
     *
     * @param input 报价请求
     * @return 原文响应
     */
    @Override
    public Object getPricesOrg(XSupplierPriceRequest input) {
        logger.info("[getPricesOrg] input={}", input);
        try {
            return adapterManager.getPricesOrg(input);
        } catch (Exception e) {
            logger.error("[getPricesOrg] 获取多酒店原始报价失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 验单原文
     *
     * @param input 验单请求
     * @return 原文响应
     */
    @Override
    public Object orderCheckOrg(XSupplierCheckRequest input) {
        logger.info("[orderCheckOrg] input={}", input);
        try {
            // 将 XSupplierCheckRequest 转换为 XSupplierPriceRequest
            XSupplierPriceRequest priceRequest = convertCheckRequestToPriceRequest(input);
            return adapterManager.orderCheckOrg(priceRequest);
        } catch (Exception e) {
            logger.error("[orderCheckOrg] 验单原文失败", e);
            return Collections.emptyMap();
        }
    }


    // ================================== 报价类接口 ==================================




    // ================================== 订单类接口入口 ==================================
    /**
     * 创建订单
     *
     * @param input 创建订单请求
     * @return 订单创建响应
     */
    @Override
    public Result<XCreateOrderResponse> createOrder(XCreateOrderRequest input) {
        logger.info("[createOrder] input={}", input);
        String supplierType = input.getSupplierType();
        return adapterManager.createOrder(supplierType, input);
    }


    /**
     * 取消订单
     *
     * @param input 取消订单请求
     * @return 取消订单响应
     */
    @Override
    public Result<XCancelOrderResponse> cancelOrder(XCancelOrderRequest input) {
        logger.info("[cancelOrder] input={}", input);
        String supplierType = input.getSupplierType();
        return adapterManager.cancelOrder(supplierType, input);
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
        logger.info("[queryOrder] supplierType={}, distributorOrderId={}, supplierOrderId={}, ext={}", supplierType, distributorOrderId, supplierOrderId, ext);
        return adapterManager.queryOrder(supplierType, distributorOrderId, supplierOrderId, ext);
    }


    /**
     * 修改订单 （一般供应商没有这个功能，一般不会影响订单价格修改信息才给修改）
     *
     * @param request 修改订单请求
     * @return 修改订单响应
     */
    @Override
    public Result<XModifyOrderResponse> modifyOrder(XModifyOrderRequest request) {
        logger.info("[modifyOrder] request={}", request);
        // 占位实现
        return Result.ok(null);
    }
    
    // ================================== 工具方法 ==================================
    
    /**
     * 将 XSupplierCheckRequest 转换为 XSupplierPriceRequest
     * 
     * @param checkRequest 验单请求
     * @return 报价请求
     */
    private XSupplierPriceRequest convertCheckRequestToPriceRequest(XSupplierCheckRequest checkRequest) {
        XSupplierPriceRequest priceRequest = new XSupplierPriceRequest();
        
        // 复制基础字段
        priceRequest.setSupplierType(checkRequest.getSupplierType());
        priceRequest.setHotelId(checkRequest.getHotelId());
        priceRequest.setCheckInDate(checkRequest.getCheckInDate());
        priceRequest.setCheckOutDate(checkRequest.getCheckOutDate());
        priceRequest.setCurrency(checkRequest.getCurrency());
        priceRequest.setOccupancy(checkRequest.getOccupancy());
        priceRequest.setRoomNum(checkRequest.getRoomNum());
        
        // 如果有其他特定字段需要转换，可以在这里添加
        
        return priceRequest;
    }
    
    // ================================== 订单类接口入口 ==================================


}
