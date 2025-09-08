package com.heytrip.hotel.supplier.controller;

import com.heytrip.common.apiservice.ISupplierApiService;
import com.heytrip.common.request.XSupplierCheckRequest;
import com.heytrip.common.request.XSupplierPriceRequest;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.XOrderCheckResponse;
import com.heytrip.common.result.Result;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Quotation
 * 报价类控制器
 * 提供酒店报价、验单相关的API接口
 *
 * @author Pax
 */
@RestController
@Validated
@RequestMapping("/pax/api/xiwanSupplier/supp")
public class HotelQuotationController {

    private static final Logger logger = LoggerFactory.getLogger(HotelQuotationController.class);

    @Resource
    private ISupplierApiService supplierApiService;

    /**
     * 获取报价（单酒店）
     *
     * @param xwPriceRequest 报价请求
     * @return 酒店房型列表
     */
    @GetMapping("/getPrice")
    public Result<List<XRoom>> getPrice(@ModelAttribute XSupplierPriceRequest xwPriceRequest) {
        xwPriceRequest.setHotelIds(xwPriceRequest.getHotelId());
        return supplierApiService.getPrice(xwPriceRequest);
    }

    /**
     * 获取报价(多酒店)
     *
     * @param xwPriceRequest 报价请求
     * @return 酒店ID到房型列表的映射
     */
    @GetMapping("/getPrices")
    public Result<Map<String, List<XRoom>>> getPrices(@ModelAttribute XSupplierPriceRequest xwPriceRequest) {
        return supplierApiService.getPrices(xwPriceRequest);
    }

    /**
     * 验单
     *
     * @param xwCheckRequest 验单请求
     * @return 验单响应
     */
    @GetMapping("/orderCheck")
    public Result<XOrderCheckResponse> checkOrder(@ModelAttribute XSupplierCheckRequest xwCheckRequest) {
        return supplierApiService.orderCheck(xwCheckRequest);
    }


    /**
     * 获取报价(单酒店)原文
     *
     * @param xwPriceRequest 报价请求
     * @return 原文响应
     */
    @GetMapping("/getPriceOrg")
    public Object getPriceOrig(@ModelAttribute XSupplierPriceRequest xwPriceRequest) {
        xwPriceRequest.setHotelIds(xwPriceRequest.getHotelId());
        return supplierApiService.getPriceOrig(xwPriceRequest);
    }

    /**
     * 获取报价(多酒店)原文
     *
     * @param xwPriceRequest 报价请求
     * @return 原文响应
     */
    @GetMapping("/getPricesOrg")
    public Object getPricesOrig(@ModelAttribute XSupplierPriceRequest xwPriceRequest) {
        return supplierApiService.getPricesOrg(xwPriceRequest);
    }


    /**
     * 验单原文
     *
     * @param xwCheckRequest 验单请求
     * @return 原文响应
     */
    @GetMapping("/checkOrderOrg")
    public Object checkOrderOrg(@ModelAttribute XSupplierCheckRequest xwCheckRequest) {
        return supplierApiService.orderCheckOrg(xwCheckRequest);
    }
}
