package com.heytrip.hotel.supplier.controller.system;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.dto.R;
import com.heytrip.hotel.supplier.entity.User;
import com.heytrip.hotel.supplier.service.UserService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

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

            // 构建登录成功响应
            Map<String, Object> loginResponse = new HashMap<>();
            loginResponse.put("userId", user.getUserId());
            loginResponse.put("userName", user.getUserName());
            loginResponse.put("userNick", user.getUserNick());
            loginResponse.put("appId", user.getAppId());
            loginResponse.put("loginTime", System.currentTimeMillis());

            logger.info("用户登录成功，用户名: {}, appId: {}", userName, user.getAppId());
            return R.ok("登录成功", loginResponse);

        } catch (Exception e) {
            logger.error("用户登录失败", e);
            return  R.fail("登录失败: " + e.getMessage());
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
            Long userId = Long.valueOf(changeRequest.get("userId").toString());
            String oldPassword = (String) changeRequest.get("oldPassword");
            String newPassword = (String) changeRequest.get("newPassword");
            String updateBy = (String) changeRequest.get("updateBy");

            if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
                return R.fail("原密码和新密码不能为空");
            }

            userService.changePassword(userId, oldPassword, newPassword, updateBy);

            logger.info("用户密码修改成功，用户ID: {}", userId);
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
    public R<Void> logout(@RequestBody Map<String, String> logoutRequest) {
        logger.info("用户退出登录");

        try {
            String userName = logoutRequest.get("userName");
            logger.info("用户退出登录，用户名: {}", userName);

            // 这里可以添加清理会话等逻辑
            return R.ok("退出登录成功");

        } catch (Exception e) {
            logger.error("退出登录失败", e);
            return R.fail("退出登录失败: " + e.getMessage());
        }
    }
}
