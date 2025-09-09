package com.heytrip.hotel.supplier.adapter;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    
    @PostConstruct
    public void initialize() {
        // 过滤启用的适配器并按优先级排序
        enabledAdapters = supplierAdapters.stream()
                .filter(SupplierAdapter::isEnabled)
                .sorted(Comparator.comparingInt(SupplierAdapter::getPriority))
                .collect(Collectors.toList());
        
        logger.info("Initialized {} enabled supplier adapters", enabledAdapters.size());
        enabledAdapters.forEach(adapter -> 
                logger.info("Enabled adapter: {} with priority: {}", 
                        adapter.getSupplierName(), adapter.getPriority()));
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
    public static class SupplierHealthStatus {
        private String supplierName;
        private boolean healthy;
        
        public SupplierHealthStatus(String supplierName, boolean healthy) {
            this.supplierName = supplierName;
            this.healthy = healthy;
        }
        
        public String getSupplierName() {
            return supplierName;
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        @Override
        public String toString() {
            return "SupplierHealthStatus{" +
                    "supplierName='" + supplierName + '\'' +
                    ", healthy=" + healthy +
                    '}';
        }
    }
}
