package com.heytrip.hotel.supplier.adapter;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

import com.heytrip.common.request.XSupplierPriceRequest;
import com.heytrip.common.request.XCreateOrderRequest;
import com.heytrip.common.request.XCancelOrderRequest;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.XCreateOrderResponse;
import com.heytrip.common.response.other.XCancelOrderResponse;
import com.heytrip.common.response.other.XQueryOrderResponse;
import com.heytrip.common.result.Result;
import com.heytrip.hotel.supplier.adapter.capability.PricingBridge;
import com.heytrip.hotel.supplier.adapter.capability.OrderBridge;

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

    // ======================= 报价与订单委派（第2阶段骨架） =======================

    /**
     * 单酒店报价
     */
    public Result<List<XRoom>> getPrice(String supplierName, XSupplierPriceRequest input) {
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
            return Result.ok(Collections.emptyList());
        }

        try {
            if (adapter instanceof PricingBridge bridge) {
                List<XRoom> rooms = bridge.getPrice(input);
                return Result.ok(rooms != null ? rooms : Collections.emptyList());
            }
            return Result.ok(Collections.emptyList());
        } catch (Exception ex) {
            logger.error("[getPrice] 委派执行失败, supplierName={}", supplierName, ex);
            return Result.ok(Collections.emptyList());
        }
    }

    /**
     * 创建订单
     */
    public Result<XCreateOrderResponse> createOrder(String supplierName, XCreateOrderRequest input) {
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
            return Result.ok(null);
        }
        try {
            if (adapter instanceof OrderBridge bridge) {
                XCreateOrderResponse resp = bridge.createOrder(input);
                return Result.ok(resp);
            }
            return Result.ok(null);
        } catch (Exception ex) {
            logger.error("[createOrder] 委派执行失败, supplierName={}", supplierName, ex);
            return Result.ok(null);
        }
    }

    /**
     * 取消订单
     */
    public Result<XCancelOrderResponse> cancelOrder(String supplierName, XCancelOrderRequest input) {
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
            return Result.ok(null);
        }
        try {
            if (adapter instanceof OrderBridge bridge) {
                XCancelOrderResponse resp = bridge.cancelOrder(input);
                return Result.ok(resp);
            }
            return Result.ok(null);
        } catch (Exception ex) {
            logger.error("[cancelOrder] 委派执行失败, supplierName={}", supplierName, ex);
            return Result.ok(null);
        }
    }

    /**
     * 查询订单
     */
    public Result<XQueryOrderResponse> queryOrder(String supplierName, String distributorOrderId, String supplierOrderId, String ext) {
        SupplierAdapter adapter = getAdapterByName(supplierName);
        if (adapter == null) {
            return Result.ok(null);
        }
        try {
            if (adapter instanceof OrderBridge bridge) {
                XQueryOrderResponse resp = bridge.queryOrder(distributorOrderId, supplierOrderId, ext);
                return Result.ok(resp);
            }
            return Result.ok(null);
        } catch (Exception ex) {
            logger.error("[queryOrder] 委派执行失败, supplierName={}", supplierName, ex);
            return Result.ok(null);
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
