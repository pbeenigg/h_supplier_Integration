package com.heytrip.hotel.supplier.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 配置Caffeine本地缓存
 * 
 * @author  Pax
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final String cities_cache = "cities"; // 城市缓存
    private static final String countries_cache = "countries"; // 国家缓存
    private static final String hotel_cache = "hotelDetails"; // 酒店详情缓存
    private static final String rooms_cache = "rooms"; // 房型缓存
    private static final String ratePlans_cache = "ratePlans"; // 价格计划缓存
    private static final String supplierConfig_cache = "supplierConfig"; // 供应商配置缓存


    
    /**
     * 配置Caffeine缓存管理器
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 配置默认缓存
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats());
        
        // 预定义缓存名称
        cacheManager.setCacheNames(
                Arrays.asList(
                        cities_cache,
                        countries_cache,
                        hotel_cache,
                        rooms_cache,
                        ratePlans_cache
                )
        );
        
        return cacheManager;
    }


    
    /**
     * 城市专用缓存
     */
    @Bean
    public Caffeine<Object, Object> citiesCacheConfig() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats();
    }

    /**
     * 国家专用缓存
     */
    @Bean
    public Caffeine<Object, Object> countriesCacheConfig() {
        return Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats();
    }
    /**
     * 酒店专用缓存
     */
    @Bean
    public Caffeine<Object, Object> hotelCacheConfig() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .recordStats();
    }

    /**
     * 房型专用缓存
     */
    @Bean
    public Caffeine<Object, Object> roomsCacheConfig() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .recordStats();
    }
    /**
     * 价格计划专用缓存
     */
    @Bean
    public Caffeine<Object, Object> ratePlansCacheConfig() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .recordStats();
    }

    
    /**
     * 供应商配置专用缓存
     */
    @Bean
    public Caffeine<Object, Object> supplierConfigCacheConfig() {
        return Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(72, TimeUnit.HOURS)
                .recordStats();
    }
    

}
