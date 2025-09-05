package com.heytrip.hotel.supplier.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;

/**
 * 缓存服务
 * 提供统一的缓存操作接口
 * 
 * @author  Pax
 */
@Service
public class CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    @Autowired
    private CacheManager cacheManager;
    
    /**
     * 获取缓存值
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param type 值类型
     * @return 缓存值
     */
    public <T> T get(String cacheName, String key, Class<T> type) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null) {
                    Object value = wrapper.get();
                    if (type.isInstance(value)) {
                        logger.debug("Cache hit for cache: {}, key: {}", cacheName, key);
                        return type.cast(value);
                    }
                }
            }
            logger.debug("Cache miss for cache: {}, key: {}", cacheName, key);
            return null;
        } catch (Exception e) {
            logger.warn("Failed to get cache value for cache: {}, key: {}", cacheName, key, e);
            return null;
        }
    }
    
    /**
     * 设置缓存值
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param value 缓存值
     */
    public void put(String cacheName, String key, Object value) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(key, value);
                logger.debug("Cache put for cache: {}, key: {}", cacheName, key);
            }
        } catch (Exception e) {
            logger.warn("Failed to put cache value for cache: {}, key: {}", cacheName, key, e);
        }
    }
    
    /**
     * 获取缓存值，如果不存在则执行callable并缓存结果
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param callable 获取值的方法
     * @param type 值类型
     * @return 缓存值
     */
    public <T> T get(String cacheName, String key, Callable<T> callable, Class<T> type) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                T value = cache.get(key, callable);
                logger.debug("Cache get with callable for cache: {}, key: {}", cacheName, key);
                return value;
            } else {
                return callable.call();
            }
        } catch (Exception e) {
            logger.warn("Failed to get cache value with callable for cache: {}, key: {}", cacheName, key, e);
            try {
                return callable.call();
            } catch (Exception ex) {
                logger.error("Failed to execute callable for cache: {}, key: {}", cacheName, key, ex);
                return null;
            }
        }
    }
    
    /**
     * 删除缓存值
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    public void evict(String cacheName, String key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                logger.debug("Cache evict for cache: {}, key: {}", cacheName, key);
            }
        } catch (Exception e) {
            logger.warn("Failed to evict cache value for cache: {}, key: {}", cacheName, key, e);
        }
    }
    
    /**
     * 清空指定缓存
     * @param cacheName 缓存名称
     */
    public void clear(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.info("Cache cleared for cache: {}", cacheName);
            }
        } catch (Exception e) {
            logger.warn("Failed to clear cache: {}", cacheName, e);
        }
    }
    
    /**
     * 检查缓存是否存在指定键
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean exists(String cacheName, String key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                return wrapper != null;
            }
            return false;
        } catch (Exception e) {
            logger.warn("Failed to check cache existence for cache: {}, key: {}", cacheName, key, e);
            return false;
        }
    }
}
