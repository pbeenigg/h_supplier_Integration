package com.heytrip.hotel.supplier.controller.system;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.dto.R;
import com.heytrip.hotel.supplier.entity.User;
import com.heytrip.hotel.supplier.service.UserService;
import com.heytrip.hotel.supplier.utils.AuthHelper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户认证和管理控制器
 *
 * @author Pax
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Resource
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        logger.info("用户登录请求");

        try {
            String userName = loginRequest.get("userName");
            String password = loginRequest.get("password");

            if (StrUtil.isBlank(userName) || StrUtil.isBlank(password)) {
                return R.fail("用户名和密码不能为空");
            }

            Optional<User> userOpt = userService.authenticateUser(userName, password);
            if (userOpt.isEmpty()) {
                return R.fail("用户名或密码错误");
            }

            User user = userOpt.get();

            // 设置用户信息到Spring Security Context
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getUserName(), null, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 设置用户信息到Request属性，供AuthHelper使用
            AuthHelper.setCurrentUser(user.getUserName());

            // 构建登录成功响应
            Map<String, Object> loginResponse = new HashMap<>();
            loginResponse.put("userId", user.getUserId());
            loginResponse.put("userName", user.getUserName());
            loginResponse.put("userNick", user.getUserNick());
            loginResponse.put("appId", user.getAppId());
            loginResponse.put("secretKey", user.getApp().getSecretKey());
            loginResponse.put("loginTime", System.currentTimeMillis());
            loginResponse.put("timeout", user.getTimeout());

            logger.info("用户登录成功，用户名: {}, appId: {}", userName, user.getAppId());
            return R.ok("登录成功", loginResponse);

        } catch (Exception e) {
            logger.error("用户登录失败", e);
            return R.fail("登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/user/info")
    public R<Map<String, Object>> getUserInfo(@RequestParam String userName) {
        logger.debug("获取用户信息，用户名: {}", userName);

        try {
            Optional<User> userOpt = userService.findByUserName(userName);
            if (userOpt.isEmpty()) {
                return R.fail("用户不存在");
            }

            User user = userOpt.get();

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("userName", user.getUserName());
            userInfo.put("userNick", user.getUserNick());
            userInfo.put("sex", user.getSex());
            userInfo.put("timeout", user.getTimeout());
            userInfo.put("appId", user.getAppId());
            userInfo.put("createAt", user.getCreateAt());
            userInfo.put("updateAt", user.getUpdateAt());

            return R.ok("查询成功", userInfo);

        } catch (Exception e) {
            logger.error("获取用户信息失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/user/change-password")
    public R<Void> changePassword(@RequestBody Map<String, Object> changeRequest) {
        logger.info("用户修改密码请求");

        try {
            // 从当前认证信息获取用户ID，如果传入userId则验证权限
            String currentUser = AuthHelper.getCurrentUser();
            Long userId = changeRequest.containsKey("userId") ?
                Long.valueOf(changeRequest.get("userId").toString()) : null;

            String oldPassword = (String) changeRequest.get("oldPassword");
            String newPassword = (String) changeRequest.get("newPassword");

            if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
                return R.fail("原密码和新密码不能为空");
            }

            // 密码强度验证
            if (newPassword.length() < 6) {
                return R.fail("新密码长度不能少于6位");
            }

            // 如果没有传入userId，则通过当前用户名获取
            if (userId == null) {
                Optional<User> currentUserOpt = userService.findByUserName(currentUser);
                if (currentUserOpt.isEmpty()) {
                    return R.fail("当前用户不存在");
                }
                userId = currentUserOpt.get().getUserId();
            }

            // 执行密码修改
            userService.changePassword(userId, oldPassword, newPassword, currentUser);

            logger.info("用户密码修改成功，用户ID: {}, 操作用户: {}", userId, currentUser);
            return R.ok("密码修改成功");

        } catch (Exception e) {
            logger.error("密码修改失败", e);
            return R.fail("密码修改失败: " + e.getMessage());
        }
    }

    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestBody(required = false) Map<String, String> logoutRequest) {
        logger.info("用户退出登录");

        try {
            // 获取当前登录用户
            String currentUser = AuthHelper.getCurrentUser();

            // 如果请求中包含用户名，验证是否与当前用户一致
            if (logoutRequest != null && logoutRequest.containsKey("userName")) {
                String requestUserName = logoutRequest.get("userName");
                if (!currentUser.equals(requestUserName) && !"system".equals(currentUser)) {
                    logger.warn("退出登录用户名不匹配，当前用户: {}, 请求用户: {}", currentUser, requestUserName);
                    return R.fail("用户验证失败");
                }
            }

            // 清理Spring Security Context
            SecurityContextHolder.clearContext();

            // 记录退出日志
            logger.info("用户退出登录成功，用户名: {}", currentUser);

            // 这里可以添加更多清理逻辑：
            // 1. 清理用户相关缓存
            // 2. 记录退出日志到数据库
            // 3. 清理用户Session相关信息
            // 4. 通知其他服务用户已退出

            return R.ok("退出登录成功");

        } catch (Exception e) {
            logger.error("退出登录失败", e);
            return R.fail("退出登录失败: " + e.getMessage());
        }
    }
}
