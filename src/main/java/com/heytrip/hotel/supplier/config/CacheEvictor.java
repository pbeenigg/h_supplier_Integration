package com.heytrip.hotel.supplier.config;

import com.heytrip.hotel.supplier.constant.CacheNames;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 缓存清理工具：用于静态数据同步完成后清理本地缓存，避免陈旧数据
 */
@Component
public class CacheEvictor {

    private static final Logger logger = LoggerFactory.getLogger(CacheEvictor.class);

    @Resource
    private CacheManager cacheManager;


    /**
     * 与静态数据相关的全部缓存名称
     * 说明：与 StaticDataQueryServiceImpl @Cacheable 的 cacheNames 保持一致
     *
     */
    private static final List<String> STATIC_CACHE_NAMES = Arrays.asList(
            CacheNames.NATIONALITY,
            CacheNames.COUNTRY,
            CacheNames.CITY,
            CacheNames.HOTEL,
            CacheNames.GIATA,
            CacheNames.ROOM,
            CacheNames.RATE_PLAN
    );

    /**
     * 供应商配置类缓存
     */
    private static final List<String> SUPPLIER_CFG_CACHES = Arrays.asList(
            CacheNames.SUPPLIER_CONFIG_CACHE,
            CacheNames.SUPPLIER_AUTH_CACHE,
            CacheNames.SUPPLIER_AUTH_FTP
    );



    /**
     * 清理与静态数据相关的全部缓存
     * 说明：由于 key 中包含 supplierId 与 supplierCode，最简单可靠的是清空整个 cache
     */
    public void evictAllStaticCaches() {
        for (String name : STATIC_CACHE_NAMES) {
            try {
                Cache cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            } catch (Exception e) {
                logger.warn("清理缓存失败：{} -> {}", name, e.getMessage());
            }
        }
        logger.info("已清理静态数据相关缓存：{}", STATIC_CACHE_NAMES);
    }

    /**
     * 通用：按缓存名清空
     */
    public void evictByCacheName(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            logger.info("已清空缓存: {}", cacheName);
        }
    }

    /**
     * 通用：按缓存名+Key 清理
     */
    public void evictByCacheNameAndKey(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(key);
            logger.info("已清理缓存: {} key={}", cacheName, key);
        }
    }

    /**
     * 按供应商维度清理静态缓存（根据我们统一的 key 前缀：supplierId:supplierCode）
     * 说明：仅适用于 CaffeineCache。对于其它实现将降级为整表清理。
     */
    public void evictStaticBySupplier(Long supplierId, String supplierCode) {
        String prefix = supplierId + ":" + supplierCode;
        for (String name : STATIC_CACHE_NAMES) {
            Cache cache = cacheManager.getCache(name);
            if (cache instanceof CaffeineCache caffeineCache) {
                try {
                    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                    // 移除以前缀开头的 Key（支持我们 list/page/one 的统一 key 规则）
                    nativeCache.asMap().keySet().removeIf(k -> Objects.toString(k, "").startsWith(prefix) || Objects.toString(k, "").contains("'ONE':" + prefix));
                    logger.info("已按供应商清理缓存: {} prefix={}", name, prefix);
                } catch (Exception ex) {
                    logger.warn("按前缀清理失败，降级为整表清空: {} -> {}", name, ex.getMessage());
                    cache.clear();
                }
            } else if (cache != null) {
                // 非 CaffeineCache 无法安全遍历 Key，整表清空
                cache.clear();
                logger.info("缓存实现不支持按前缀，已整表清空: {}", name);
            }
        }
    }

    /**
     * 清理供应商配置类缓存（config/auth/ftp）
     * 如果调用方可构造出具体 key，也可以改用 evictByCacheNameAndKey
     */
    public void evictSupplierConfigCaches() {
        for (String name : SUPPLIER_CFG_CACHES) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
                logger.info("已清理供应商配置缓存: {}", name);
            }
        }
    }

    /**
     * 清理系统配置缓存
     */
    public void evictSystemConfig() {
        Cache cache = cacheManager.getCache(CacheNames.SYSTEM_CONFIG);
        if (cache != null) {
            cache.clear();
            logger.info("已清理系统配置缓存: {}", CacheNames.SYSTEM_CONFIG);
        }
    }
}
