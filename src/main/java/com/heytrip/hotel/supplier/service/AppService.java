package com.heytrip.hotel.supplier.service;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.constant.CacheNames;
import com.heytrip.hotel.supplier.entity.App;
import com.heytrip.hotel.supplier.exception.BusinessException;
import com.heytrip.hotel.supplier.repository.AppRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 应用管理服务
 *
 * @author Pax
 */
@Service
public class AppService {

    private static final Logger logger = LoggerFactory.getLogger(AppService.class);

    @Resource
    private AppRepository appRepository;

    /**
     * 根据appId查找应用
     */
    @Cacheable(value = CacheNames.APP, key = "#appId", unless = "#result.isEmpty()")
    public Optional<App> findByAppId(String appId) {
        logger.debug("查找应用，appId: {}", appId);
        return appRepository.findByAppId(appId);
    }

    /**
     * 验证应用是否有效（存在且未过期）
     */
    public boolean validateApp(String appId) {
        Optional<App> appOpt = findByAppId(appId);
        if (appOpt.isEmpty()) {
            logger.warn("应用不存在，appId: {}", appId);
            return false;
        }

        App app = appOpt.get();
        if (app.isExpired()) {
            logger.warn("应用已过期，appId: {}, 创建时间: {}, 超时时间: {}小时",
                       appId, app.getCreateAt(), app.getTimeout());
            return false;
        }

        logger.debug("应用验证通过，appId: {}", appId);
        return true;
    }

    /**
     * 获取应用的密钥信息（用于签名验证）
     */
    @Cacheable(value = CacheNames.APP, key = "#appId + ':secret'", unless = "#result.isEmpty()")
    public Optional<String> getSecretKey(String appId) {
        return findByAppId(appId).map(App::getSecretKey);
    }

    /**
     * 创建新应用
     */
    @Transactional
    @CachePut(value = CacheNames.APP, key = "#appId")
    public App createApp(String appId, String secretKey, String encryptionKey,
                        Integer rateLimit, Integer timeout, String createBy) {
        logger.info("创建新应用，appId: {}, 创建人: {}", appId, createBy);

        // 检查appId是否已存在
        if (appRepository.existsByAppId(appId)) {
            throw new BusinessException("应用ID已存在: " + appId);
        }

        // 参数验证
        if (StrUtil.isBlank(appId) || StrUtil.isBlank(secretKey) || StrUtil.isBlank(encryptionKey)) {
            throw new BusinessException("应用ID、密钥和加密密钥不能为空");
        }

        if (rateLimit == null || rateLimit <= 0) {
            rateLimit = 1000; // 默认速率限制
        }

        if (timeout == null) {
            timeout = -1; // 默认永不过期
        }

        App app = new App(appId, secretKey, encryptionKey, rateLimit, timeout, createBy);
        App savedApp = appRepository.save(app);

        logger.info("应用创建成功，appId: {}", appId);
        return savedApp;
    }

    /**
     * 自动生成应用（为用户创建时使用）
     */
    @Transactional
    @CachePut(value = CacheNames.APP, key = "#result.appId")
    public App generateAppForUser(String userName, String createBy) {
        logger.info("为用户生成应用，用户名: {}, 创建人: {}", userName, createBy);

        // 生成唯一的appId
        String appId = generateUniqueAppId(userName);

        // 生成安全的密钥
        String secretKey = generateSecretKey();
        String encryptionKey = generateEncryptionKey();

        return createApp(appId, secretKey, encryptionKey, 1000, -1, createBy);
    }

    /**
     * 更新应用信息
     */
    @Transactional
    @CacheEvict(value = CacheNames.APP, allEntries = true)
    public App updateApp(String appId, Integer rateLimit, Integer timeout, String updateBy) {
        logger.info("更新应用信息，appId: {}, 更新人: {}", appId, updateBy);

        Optional<App> appOpt = findByAppId(appId);
        if (appOpt.isEmpty()) {
            throw new BusinessException("应用不存在: " + appId);
        }

        App app = appOpt.get();

        if (rateLimit != null && rateLimit > 0) {
            app.setRateLimit(rateLimit);
        }

        if (timeout != null) {
            app.setTimeout(timeout);
        }

        app.setUpdateBy(updateBy);
        app.setUpdateAt(LocalDateTime.now());

        App updatedApp = appRepository.save(app);
        logger.info("应用信息更新成功，appId: {}", appId);

        return updatedApp;
    }

    /**
     * 删除应用
     */
    @Transactional
    @CacheEvict(value = CacheNames.APP, allEntries = true)
    public void deleteApp(String appId, String deleteBy) {
        logger.info("删除应用，appId: {}, 删除人: {}", appId, deleteBy);

        if (!appRepository.existsByAppId(appId)) {
            throw new BusinessException("应用不存在: " + appId);
        }

        appRepository.deleteById(appId);
        logger.info("应用删除成功，appId: {}", appId);
    }

    /**
     * 获取所有活跃应用
     */
    public List<App> findAllActiveApps() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(24); // 24小时前作为过期基准
        return appRepository.findAllActiveApps(expireTime);
    }

    /**
     * 获取即将过期的应用
     */
    public List<App> findAppsExpiringWithinHours(int hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime beforeTime = now.minusHours(hours);
        return appRepository.findAppsExpiringWithinHours(now, beforeTime);
    }

    /**
     * 生成唯一的应用ID
     */
    private String generateUniqueAppId(String userName) {
        String baseAppId = "app_" + userName + "_" + System.currentTimeMillis();

        // 确保唯一性
        String appId = baseAppId;
        int counter = 1;
        while (appRepository.existsByAppId(appId)) {
            appId = baseAppId + "_" + counter;
            counter++;
        }

        return appId;
    }

    /**
     * 生成安全的密钥
     */
    private String generateSecretKey() {
        return "SK_" + UUID.randomUUID().toString().replace("-", "").toUpperCase() + "_" + System.currentTimeMillis();
    }

    /**
     * 生成加密密钥
     */
    private String generateEncryptionKey() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }
}
