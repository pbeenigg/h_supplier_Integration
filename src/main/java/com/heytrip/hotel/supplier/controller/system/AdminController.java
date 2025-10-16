package com.heytrip.hotel.supplier.controller.system;

import com.heytrip.hotel.supplier.dto.R;
import com.heytrip.hotel.supplier.entity.App;
import com.heytrip.hotel.supplier.entity.User;
import com.heytrip.hotel.supplier.service.AppService;
import com.heytrip.hotel.supplier.service.UserService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理员用户和应用管理控制器
 *
 * @author Pax
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    // ==================== 用户管理 ====================

    /**
     * 创建新用户
     */
    @PostMapping("/user/create")
    public R<Map<String, Object>> createUser(@RequestBody Map<String, Object> createRequest) {
        logger.info("管理员创建用户请求");

        try {
            String userName = (String) createRequest.get("userName");
            String password = (String) createRequest.get("password");
            String userNick = (String) createRequest.get("userNick");
            String sex = (String) createRequest.get("sex");
            Integer timeout = createRequest.get("timeout") != null ?
                            Integer.valueOf(createRequest.get("timeout").toString()) : -1;
            String createBy = (String) createRequest.get("createBy");

            User user = userService.createUser(userName, password, userNick, sex, timeout, createBy);

            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("userId", user.getUserId());
            userResponse.put("userName", user.getUserName());
            userResponse.put("userNick", user.getUserNick());
            userResponse.put("appId", user.getAppId());
            userResponse.put("createAt", user.getCreateAt());

            logger.info("用户创建成功，用户名: {}, appId: {}", userName, user.getAppId());
            return R.ok("用户创建成功", userResponse);

        } catch (Exception e) {
            logger.error("用户创建失败", e);
            return R.fail("用户创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/user/update")
    public R<Map<String, Object>> updateUser(@RequestBody Map<String, Object> updateRequest) {
        logger.info("管理员更新用户请求");

        try {
            Long userId = Long.valueOf(updateRequest.get("userId").toString());
            String userNick = (String) updateRequest.get("userNick");
            String sex = (String) updateRequest.get("sex");
            Integer timeout = updateRequest.get("timeout") != null ?
                            Integer.valueOf(updateRequest.get("timeout").toString()) : null;
            String updateBy = (String) updateRequest.get("updateBy");

            User user = userService.updateUser(userId, userNick, sex, timeout, updateBy);

            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("userId", user.getUserId());
            userResponse.put("userName", user.getUserName());
            userResponse.put("userNick", user.getUserNick());
            userResponse.put("sex", user.getSex());
            userResponse.put("timeout", user.getTimeout());
            userResponse.put("updateAt", user.getUpdateAt());

            logger.info("用户更新成功，用户ID: {}", userId);
            return R.ok("用户更新成功", userResponse);

        } catch (Exception e) {
            logger.error("用户更新失败", e);
            return R.fail("用户更新失败: " + e.getMessage());
        }
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/user/reset-password")
    public R<Void> resetPassword(@RequestBody Map<String, Object> resetRequest) {
        logger.info("管理员重置密码请求");

        try {
            Long userId = Long.valueOf(resetRequest.get("userId").toString());
            String newPassword = (String) resetRequest.get("newPassword");
            String updateBy = (String) resetRequest.get("updateBy");

            userService.resetPassword(userId, newPassword, updateBy);

            logger.info("密码重置成功，用户ID: {}", userId);
            return R.ok("密码重置成功");

        } catch (Exception e) {
            logger.error("密码重置失败", e);
            return R.fail("密码重置失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/user/delete/{userId}")
    public R<Void> deleteUser(@PathVariable Long userId, @RequestParam String deleteBy) {
        logger.info("管理员删除用户请求，用户ID: {}", userId);

        try {
            userService.deleteUser(userId, deleteBy);

            logger.info("用户删除成功，用户ID: {}", userId);
            return R.ok("用户删除成功");

        } catch (Exception e) {
            logger.error("用户删除失败", e);
            return R.fail("用户删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有活跃用户
     */
    @GetMapping("/user/list")
    public R<List<User>> getUserList() {
        logger.debug("获取用户列表请求");

        try {
            List<User> users = userService.findAllActiveUsers();
            return R.ok("查询成功", users);

        } catch (Exception e) {
            logger.error("获取用户列表失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/user/search")
    public R<List<User>> searchUsers(@RequestParam String userNick) {
        logger.debug("搜索用户请求，昵称: {}", userNick);

        try {
            List<User> users = userService.searchUsersByNick(userNick);
            return R.ok("搜索成功", users);

        } catch (Exception e) {
            logger.error("搜索用户失败", e);
            return R.fail("搜索失败: " + e.getMessage());
        }
    }

    // ==================== 应用管理 ====================

    /**
     * 创建新应用
     */
    @PostMapping("/app/create")
    public R<Map<String, Object>> createApp(@RequestBody Map<String, Object> createRequest) {
        logger.info("管理员创建应用请求");

        try {
            String appId = (String) createRequest.get("appId");
            String secretKey = (String) createRequest.get("secretKey");
            String encryptionKey = (String) createRequest.get("encryptionKey");
            Integer rateLimit = createRequest.get("rateLimit") != null ?
                              Integer.valueOf(createRequest.get("rateLimit").toString()) : 1000;
            Integer timeout = createRequest.get("timeout") != null ?
                            Integer.valueOf(createRequest.get("timeout").toString()) : -1;
            String createBy = (String) createRequest.get("createBy");

            App app = appService.createApp(appId, secretKey, encryptionKey, rateLimit, timeout, createBy);

            Map<String, Object> appResponse = new HashMap<>();
            appResponse.put("appId", app.getAppId());
            appResponse.put("rateLimit", app.getRateLimit());
            appResponse.put("timeout", app.getTimeout());
            appResponse.put("createAt", app.getCreateAt());

            logger.info("应用创建成功，appId: {}", appId);
            return R.ok("应用创建成功", appResponse);

        } catch (Exception e) {
            logger.error("应用创建失败", e);
            return R.fail("应用创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新应用信息
     */
    @PutMapping("/app/update")
    public R<Map<String, Object>> updateApp(@RequestBody Map<String, Object> updateRequest) {
        logger.info("管理员更新应用请求");

        try {
            String appId = (String) updateRequest.get("appId");
            Integer rateLimit = updateRequest.get("rateLimit") != null ?
                              Integer.valueOf(updateRequest.get("rateLimit").toString()) : null;
            Integer timeout = updateRequest.get("timeout") != null ?
                            Integer.valueOf(updateRequest.get("timeout").toString()) : null;
            String updateBy = (String) updateRequest.get("updateBy");

            App app = appService.updateApp(appId, rateLimit, timeout, updateBy);

            Map<String, Object> appResponse = new HashMap<>();
            appResponse.put("appId", app.getAppId());
            appResponse.put("rateLimit", app.getRateLimit());
            appResponse.put("timeout", app.getTimeout());
            appResponse.put("updateAt", app.getUpdateAt());

            logger.info("应用更新成功，appId: {}", appId);
            return R.ok("应用更新成功", appResponse);

        } catch (Exception e) {
            logger.error("应用更新失败", e);
            return R.fail("应用更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除应用
     */
    @DeleteMapping("/app/delete/{appId}")
    public R<Void> deleteApp(@PathVariable String appId, @RequestParam String deleteBy) {
        logger.info("管理员删除应用请求，appId: {}", appId);

        try {
            appService.deleteApp(appId, deleteBy);

            logger.info("应用删除成功，appId: {}", appId);
            return R.ok("应用删除成功");

        } catch (Exception e) {
            logger.error("应用删除失败", e);
            return R.fail("应用删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取应用信息
     */
    @GetMapping("/app/info/{appId}")
    public R<Map<String, Object>> getAppInfo(@PathVariable String appId) {
        logger.debug("获取应用信息请求，appId: {}", appId);

        try {
            Optional<App> appOpt = appService.findByAppId(appId);
            if (appOpt.isEmpty()) {
                return R.fail("应用不存在");
            }

            App app = appOpt.get();

            Map<String, Object> appInfo = new HashMap<>();
            appInfo.put("appId", app.getAppId());
            appInfo.put("rateLimit", app.getRateLimit());
            appInfo.put("timeout", app.getTimeout());
            appInfo.put("createAt", app.getCreateAt());
            appInfo.put("updateAt", app.getUpdateAt());
            appInfo.put("createBy", app.getCreateBy());
            appInfo.put("updateBy", app.getUpdateBy());
            // 注意：出于安全考虑，不返回密钥信息

            return R.ok("查询成功", appInfo);

        } catch (Exception e) {
            logger.error("获取应用信息失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有活跃应用
     */
    @GetMapping("/app/list")
    public R<List<App>> getAppList() {
        logger.debug("获取应用列表请求");

        try {
            List<App> apps = appService.findAllActiveApps();
            return R.ok("查询成功", apps);

        } catch (Exception e) {
            logger.error("获取应用列表失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }
}
