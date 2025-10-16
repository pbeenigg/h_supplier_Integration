package com.heytrip.hotel.supplier.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 认证助手类
 * 用于在响应式编程中获取简单的签名认证信息
 * 适用于只需要签名认证而不需要复杂用户登录系统的场景
 *
 * @author Pax
 */
public class AuthHelper {

    private static final Logger logger = LoggerFactory.getLogger(AuthHelper.class);

    /**
     * 检查当前请求是否已认证
     *
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        try {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                Boolean authenticated = (Boolean) request.getAttribute("AUTHENTICATED");
                return authenticated != null && authenticated;
            }

            return false;

        } catch (Exception e) {
            logger.warn("检查认证状态时发生异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前认证的APP ID
     *
     * @return APP ID或null
     */
    public static String getAppId() {
        try {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                return (String) request.getAttribute("APP_ID");
            }

            return null;

        } catch (Exception e) {
            logger.warn("获取APP ID时发生异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前登录用户的用户名
     * 优先从Security Context获取，如果没有则从Request属性获取
     *
     * @return 当前用户名或"system"作为默认值
     */
    public static String getCurrentUser() {
        try {
            // 优先从Spring Security Context获取用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    !"anonymousUser".equals(authentication.getName())) {
                String username = authentication.getName();
                if (username != null && !username.trim().isEmpty()) {
                    return username;
                }
            }

            // 从Request属性获取用户信息
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();

                // 优先获取用户名
                String userName = (String) request.getAttribute("USER_NAME");
                if (userName != null && !userName.trim().isEmpty()) {
                    return userName;
                }

                // 如果没有用户名，尝试获取APP_ID作为替代
                String appId = (String) request.getAttribute("APP_ID");
                if (appId != null && !appId.trim().isEmpty()) {
                    return appId;
                }
            }

            // 如果都没有，返回系统默认用户
            return "system";

        } catch (Exception e) {
            logger.warn("获取当前用户时发生异常: {}", e.getMessage());
            return "system";
        }
    }

    /**
     * 设置当前用户信息到Request属性中
     *
     * @param userName 用户名
     */
    public static void setCurrentUser(String userName) {
        try {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null && userName != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                request.setAttribute("USER_NAME", userName);
                logger.debug("设置当前用户: {}", userName);
            }

        } catch (Exception e) {
            logger.warn("设置当前用户时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 记录认证信息（用于调试）
     *
     * @param context 上下文信息
     */
    public static void logAuthInfo(String context) {
        try {
            String appId = getAppId();
            String currentUser = getCurrentUser();
            boolean authenticated = isAuthenticated();

            logger.info("认证信息 [{}] - 已认证: {}, APP ID: {}, 当前用户: {}",
                    context, authenticated, appId, currentUser);

        } catch (Exception e) {
            logger.warn("记录认证信息时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 要求当前请求必须已认证，否则抛出异常
     *
     * @throws RuntimeException 如果未认证
     */
    public static void requireAuthentication() {
        if (!isAuthenticated()) {
            throw new RuntimeException("此操作需要认证");
        }
    }

    /**
     * 获取认证的时间戳
     *
     * @return 认证时间戳或null
     */
    public static String getAuthTimestamp() {
        try {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                return (String) request.getAttribute("AUTH_TIMESTAMP");
            }

            return null;

        } catch (Exception e) {
            logger.warn("获取认证时间戳时发生异常: {}", e.getMessage());
            return null;
        }
    }
}
