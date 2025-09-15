package com.heytrip.hotel.supplier.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.heytrip.hotel.supplier.constant.StaticCacheNames;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置（Caffeine）
 */
@Configuration
@EnableCaching
public class CacheConfig {


    @Bean
    public Caffeine<Object, Object> caffeineSpec() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(7, TimeUnit.DAYS);
    }


    
    /**
     * 配置Caffeine缓存管理器
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 配置默认缓存
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(7, TimeUnit.DAYS)
                .recordStats());
        
        // 预定义缓存名称
        cacheManager.setCacheNames(
                Arrays.asList(
                        StaticCacheNames.SUPPLIER_CONFIG_CACHE,
                        StaticCacheNames.SUPPLIER_AUTH_CACHE,
                        StaticCacheNames.SUPPLIER_AUTH_CACHE,
                        StaticCacheNames.SYSTEM_CONFIG
                )
        );
        
        return cacheManager;
    }

    

}
