package com.heytrip.hotel.supplier.config;

import com.heytrip.hotel.supplier.constant.StaticCacheNames;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 缓存清理工具：用于静态数据同步完成后清理本地缓存，避免陈旧数据
 */
@Component
public class CacheEvictor {

    private static final Logger logger = LoggerFactory.getLogger(CacheEvictor.class);

    /**
     * 与静态数据相关的全部缓存名称
     * 说明：与 StaticDataQueryServiceImpl @Cacheable 的 cacheNames 保持一致
     *
     */
    private static final List<String> STATIC_CACHE_NAMES = Arrays.asList(
            StaticCacheNames.NATIONALITY,
            StaticCacheNames.COUNTRY,
            StaticCacheNames.CITY,
            StaticCacheNames.HOTEL,
            StaticCacheNames.GIATA,
            StaticCacheNames.ROOM,
            StaticCacheNames.RATE_PLAN
    );

    @Resource
    private CacheManager cacheManager;

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
}
