package com.heytrip.hotel.supplier.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.heytrip.hotel.supplier.constant.StaticCacheNames;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 缓存配置（Caffeine版）
 * 说明：
 * - 为不同的 cacheNames 配置不同的 TTL 与最大容量
 * - 作为默认 CacheManager 提供，现阶段所有 @Cacheable 将使用本配置
 * - 如后续引入 Redis 作为二级缓存，可新增 RedisCacheManager，并在注解上通过 cacheManager 精准指定
 */
@Configuration
public class CacheConfig {

    @Bean
    public SimpleCacheManager cacheManager() {
        List<Cache> caches = new ArrayList<>();

        // 国家：读多写少，更新极低频
        caches.add(new CaffeineCache(
                StaticCacheNames.COUNTRY,
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(Duration.ofDays(20))
                        .recordStats()
                        .build()
        ));

        // 城市：读多写少
        caches.add(new CaffeineCache(
                StaticCacheNames.CITY,
                Caffeine.newBuilder()
                        .maximumSize(20_000)
                        .expireAfterWrite(Duration.ofDays(20))
                        .recordStats()
                        .build()
        ));

        // 酒店：更新频率较低，数据量较大
        caches.add(new CaffeineCache(
                StaticCacheNames.HOTEL,
                Caffeine.newBuilder()
                        .maximumSize(50_000)
                        .expireAfterWrite(Duration.ofDays(20))
                        .recordStats()
                        .build()
        ));

        // 房型：相对变动频繁，热点较明显（先占位，后续有真实返回再调优）
        caches.add(new CaffeineCache(
                StaticCacheNames.ROOM,
                Caffeine.newBuilder()
                        .maximumSize(50_000)
                        .expireAfterWrite(Duration.ofDays(20))
                        .recordStats()
                        .build()
        ));

        // 价计划：与房型类似（先占位，后续再调优）
        caches.add(new CaffeineCache(
                StaticCacheNames.RATE_PLAN,
                Caffeine.newBuilder()
                        .maximumSize(100_000)
                        .expireAfterWrite(Duration.ofDays(20))
                        .recordStats()
                        .build()
        ));

        // 国籍：极低变动
        caches.add(new CaffeineCache(
                StaticCacheNames.NATIONALITY,
                Caffeine.newBuilder()
                        .maximumSize(2_000)
                        .expireAfterWrite(Duration.ofDays(20))
                        .recordStats()
                        .build()
        ));

        // GIATA 映射：数据量较大，低频更新
        caches.add(new CaffeineCache(
                StaticCacheNames.GIATA,
                Caffeine.newBuilder()
                        .maximumSize(100_000)
                        .expireAfterWrite(Duration.ofDays(20))
                        .recordStats()
                        .build()
        ));

        // 供应商配置缓存：读多写少，变更需要短时间生效
        caches.add(new CaffeineCache(
                StaticCacheNames.SUPPLIER_CONFIG_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(Duration.ofDays(30))
                        .recordStats()
                        .build()
        ));

        // 供应商认证配置缓存：安全敏感，TTL 较短
        caches.add(new CaffeineCache(
                StaticCacheNames.SUPPLIER_AUTH_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(Duration.ofDays(30))
                        .recordStats()
                        .build()
        ));

        // 供应商FTP配置缓存：中等变更频率
        caches.add(new CaffeineCache(
                StaticCacheNames.SUPPLIER_AUTH_FTP,
                Caffeine.newBuilder()
                        .maximumSize(2_000)
                        .expireAfterWrite(Duration.ofDays(30))
                        .recordStats()
                        .build()
        ));

        // 系统配置缓存：读多写少
        caches.add(new CaffeineCache(
                StaticCacheNames.SYSTEM_CONFIG,
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(Duration.ofDays(30))
                        .recordStats()
                        .build()
        ));



        SimpleCacheManager mgr = new SimpleCacheManager();
        mgr.setCaches(caches);
        return mgr;
    }
}
