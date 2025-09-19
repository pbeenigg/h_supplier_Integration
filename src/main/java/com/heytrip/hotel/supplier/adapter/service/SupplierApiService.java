package com.heytrip.hotel.supplier.adapter.service;

import com.heytrip.common.apiservice.ISupplierApiService;
import com.heytrip.common.request.*;
import com.heytrip.common.response.base.XHotel;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.*;
import com.heytrip.common.result.PageDto;
import com.heytrip.common.result.Result;
import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.entity.Hotel;
import com.heytrip.hotel.supplier.entity.Room;
import com.heytrip.hotel.supplier.repository.HotelBookableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import com.heytrip.hotel.supplier.entity.HotelBookable;

/**
 * HeyTrip 内部供应商对接标准接口实现
 * <p>
 * ### 静态数据类接口
 * - getCities
 * - getCountries
 * - getHotel
 * - getRooms
 * - getBookableHotelIds
 * - getHotelIncrement
 * - getRoomIncrement
 * - getHotelRoomOrigContent
 * <p>
 * ### 报价类接口
 * - getPrice
 * - getPrices
 * - GetPriceCacheIncrement
 * - orderCheck
 * - getPriceOrig
 * - getPricesOrg
 * - orderCheckOrg
 * <p>
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

    @Autowired
    private HotelBookableRepository hotelBookableRepository;

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

            Optional<XHotel> hotel = staticDataQueryService.getHotelByHotelCode(supplierId, supplierName, hotelId);
            return Result.ok(hotel.orElse(null));
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

            var page = staticDataQueryService.pageRooms(supplierId, supplierName, hotelId, null, 0, 1000);
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
        logger.info("[getBookableHotelIds] supplierType={}, pageIndex={}, pageSize={}, ext={}", supplierType, pageIndex, pageSize, ext);

        try {
            var adapter = adapterManager.getAdapterByName(supplierType);
            if (adapter == null) {
                logger.warn("[getBookableHotelIds] 未找到供应商适配器, supplierType={}", supplierType);
                return Result.ok(Collections.emptyList());
            }

            Long supplierId = adapter.getSupplierId();
            String supplierCode = adapter.getSupplierName();

            // 构建分页参数，限制pageSize最大100
            Pageable pageable = PageRequest.of(Math.max(pageIndex, 0), Math.min(Math.max(pageSize, 1), 100));



            // 构建Specification查询条件（必须条件：supplierId + supplierCode + isBookable=true）
            Specification<HotelBookable> spec = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                // 必须条件：supplierId
                predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplierId));
                // 必须条件：supplierCode
                predicates.add(criteriaBuilder.equal(root.get("supplierCode"), supplierCode));
                // 必须条件：isBookable = true
                predicates.add(criteriaBuilder.equal(root.get("isBookable"), true));
                
                // 按创建时间倒序排列
                query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
                
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };

            // 执行分页查询
            Page<HotelBookable> pageData = hotelBookableRepository.findAll(spec, pageable);
            
            // 提取酒店代码列表
            List<String> hotelCodes = pageData.getContent().stream()
                    .map(HotelBookable::getHotelCode)
                    .collect(Collectors.toList());

            logger.info("[getBookableHotelIds] 查询完成，supplierType={}, supplierId={}, supplierCode={}, pageIndex={}, pageSize={}, totalElements={}, currentPageSize={}",
                    supplierType, supplierId, supplierCode, pageIndex, pageSize, pageData.getTotalElements(), hotelCodes.size());

            // 构建分页信息
            PageDto resultPage = new PageDto(
                pageData.getNumber() + 1, // 当前页(从1开始)
                pageData.getSize(), // 每页行数
                (int) pageData.getTotalElements(), // 总记录数
                pageData.getTotalPages() // 总页数
            );

            return Result.ok(hotelCodes, resultPage);

        } catch (Exception e) {
            logger.error("[getBookableHotelIds] 查询可售酒店列表失败，supplierType={}, pageIndex={}, pageSize={}",
                    supplierType, pageIndex, pageSize, e);
            return Result.ok(Collections.emptyList());
        }
    }




    // ================================== 增量查询接口入口 ==================================

    /**
     * 获取酒店基础信息关键信息变化增量（例如名称，坐标，地址，电话，城市，国家）
     * 基于自增ID的增量查询，查询ID大于指定maxId的记录
     * @param supplierType 供应商类型
     * @param maxId        上次请求的最大增量编
     * @param query        查询参数
     * @return 酒店增量信息
     */
    @Override
    public Result<XHotelIncrement> getHotelIncrement(String supplierType, long maxId, String query) {
        logger.info("[getHotelIncrement] 开始执行酒店增量查询，供应商类型：{}，最大ID：{}，查询参数：{}", 
                   supplierType, maxId, query);
        
        try {
            // 获取供应商适配器
            var adapter = adapterManager.getAdapterByName(supplierType);
            if (adapter == null) {
                logger.warn("[getHotelIncrement] 未找到供应商适配器, supplierType={}", supplierType);
                return Result.fail("未找到供应商适配器");
            }

            Long supplierId = adapter.getSupplierId();
            String supplierCode = adapter.getSupplierName(); // 按约定：supplierName 等于 supplierType
            
            // 默认每页大小为1000，可以通过query参数调整
            int pageSize = 1000;
            if (query != null && query.contains("pageSize=")) {
                try {
                    String pageSizeStr = query.substring(query.indexOf("pageSize=") + 9);
                    if (pageSizeStr.contains("&")) {
                        pageSizeStr = pageSizeStr.substring(0, pageSizeStr.indexOf("&"));
                    }
                    pageSize = Math.min(Integer.parseInt(pageSizeStr), 1000);
                } catch (Exception e) {
                    logger.warn("[getHotelIncrement] 解析pageSize参数失败，使用默认值1000");
                }
            }
            
            // 调用增量ID查询服务
            Page<Hotel> pageData = staticDataQueryService.getIncrementalHotels(supplierId, supplierCode, maxId, pageSize);
            // 提取房型代码并转换为XRoomIncrementDetail对象
            List<XHotelIncrement.XHotelIncrementDetail> hotelDetails = pageData.getContent().stream()
                    .map(hotel -> {
                        XHotelIncrement.XHotelIncrementDetail detail = new XHotelIncrement.XHotelIncrementDetail();
                        detail.setHotelId(hotel.getHotelCodeMd5());
                        return detail;
                    })
                    .collect(Collectors.toList());

            // 计算本次查询的最大ID
            Long currentMaxId = pageData.getContent().stream()
                    .mapToLong(Hotel::getId)
                    .max()
                    .orElse(maxId);

            // 构建XHotelIncrement响应对象
            XHotelIncrement increment = new XHotelIncrement();
            increment.setMaxId(currentMaxId);
            increment.setDetails(hotelDetails);

            // 构建分页信息
            PageDto resultPage = new PageDto(
                    pageData.getNumber() + 1, // 当前页(从1开始)
                    pageData.getSize(), // 每页行数
                    (int) pageData.getTotalElements(), // 总记录数
                    pageData.getTotalPages() // 总页数
            );
            
            return Result.ok(increment,resultPage);
            
        } catch (Exception e) {
            logger.error("[getHotelIncrement] 酒店增量查询异常", e);
            return Result.fail("酒店增量查询失败：" + e.getMessage());
        }
    }


    /**
     * 获取房型基础信息关键信息变化增量（例如名称，床型，入住人数，面积，窗型，景观）
     * 基于自增ID的增量查询，查询ID大于指定maxId的记录
     * @param supplierType 供应商类型
     * @param maxId        上次请求的最大增量编号
     * @param query        查询参数
     * @return 房型增量信息
     */
    @Override
    public Result<XRoomIncrement> getRoomIncrement(String supplierType, long maxId, String query) {
        logger.info("[getRoomIncrement] 开始执行房型增量查询，供应商类型：{}，最大ID：{}，查询参数：{}", 
                   supplierType, maxId, query);
        
        try {
            // 获取供应商适配器
            var adapter = adapterManager.getAdapterByName(supplierType);
            if (adapter == null) {
                logger.warn("[getRoomIncrement] 未找到供应商适配器, supplierType={}", supplierType);
                return Result.fail("未找到供应商适配器");
            }

            Long supplierId = adapter.getSupplierId();
            String supplierCode = adapter.getSupplierName(); // 按约定：supplierName 等于 supplierType
            
            // 默认每页大小为1000，可以通过query参数调整
            int pageSize = 1000;
            if (query != null && query.contains("pageSize=")) {
                try {
                    String pageSizeStr = query.substring(query.indexOf("pageSize=") + 9);
                    if (pageSizeStr.contains("&")) {
                        pageSizeStr = pageSizeStr.substring(0, pageSizeStr.indexOf("&"));
                    }
                    pageSize = Math.min(Integer.parseInt(pageSizeStr), 1000);
                } catch (Exception e) {
                    logger.warn("[getRoomIncrement] 解析pageSize参数失败，使用默认值1000");
                }
            }
            
            // 调用增量ID查询服务
            Page<Room> pageData = staticDataQueryService.getIncrementalRooms(supplierId, supplierCode, maxId, pageSize);
            // 提取房型代码并转换为XRoomIncrementDetail对象
            List<XRoomIncrement.XRoomIncrementDetail> roomDetails = pageData.getContent().stream()
                    .map(room -> {
                        XRoomIncrement.XRoomIncrementDetail detail = new XRoomIncrement.XRoomIncrementDetail();
                        detail.setHotelId(room.getRoomCodeMd5());
                        return detail;
                    })
                    .collect(Collectors.toList());

            // 计算本次查询的最大ID
            Long currentMaxId = pageData.getContent().stream()
                    .mapToLong(Room::getId)
                    .max()
                    .orElse(maxId);

            // 构建XRoomIncrement响应对象
            XRoomIncrement increment = new XRoomIncrement();
            increment.setMaxId(currentMaxId);
            increment.setDetails(roomDetails);

            // 构建分页信息
            PageDto resultPage = new PageDto(
                    pageData.getNumber() + 1, // 当前页(从1开始)
                    pageData.getSize(), // 每页行数
                    (int) pageData.getTotalElements(), // 总记录数
                    pageData.getTotalPages() // 总页数
            );
            
            return Result.ok(increment,resultPage);
            
        } catch (Exception e) {
            logger.error("[getRoomIncrement] 房型增量查询异常", e);
            return Result.fail("房型增量查询失败：" + e.getMessage());
        }
    }





    /**
     * 获取供应商酒店房型基础信息(原文)
     *
     * @param supplierType 供应商类型
     * @param hotelId      酒店ID
     * @param language     语言
     * @param ext          扩展参数
     * @return 原文数据
     */
    @Override
    public Object getHotelRoomOrigContent(String supplierType, String hotelId, String language, String ext) {
        logger.info("[getHotelRoomOrigContent] supplierType={}, hotelId={}, language={}, ext={}", supplierType, hotelId, language, ext);
        return adapterManager.getHotelRoomOrigContent(supplierType, hotelId, language, ext);
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
        return adapterManager.getPrice(input);
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
        return adapterManager.getPrices(input);
    }


    /**
     *  获取价格缓存变价增量(一般国内供应商需要使用)
     *
     * @param supplierType      供应商类型
     * @param maxId             上次请求的最大增量编号
     * @param minTime           最小更新时间
     * @param includeChangeDate 是否包含变更日期
     * @param query             查询参数
     * @return 价格增量信息
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
        return adapterManager.orderCheck(input);
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
        return adapterManager.getPriceOrig(input);
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
        return adapterManager.getPricesOrg(input);
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
        return adapterManager.orderCheckOrg(input);
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
