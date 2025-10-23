package com.heytrip.hotel.supplier.service;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.constant.CacheNames;
import com.heytrip.hotel.supplier.entity.App;
import com.heytrip.hotel.supplier.entity.User;
import com.heytrip.hotel.supplier.exception.BasicException;
import com.heytrip.hotel.supplier.repository.UserRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户管理服务
 *
 * @author Pax
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Resource
    private UserRepository userRepository;

    @Resource
    private AppService appService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录验证
     */
    @Cacheable(value = CacheNames.USER, key = "#userName + ':auth'", unless = "#result == null")
    public Optional<User> authenticateUser(String userName, String password) {
        logger.debug("用户登录验证，用户名: {}", userName);

        Optional<User> userOpt = userRepository.findByUserNameWithApp(userName);
        if (userOpt.isEmpty()) {
            logger.warn("用户不存在，用户名: {}", userName);
            throw  new BasicException("用户不存在: " + userName);
        }

        User user = userOpt.get();

        // 检查用户是否过期
        if (user.isExpired()) {
            logger.warn("用户已过期，用户名: {}, 创建时间: {}, 超时时间: {}小时",
                       userName, user.getCreateAt(), user.getTimeout());
           throw new BasicException("用户已过期: " + userName);
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.warn("密码验证失败，用户名: {}", userName);
            throw new BasicException("用户名或密码错误");
        }

        // 验证关联的应用是否有效
        if (!appService.validateApp(user.getAppId())) {
            logger.warn("用户关联的应用无效，用户名: {}, appId: {}", userName, user.getAppId());
          throw new BasicException("用户关联的应用无效: " + userName);
        }

        logger.info("用户登录验证成功，用户名: {}, appId: {}", userName, user.getAppId());
        return Optional.of(user);
    }

    /**
     * 根据用户名查找用户
     */
    @Cacheable(value = CacheNames.USER, key = "#userName", unless = "#result == null")
    public Optional<User> findByUserName(String userName) {
        logger.debug("查找用户，用户名: {}", userName);
        return userRepository.findByUserName(userName);
    }

    /**
     * 根据appId查找用户
     */
    @Cacheable(value = CacheNames.USER, key = "'app:' + #appId", unless = "#result == null")
    public Optional<User> findByAppId(String appId) {
        logger.debug("根据appId查找用户，appId: {}", appId);
        return userRepository.findByAppId(appId);
    }

    /**
     * 创建新用户（同时创建关联的应用）
     */
    @Transactional
    @CachePut(value = CacheNames.USER, key = "#userName")
    public User createUser(String userName, String password, String userNick,
                          String sex, Integer timeout, String createBy) {
        logger.info("创建新用户，用户名: {}, 创建人: {}", userName, createBy);

        // 参数验证
        if (StrUtil.isBlank(userName) || StrUtil.isBlank(password)) {
            throw new BasicException("用户名和密码不能为空");
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUserName(userName)) {
            throw new BasicException("用户名已存在: " + userName);
        }

        // 设置默认值
        if (StrUtil.isBlank(userNick)) {
            userNick = userName;
        }
        if (StrUtil.isBlank(sex)) {
            sex = "U"; // 默认未知
        }
        if (timeout == null) {
            timeout = -1; // 默认永不过期
        }

        // 为用户生成关联的应用
        App app = appService.generateAppForUser(userName, createBy);

        // 加密密码
        String encodedPassword = passwordEncoder.encode(password);

        // 创建用户
        User user = new User(userName, encodedPassword, userNick, sex, timeout, app.getAppId(), createBy);
        User savedUser = userRepository.save(user);

        logger.info("用户创建成功，用户名: {}, appId: {}", userName, app.getAppId());
        return savedUser;
    }

    /**
     * 更新用户信息
     */
    @Transactional
    @CacheEvict(value = CacheNames.USER, allEntries = true)
    public User updateUser(Long userId, String userNick, String sex, Integer timeout, String updateBy) {
        logger.info("更新用户信息，用户ID: {}, 更新人: {}", userId, updateBy);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new BasicException("用户不存在，ID: " + userId);
        }

        User user = userOpt.get();

        if (!StrUtil.isBlank(userNick)) {
            user.setUserNick(userNick);
        }

        if (!StrUtil.isBlank(sex)) {
            user.setSex(sex);
        }

        if (timeout != null) {
            user.setTimeout(timeout);
        }

        user.setUpdateBy(updateBy);
        user.setUpdateAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        logger.info("用户信息更新成功，用户ID: {}", userId);

        return updatedUser;
    }

    /**
     * 修改用户密码
     */
    @Transactional
    @CacheEvict(value = CacheNames.USER, allEntries = true)
    public void changePassword(Long userId, String oldPassword, String newPassword, String updateBy) {
        logger.info("修改用户密码，用户ID: {}, 更新人: {}", userId, updateBy);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new BasicException("用户不存在，ID: " + userId);
        }

        User user = userOpt.get();

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BasicException("原密码错误");
        }

        // 参数验证
        if (StrUtil.isBlank(newPassword) || newPassword.length() < 6) {
            throw new BasicException("新密码不能为空且长度不能少于6位");
        }

        // 更新密码
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
        user.setUpdateBy(updateBy);
        user.setUpdateAt(LocalDateTime.now());

        userRepository.save(user);
        logger.info("用户密码修改成功，用户ID: {}", userId);
    }


    /**
     * 重置用户密码（管理员操作）
     */
    @Transactional
    @CacheEvict(value = CacheNames.USER, allEntries = true)
    public void resetPassword(Long userId, String newPassword, String updateBy) {
        logger.info("重置用户密码，用户ID: {}, 更新人: {}", userId, updateBy);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new BasicException("用户不存在，ID: " + userId);
        }

        // 参数验证
        if (StrUtil.isBlank(newPassword) || newPassword.length() < 6) {
            throw new BasicException("新密码不能为空且长度不能少于6位");
        }

        User user = userOpt.get();
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
        user.setUpdateBy(updateBy);
        user.setUpdateAt(LocalDateTime.now());

        userRepository.save(user);
        logger.info("用户密码重置成功，用户ID: {}", userId);
    }

    /**
     * 删除用户（同时删除关联的应用）
     */
    @Transactional
    @CacheEvict(value = CacheNames.USER, allEntries = true)
    public void deleteUser(Long userId, String deleteBy) {
        logger.info("删除用户，用户ID: {}, 删除人: {}", userId, deleteBy);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new BasicException("用户不存在，ID: " + userId);
        }

        User user = userOpt.get();
        String appId = user.getAppId();

        // 删除用户
        userRepository.deleteById(userId);

        // 删除关联的应用
        try {
            appService.deleteApp(appId, deleteBy);
        } catch (Exception e) {
            logger.warn("删除用户关联的应用失败，appId: {}, 错误: {}", appId, e.getMessage());
        }

        logger.info("用户删除成功，用户ID: {}, appId: {}", userId, appId);
    }

    /**
     * 获取所有活跃用户
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }



    /**
     * 根据用户昵称搜索用户
     */
    public List<User> searchUsersByNick(String userNick) {
        logger.debug("根据昵称搜索用户，昵称: {}", userNick);
        return userRepository.findByUserNickContaining(userNick);
    }
}
