package com.heytrip.hotel.supplier.controller.system;

import com.heytrip.hotel.supplier.config.CacheEvictor;
import com.heytrip.hotel.supplier.constant.StaticCacheNames;
import com.heytrip.hotel.supplier.dto.R;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cache
 * 缓存管理模块
 * 功能：
 * 1) 触发缓存清理（静态缓存、按供应商、按缓存名、按键）
 * 2) 缓存统计（Caffeine 统计信息）
 * 3) 缓存查询（列出键、读取单个键值）
 * <p>
 * 审计：通过请求头 X-Operator 记录操作人，落日志。
 */
@RestController
@RequestMapping(value = "/cache")
public class CacheController {

    private static final Logger log = LoggerFactory.getLogger(CacheController.class);

    @Resource
    private CacheManager cacheManager;

    @Resource
    private CacheEvictor cacheEvictor;



    /**
     * 全量清理与静态数据相关的全部缓存
     * 说明：由于 key 中包含 supplierId 与 supplierCode，最简单可靠的是清空整个 cache
     */
    @PostMapping("/evict/static/all")
    public R<Void> evictAllStatic(@RequestHeader(name = "X-Operator", required = false) String operator) {
        cacheEvictor.evictAllStaticCaches();
        log.info("[CacheAdmin] 全量清理静态缓存 by {}", op(operator));
        return R.ok("全量清理静态缓存成功");
    }

    /**
     * 按供应商清理静态缓存
     * 说明：由于 key 中包含 supplierId 与 supplierCode，最简单可靠的是清空整个 cache
     */
    @PostMapping("/evict/static/by-supplier")
    public R<Void> evictStaticBySupplier(@RequestHeader(name = "X-Operator", required = false) String operator,
                                         @RequestParam Long supplierId,
                                         @RequestParam String supplierCode) {
        cacheEvictor.evictStaticBySupplier(supplierId, supplierCode);
        log.info("[CacheAdmin] 按供应商清理静态缓存 supplierId={}, supplierCode={} by {}", supplierId, supplierCode, op(operator));
        return R.ok("按供应商清理静态缓存成功");
    }

    /**
     * 按缓存名清理整个缓存
     * 说明：由于我们使用的 key 都是简单字符串，因此直接传递即可
     */
    @PostMapping("/evict/by-name")
    public R<Void> evictByName(@RequestHeader(name = "X-Operator", required = false) String operator,
                               @RequestParam String cacheName) {
        cacheEvictor.evictByCacheName(cacheName);
        log.info("[CacheAdmin] 清空缓存 cacheName={} by {}", cacheName, op(operator));
        return R.ok("清空缓存成功");
    }

    /**
     * 按缓存名与键清理缓存项
     * 说明：由于我们使用的 key 都是简单字符串，因此直接传递即可
     */
    @PostMapping("/evict/by-key")
    public R<Void> evictByKey(@RequestHeader(name = "X-Operator", required = false) String operator,
                              @RequestParam String cacheName,
                              @RequestParam String key) {
        cacheEvictor.evictByCacheNameAndKey(cacheName, key);
        log.info("[CacheAdmin] 清理缓存项 cacheName={}, key={} by {}", cacheName, key, op(operator));
        return R.ok("清理缓存项成功");
    }

    /**
     * 清理供应商配置类缓存
     */
    @PostMapping("/evict/supplier-config/all")
    public R<Void> evictSupplierConfig(@RequestHeader(name = "X-Operator", required = false) String operator) {
        cacheEvictor.evictSupplierConfigCaches();
        log.info("[CacheAdmin] 清理供应商配置类缓存 by {}", op(operator));
        return R.ok("清理供应商配置类缓存成功");
    }

    /**
     * 清理系统配置缓存
     */
    @PostMapping("/evict/system-config")
    public R<Void> evictSystemConfig(@RequestHeader(name = "X-Operator", required = false) String operator) {
        cacheEvictor.evictSystemConfig();
        log.info("[CacheAdmin] 清理系统配置缓存 by {}", op(operator));
        return R.ok("清理系统配置缓存成功");
    }

