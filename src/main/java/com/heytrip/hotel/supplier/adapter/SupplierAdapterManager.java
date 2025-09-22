package com.heytrip.hotel.supplier.adapter;

import com.heytrip.common.request.XCancelOrderRequest;
import com.heytrip.common.request.XCreateOrderRequest;
import com.heytrip.common.request.XSupplierCheckRequest;
import com.heytrip.common.request.XSupplierPriceRequest;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.XCancelOrderResponse;
import com.heytrip.common.response.other.XCreateOrderResponse;
import com.heytrip.common.response.other.XOrderCheckResponse;
import com.heytrip.common.response.other.XQueryOrderResponse;
import com.heytrip.common.result.Result;
import com.heytrip.hotel.supplier.adapter.capability.OrderBridge;
import com.heytrip.hotel.supplier.adapter.capability.PricingBridge;
import com.heytrip.hotel.supplier.adapter.capability.StaticBridge;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 供应商适配器管理器
 * 负责管理所有供应商适配器，协调多供应商查询和聚合结果
 * 
 * @author  Pax
 */
@Component
public class SupplierAdapterManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SupplierAdapterManager.class);
    
    @Autowired
    private List<SupplierAdapter> supplierAdapters;
    
    private List<SupplierAdapter> enabledAdapters;

    /**
     * 通过 supplierName 路由的适配器映射
     */
    private final Map<String, SupplierAdapter> adapterByName = new HashMap<>();
    
    @PostConstruct
    public void initialize() {
        // 过滤启用的适配器并按优先级排序
        enabledAdapters = supplierAdapters.stream()
                .filter(SupplierAdapter::isEnabled)
                .sorted(Comparator.comparingInt(SupplierAdapter::getPriority))
                .collect(Collectors.toList());
        
        logger.info("已初始化 {} 已启用的供应商适配器", enabledAdapters.size());
        enabledAdapters.forEach(adapter -> 
                logger.info("已启用的适配器: {} 具有优先级: {}",
                        adapter.getSupplierName(), adapter.getPriority()));

        // 构建 SupplierName -> adapter 的映射（统一转大写存储，路由时忽略大小写）
        adapterByName.clear();
        enabledAdapters.forEach(adapter -> {
            try {
                String supplierName = adapter.getSupplierName();
                if (supplierName != null && !supplierName.isEmpty()) {
                    adapterByName.put(supplierName.toUpperCase(), adapter);
                } else {
                    logger.warn("适配器未提供有效的SupplierName, supplierName={}", adapter.getSupplierName());
                }
            } catch (Exception ex) {
                logger.error("构建适配器路由映射失败: {}", adapter.getSupplierName(), ex);
            }
        });
        logger.info("已建立按SupplierName路由的映射表，数量: {}", adapterByName.size());
    }
    

    /**
     * 获取所有启用的供应商列表
     * @return 供应商名称列表
     */
    public List<String> getEnabledSuppliers() {
        return enabledAdapters.stream()
                .map(SupplierAdapter::getSupplierName)
                .collect(Collectors.toList());
    }
    
    /**
     * 按 SupplierName 获取适配器
     * @param supplierName 供应商名称（不区分大小写）
     * @return 匹配到的适配器，若不存在返回 null
     */
    public SupplierAdapter getAdapterByName(String supplierName) {
        if (supplierName == null) return null;
        SupplierAdapter adapter = adapterByName.get(supplierName.toUpperCase());
        if (adapter == null) {
            logger.warn("未找到匹配的供应商适配器，SupplierName={}", supplierName);
        }
        return adapter;
    }

    // ==============================================  酒店｜报价｜订单 - 方法桥接委派 ==============================================


    /**
     * 获取供应商酒店房型基础信息(原文)
     * @param supplierType
     * @param hotelId
     * @param language
     * @param ext
     * @return
     */
    public Object getHotelRoomOrigContent(String supplierType, String hotelId, String language, String ext){
        SupplierAdapter adapter = getAdapterByName(supplierType);
        if (adapter == null) {
            logger.warn("[getHotelRoomOrigContent] 未找到供应商适配器: {}", supplierType);
            return Result.fail("未找到供应商适配器");
        }

        try {
            if (adapter instanceof StaticBridge bridge) {
                Object object = bridge.getHotelRoomOrigContent(supplierType, hotelId, language, ext);
                return object;
            }
            return Result.fail("适配器还未不支持 StaticBridge");
        } catch (Exception ex) {
            logger.error("[getHotelRoomOrigContent] 委派执行失败, supplierName={}", supplierType, ex);
            return Result.fail(ex.getMessage());
        }
    }




    /**
     * 单酒店报价
     */
    public Result<List<XRoom>> getPrice(XSupplierPriceRequest input) {
        SupplierAdapter adapter = getAdapterByName(input.getSupplierType());
        if (adapter == null) {
            logger.warn("[getPrice] 未找到供应商适配器: {}", input.getSupplierType());
            return Result.fail("未找到供应商适配器");
        }

        try {
            if (adapter instanceof PricingBridge bridge) {
                List<XRoom> rooms = bridge.getPrice(input);
                return Result.ok(rooms != null ? rooms : Collections.emptyList());
            }
            return Result.fail("适配器还未不支持 PricingBridge");
        } catch (Exception ex) {
            logger.error("[getPrice] 委派执行失败, supplierName={}", input.getSupplierType(), ex);
            return Result.fail(ex.getMessage());
        }
    }

    /**
     * 多酒店报价
     */
    public Result<Map<String, List<XRoom>>> getPrices(XSupplierPriceRequest input){
        SupplierAdapter adapter = getAdapterByName(input.getSupplierType());
        if (adapter == null) {
            logger.warn("[getPrices] 未找到供应商适配器: {}", input.getSupplierType());
            return Result.fail("未找到供应商适配器");
        }
        try {
            if (adapter instanceof PricingBridge bridge) {
                Map<String, List<XRoom>>  result = bridge.getPrices(input);
                return Result.ok(result != null ? result : Collections.emptyMap());
            }
            return Result.fail("适配器还未不支持 PricingBridge");
        } catch (Exception ex) {
            logger.error("[getPrices] 委派执行失败, supplierName={}", input.getSupplierType(), ex);
            return Result.fail(ex.getMessage());
        }
    }

    /**
     * 获取原始单酒店报价（供应商原始数据格式）
     */
    public Object getPriceOrig(XSupplierPriceRequest input) {
        String supplierName = input.getSupplierType();
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
            logger.warn("[getPriceOrig] 未找到供应商适配器: {}", supplierName);
            return Result.fail("未找到供应商适配器");
        }

        try {
            if (adapter instanceof PricingBridge bridge) {
                return bridge.getPriceOrig(input);
            }
            logger.warn("[getPriceOrig] 适配器不支持 PricingBridge: {}", supplierName);
            return Result.fail("适配器还未不支持 PricingBridge");
        } catch (Exception ex) {
            logger.error("[getPriceOrig] 委派执行失败, supplierName={}", supplierName, ex);
            return Result.fail(ex.getMessage());
        }
    }

    /**
     * 获取原始多酒店报价（供应商原始数据格式）
     */
    public Object getPricesOrg(XSupplierPriceRequest input) {
        String supplierName = input.getSupplierType();
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
            logger.warn("[getPricesOrg] 未找到供应商适配器: {}", supplierName);
            return Result.fail("未找到供应商适配器");
        }

        try {
            if (adapter instanceof PricingBridge bridge) {
                return bridge.getPricesOrg(input);
            }
            logger.warn("[getPricesOrg] 适配器不支持 PricingBridge: {}", supplierName);
            return Result.fail("适配器还未不支持 PricingBridge");
        } catch (Exception ex) {
            logger.error("[getPricesOrg] 委派执行失败, supplierName={}", supplierName, ex);
            return Result.fail(ex.getMessage());
        }
    }


    /**
     * 订单前置校验（标准格式）
     */
    public Result<XOrderCheckResponse> orderCheck(XSupplierCheckRequest input) {
        String supplierName = input.getSupplierType();
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
            logger.warn("[orderCheck] 未找到供应商适配器: {}", supplierName);
            return Result.fail("未找到供应商适配器");
        }
        try {
            if (adapter instanceof PricingBridge bridge) {
                XOrderCheckResponse result = bridge.orderCheck(input);
                return Result.ok(result != null ? result : null);
            }
            logger.warn("[orderCheck] 适配器不支持 PricingBridge: {}", supplierName);
            return Result.fail("适配器还未不支持 PricingBridge");
        } catch (Exception ex) {
            logger.error("[orderCheck] 委派执行失败, supplierName={}", supplierName, ex);
            return Result.fail(ex.getMessage());
        }
    }

    /**
     * 订单前置校验（供应商原始格式）
     */
    public Object orderCheckOrg(XSupplierCheckRequest input) {
        String supplierName = input.getSupplierType();
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
            logger.warn("[orderCheckOrg] 未找到供应商适配器: {}", supplierName);
            return Collections.emptyMap();
        }

        try {
            if (adapter instanceof PricingBridge bridge) {
                return bridge.orderCheckOrg(input);
            }
            logger.warn("[orderCheckOrg] 适配器不支持 PricingBridge: {}", supplierName);
            return Collections.emptyMap();
        } catch (Exception ex) {
            logger.error("[orderCheckOrg] 委派执行失败, supplierName={}", supplierName, ex);
            return ex.getMessage();
        }
    }

    /**
     * 创建订单
     */
    public Result<XCreateOrderResponse> createOrder(String supplierName, XCreateOrderRequest input) {
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
           return Result.fail("未找到供应商适配器");
        }
        try {
            if (adapter instanceof OrderBridge bridge) {
                XCreateOrderResponse resp = bridge.createOrder(input);
                return Result.ok(resp);
            }
            logger.warn("[createOrder] 适配器不支持 OrderBridge: {}", supplierName);
            return Result.fail("适配器还未不支持 OrderBridge");
        } catch (Exception ex) {
            logger.error("[createOrder] 委派执行失败, supplierName={}", supplierName, ex);
            return Result.fail(ex.getMessage());
        }
    }

    /**
     * 取消订单
     */
    public Result<XCancelOrderResponse> cancelOrder(String supplierName, XCancelOrderRequest input) {
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
            return Result.fail("未找到供应商适配器");
        }
        try {
            if (adapter instanceof OrderBridge bridge) {
                XCancelOrderResponse resp = bridge.cancelOrder(input);
                return Result.ok(resp);
            }
           logger.warn("[cancelOrder] 适配器不支持 OrderBridge: {}", supplierName);
            return Result.fail("适配器还未不支持 OrderBridge");
        } catch (Exception ex) {
            logger.error("[cancelOrder] 委派执行失败, supplierName={}", supplierName, ex);
            return Result.fail(ex.getMessage());
        }
    }

    /**
     * 查询订单
     */
    public Result<XQueryOrderResponse> queryOrder(String supplierName, String distributorOrderId, String supplierOrderId, String ext) {
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
            return Result.fail("未找到供应商适配器");
        }
        try {
            if (adapter instanceof OrderBridge bridge) {
                XQueryOrderResponse resp = bridge.queryOrder(distributorOrderId, supplierOrderId, ext);
                return Result.ok(resp);
            }
            logger.warn("[queryOrder] 适配器不支持 OrderBridge: {}", supplierName);
            return Result.fail("适配器还未不支持 OrderBridge");
        } catch (Exception ex) {
            logger.error("[queryOrder] 委派执行失败, supplierName={}", supplierName, ex);
            return Result.fail(ex.getMessage());
        }
    }

    /**
     * 检查供应商健康状态
     * @param supplierName 供应商名称
     * @return 健康状态
     */
    public Mono<Boolean> checkSupplierHealth(String supplierName) {
        SupplierAdapter adapter = findAdapterByName(supplierName);
        if (adapter == null) {
            return Mono.just(false);
        }

        return adapter.healthCheck()
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(false);
    }
    
    /**
     * 检查所有供应商健康状态
     * @return 健康状态映射
     */
    public Mono<List<SupplierHealthStatus>> checkAllSuppliersHealth() {
        return Flux.fromIterable(enabledAdapters)
                .flatMap(adapter -> 
                        adapter.healthCheck()
                                .map(healthy -> new SupplierHealthStatus(adapter.getSupplierName(), healthy))
                                .onErrorReturn(new SupplierHealthStatus(adapter.getSupplierName(), false))
                )
                .collectList();
    }
    


    /**
     * 根据供应商名称查找适配器
     * @param supplierName 供应商名称
     * @return 供应商适配器或null
     */
    private SupplierAdapter findAdapterByName(String supplierName) {
        return enabledAdapters.stream()
                .filter(adapter -> adapter.getSupplierName().equalsIgnoreCase(supplierName))
                .findFirst()
                .orElse(null);
    }
    

    /**
     * 供应商健康状态内部类
     */
    @Data
    public static class SupplierHealthStatus {
        private String supplierName;
        private boolean healthy;

        public SupplierHealthStatus(String supplierName, boolean healthy) {
            this.supplierName = supplierName;
            this.healthy = healthy;
        }
    }
}
