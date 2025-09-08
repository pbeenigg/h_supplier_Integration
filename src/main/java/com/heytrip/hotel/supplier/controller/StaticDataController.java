package com.heytrip.hotel.supplier.controller;

import com.heytrip.common.apiservice.ISupplierApiService;
import com.heytrip.common.response.base.XHotel;
import com.heytrip.common.response.other.XCityResponse;
import com.heytrip.common.response.other.XCountryResponse;
import com.heytrip.common.response.other.XHotelIncrement;
import com.heytrip.common.response.other.XRoomIncrement;
import com.heytrip.common.result.Result;
import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * StaticData
 * 静态数据控制器
 * 提供城市,酒店信息，房型，床型，语言，国家,国籍等静态数据API
 *
 * @author Pax
 */
@RestController
@Validated
@RequestMapping("/pax/api/xiwanSupplier/supp")
public class StaticDataController {

    private static final Logger logger = LoggerFactory.getLogger(StaticDataController.class);


    @Resource
    private ISupplierApiService supplierApiService;

    /**
     * 获取国家信息 (国际供应商要实现)
     *
     * @param supplierType 供应商类型
     * @param language     语言
     * @return 国家列表
     */
    @GetMapping("/getCountries")
    public Result<List<XCountryResponse>> getCountries(@RequestParam(value = "supplierType") String supplierType,
                                                       @RequestParam(value = "language", required = false) String language
    ) {
        return supplierApiService.getCountries(supplierType, language);
    }

    /**
     * 获取城市信息 (国际供应商要实现)
     *
     * @param supplierType 供应商类型
     * @param countryId    国家ID
     * @param language     语言
     * @return 城市列表
     */
    @GetMapping("/getCities")
    public Result<List<XCityResponse>> getCities(@RequestParam(value = "supplierType") String supplierType,
                                                 @RequestParam(value = "countryId", required = false) String countryId,
                                                 @RequestParam(value = "language", required = false) String language
    ) {
        return supplierApiService.getCities(supplierType, countryId, language);
    }


    /**
     * 获取可售酒店编号 (获取到没有数据就代表最后一页)
     *
     * @param supplierType 供应商类型
     * @param pageIndex    页码
     * @param pageSize     每页数量
     * @param ext          扩展参数
     * @return 酒店ID列表
     */
    @GetMapping("/getBookableHotelIds")
    public Result<List<String>> getBookableHotelIds(@RequestParam(value = "supplierType") String supplierType,
                                                    @RequestParam("pageIndex") int pageIndex,
                                                    @RequestParam("pageSize") int pageSize,
                                                    @RequestParam(value = "ext", required = false) String ext
    ) {
        return supplierApiService.getBookableHotelIds(supplierType, pageIndex, pageSize, ext);
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
    @GetMapping("/getHotel")
    public Result<XHotel> getHotel(@RequestParam(value = "supplierType") String supplierType,
                                   @RequestParam("hotelId") String hotelId,
                                   @RequestParam(value = "language", required = false) String language,
                                   @RequestParam(value = "ext", required = false) String ext
    ) {
        return supplierApiService.getHotel(supplierType, hotelId, language, ext);
    }

    /**
     * 获取基础房型信息
     *
     * @param supplierType 供应商类型
     * @param hotelId      酒店ID
     * @param language     语言
     * @param ext          扩展参数
     * @return       房型列表
     *
     */
    @GetMapping("/getRooms")
    public Object getRooms(
            @RequestParam(value = "supplierType") String supplierType,
            @RequestParam("hotelId") String hotelId,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "ext", required = false) String ext
    ) {
        return supplierApiService.getRooms(supplierType, hotelId, language, ext);
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
    @GetMapping("/getHotelRoomOrigContent")
    public Object getHotelRoomOrigContent(
            @RequestParam(value = "supplierType") String supplierType,
            @RequestParam("hotelId") String hotelId,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "ext", required = false) String ext
    ) {
        return supplierApiService.getHotelRoomOrigContent(supplierType, hotelId, language, ext);
    }

    /**
     * 获取酒店基础信息关键信息变化增量（例如名称，坐标，地址，电话，城市，国家）
     *
     * @param supplierType 供应商类型
     * @param maxId        最大ID
     * @param query        查询参数
     * @return             酒店增量信息
     */
    @GetMapping("/getHotelIncrement")
    public Result<XHotelIncrement> getHotelIncrement(@RequestParam(value = "supplierType") String supplierType,
                                                     @RequestParam("maxId") Long maxId,
                                                     @RequestParam(value = "query", required = false) String query
    ) {
        return supplierApiService.getHotelIncrement(supplierType, maxId, query);
    }

    /**
     * 获取房型基础信息关键信息变化增量（例如名称，床型，入住人数，面积，窗型，景观）
     *
     * @param supplierType 供应商类型
     * @param maxId        最大ID
     * @param query        查询参数
     * @return             房型增量信息
     */
    @GetMapping("/getRoomIncrement")
    public Result<XRoomIncrement> getRoomIncrement(@RequestParam(value = "supplierType") String supplierType,
                                                   @RequestParam("maxId") Long maxId,
                                                   @RequestParam(value = "query", required = false) String query
    ) {
        return supplierApiService.getRoomIncrement(supplierType, maxId, query);
    }


}
