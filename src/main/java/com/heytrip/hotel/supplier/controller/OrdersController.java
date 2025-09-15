package com.heytrip.hotel.supplier.controller;

import com.heytrip.common.apiservice.ISupplierApiService;
import com.heytrip.common.request.XCancelOrderRequest;
import com.heytrip.common.request.XCreateOrderRequest;
import com.heytrip.common.request.XModifyOrderRequest;
import com.heytrip.common.response.other.XCancelOrderResponse;
import com.heytrip.common.response.other.XCreateOrderResponse;
import com.heytrip.common.response.other.XQueryOrderResponse;
import com.heytrip.common.result.Result;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Orders
 * 订单类控制器
 * 提供酒店订单创建、取消,查询、修改相关的API接口
 * 
 * @author  Pax
 */
@RestController
@Validated
@RequestMapping("/pax/api/xiwanSupplier/supp")
public class OrdersController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrdersController.class);

    @Resource
    private ISupplierApiService supplierApiService;



    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return 订单创建响应
     */
    @PostMapping("/createOrder")
    public Result<XCreateOrderResponse> createOrder(@RequestBody XCreateOrderRequest request) {
        return supplierApiService.createOrder(request);
    }

    /**
     * 取消订单
     *
     * @param request 取消订单请求
     * @return 取消订单响应
     */
    @PostMapping("/cancelOrder")
    public Result<XCancelOrderResponse> cancelOrder(@RequestBody XCancelOrderRequest request) {
        return supplierApiService.cancelOrder(request);
    }

    /**
     * 查询订单
     *
     * @param supplierType 供应商类型
     * @param distributorOrderId 分销商订单号
     * @param supplierOrderId 供应商订单号
     * @param ext 扩展参数
     * @return 订单查询响应
     */
    @GetMapping("/queryOrder")
    public Result<XQueryOrderResponse> queryOrder(
            @RequestParam(value = "supplierType") String supplierType,
            @RequestParam(value = "distributorOrderId", required = false) String distributorOrderId,
            @RequestParam(value = "supplierOrderId",required = false) String supplierOrderId,
            @RequestParam(value = "ext", required = false) String ext

    ) {
        return supplierApiService.queryOrder(supplierType, distributorOrderId, supplierOrderId, ext);
    }

    /**
     * 修改订单 （一般供应商没有这个功能，一般不会影响订单价格修改信息才给修改）
     *
     * @param request 修改订单请求
     * @return 修改订单响应
     */
    @PostMapping("/modifyOrder")
    public Object modifyOrder(@RequestBody XModifyOrderRequest request) {
        return supplierApiService.modifyOrder(request);
    }
}
