package com.heytrip.hotel.supplier.service;

import com.heytrip.hotel.supplier.entity.App;
import com.heytrip.hotel.supplier.entity.User;
import com.heytrip.hotel.supplier.repository.AppRepository;
import com.heytrip.hotel.supplier.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据初始化服务
 * 负责在系统启动时初始化预设的应用和用户数据
 *
 * @author Pax
 */
@Service
public class DataInitService {

    private static final Logger logger = LoggerFactory.getLogger(DataInitService.class);

    @Resource
    private AppRepository appRepository;

    @Resource
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 系统启动时初始化预设数据
     */
    @PostConstruct
    @Transactional
    public void initializeData() {
        logger.info("开始初始化系统预设数据");

        try {
            // 初始化默认应用
            initializeDefaultApp();

            // 初始化超级管理员
            initializeSuperAdmin();

            logger.info("系统预设数据初始化完成");

        } catch (Exception e) {
            logger.error("系统预设数据初始化失败", e);
            throw new RuntimeException("系统预设数据初始化失败", e);
        }
    }

    /**
     * 初始化默认应用
     */
    private void initializeDefaultApp() {
        String defaultAppId = "heytrip_supplier_integration_pax";

        if (!appRepository.existsByAppId(defaultAppId)) {
            logger.info("创建默认应用，appId: {}", defaultAppId);

            App defaultApp = new App(
                defaultAppId,
                "HeyTrip@Pax#SupplierIntegration!2025",
                "427ae41e4649b934ca495991b7852b855",
                1000,
                720,
                "admin"
            );

            appRepository.save(defaultApp);
            logger.info("默认应用创建成功，appId: {}", defaultAppId);
        } else {
            logger.info("默认应用已存在，跳过创建，appId: {}", defaultAppId);
        }
    }

    /**
     * 初始化超级管理员
     */
    private void initializeSuperAdmin() {
        String adminUserName = "admin";

        if (!userRepository.existsByUserName(adminUserName)) {
            logger.info("创建超级管理员用户，用户名: {}", adminUserName);

            // 密码: admin123
            String encodedPassword = passwordEncoder.encode("admin123");

            User adminUser = new User(
                adminUserName,
                encodedPassword,
                "超级管理员",
                "U",
                -1,
                "heytrip_supplier_integration_pax",
                "admin"
            );

            // 手动设置用户ID为1
            adminUser.setUserId(1L);

            userRepository.save(adminUser);
            logger.info("超级管理员用户创建成功，用户名: {}", adminUserName);
        } else {
            logger.info("超级管理员用户已存在，跳过创建，用户名: {}", adminUserName);
        }
    }

    /**
     * 验证预设数据的完整性
     */
    public boolean validatePresetData() {
        logger.debug("验证预设数据完整性");

        try {
            // 验证默认应用
            boolean appExists = appRepository.existsByAppId("heytrip_supplier_integration_pax");
            if (!appExists) {
                logger.error("默认应用不存在");
                return false;
            }

            // 验证超级管理员
            boolean adminExists = userRepository.existsByUserName("admin");
            if (!adminExists) {
                logger.error("超级管理员用户不存在");
                return false;
            }

            logger.debug("预设数据验证通过");
            return true;

        } catch (Exception e) {
            logger.error("预设数据验证失败", e);
            return false;
        }
    }
}
