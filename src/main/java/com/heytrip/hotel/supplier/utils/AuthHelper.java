package com.heytrip.hotel.supplier.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

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
     * 获取当前认证的Sign
     *
     * @return Sign或null
     */
    public static String getSign() {
        try {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                return (String) request.getAttribute("AUTH_SIGNATURE");
            }

            return null;

        } catch (Exception e) {
            logger.warn("获取Sign时发生异常: {}", e.getMessage());
            return null;
        }
    }


    /**
     * 获取认证时间戳
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
    
    /**
     * 记录认证信息到日志（用于调试）
     * 
     * @param operation 操作描述
     */
    public static void logAuthInfo(String operation) {
        try {
            boolean authenticated = isAuthenticated();
            String appId = getAppId();
            String timestamp = getAuthTimestamp();
            String signature = getSign();
            
            if (authenticated) {
                logger.debug("执行操作 '{}' - 已认证，APP ID: {}, 时间戳: {},签名: {}", operation, appId, timestamp,signature);
            } else {
                logger.debug("执行操作 '{}' - 未认证", operation);
            }
            
        } catch (Exception e) {
            logger.warn("记录认证信息时发生异常: {}", e.getMessage());
        }
    }
    
    /**
     * 验证当前请求的认证状态，如果未认证则抛出异常
     * 
     * @param operation 操作描述
     * @throws SecurityException 如果未认证
     */
    public static void requireAuthentication(String operation) throws SecurityException {
        if (!isAuthenticated()) {
            String message = String.format("操作 '%s' 需要认证，但当前请求未认证", operation);
            logger.warn(message);
            throw new SecurityException(message);
        }
    }
}