    /**
     * 列出缓存统计（仅支持 CaffeineCache）
     * 说明：
     * - 仅列出我们已知的缓存名称，防止意外暴露其它缓存
     * - 对于非 CaffeineCache 的实现，仅返回类型信息
     * @return
     */
    @GetMapping("/stats")
    public R<List<Map<String, Object>>> stats() {
        List<Map<String, Object>> list = new ArrayList<>();
        Collection<String> names = knownCacheNames();
        for (String name : names) {
            Cache cache = cacheManager.getCache(name);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            if (cache instanceof CaffeineCache caffeineCache) {
                var stats = caffeineCache.getNativeCache().stats();
                row.put("estimatedSize", caffeineCache.getNativeCache().estimatedSize());
                row.put("hitCount", stats.hitCount());
                row.put("missCount", stats.missCount());
                row.put("hitRate", stats.hitRate());
                row.put("evictionCount", stats.evictionCount());
                row.put("loadCount", stats.loadCount());
            } else {
                row.put("type", cache != null ? cache.getClass().getSimpleName() : "null");
            }
            list.add(row);
        }
        return R.ok("缓存统计查询成功", list);
    }

    /**
     * 列出缓存中的 Key（仅支持 CaffeineCache）
     * 说明：
     * - 仅返回前 1000 个 Key，防止数据量过大
     * - 可选按前缀过滤
     * - 仅支持 CaffeineCache，其他实现将返回不支持的错误
     * @param cacheName
     * @param prefix
     * @return
     */
    @GetMapping("/keys")
    public R<List<String>> listKeys(@RequestParam String cacheName,
                                    @RequestParam(required = false) String prefix) {
        Cache cache = cacheManager.getCache(cacheName);
        if (!(cache instanceof CaffeineCache caffeineCache)) {
            return R.fail("当前缓存实现不支持列出Key");
        }
        var map = caffeineCache.getNativeCache().asMap();
        List<String> keys = map.keySet().stream()
                .map(k -> Objects.toString(k, ""))
                .filter(k -> !StringUtils.hasText(prefix) || k.startsWith(prefix))
                .sorted()
                .limit(1000)
                .collect(Collectors.toList());
        return R.ok("缓存Key列表查询成功", keys);
    }


    /**
     * 读取缓存项
     * @param cacheName
     * @param key
     * @return
     */
    @GetMapping("/get")
    public R<Object> getValue(@RequestParam String cacheName,
                              @RequestParam String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return R.fail("缓存不存在");
        }
        Cache.ValueWrapper wrapper = cache.get(key);
        Object value = wrapper != null ? wrapper.get() : null;
        return R.ok("缓存值获取成功", value);
    }

    // ============== 辅助工具 ==============
    private Collection<String> knownCacheNames() {
        // 如果 CacheManager 支持 cacheNames 列举，则直接返回；否则回退为我们维护的集合
        Collection<String> names = cacheManager.getCacheNames();
        if (names != null && !names.isEmpty()) {
            return names;
        }
        return Arrays.asList(
                StaticCacheNames.COUNTRY,
                StaticCacheNames.CITY,
                StaticCacheNames.HOTEL,
                StaticCacheNames.ROOM,
                StaticCacheNames.RATE_PLAN,
                StaticCacheNames.NATIONALITY,
                StaticCacheNames.GIATA,
                StaticCacheNames.SUPPLIER_CONFIG_CACHE,
                StaticCacheNames.SUPPLIER_AUTH_CACHE,
                StaticCacheNames.SUPPLIER_AUTH_FTP,
                StaticCacheNames.SYSTEM_CONFIG
        );
    }

    private String op(String operator) {
        return StringUtils.hasText(operator) ? operator : "anonymous";
    }
}
