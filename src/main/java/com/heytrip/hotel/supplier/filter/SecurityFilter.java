package com.heytrip.hotel.supplier.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.config.Config;
import com.heytrip.hotel.supplier.utils.SignUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * MD5签名认证过滤器
 * 实现基于MD5签名的API认证机制
 *
 * @author Pax
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private static final String HEADER_APP_ID = "X-App-Id";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_SIGNATURE = "X-Signature";

    private static final long MAX_TIME_SKEW_SECONDS = 300; // 5分钟时间偏差

    @Resource
    private Config CONFIG;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // 跳过不需要认证的接口
        if (skipAuthentication(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 验证认证头部
            if (!validateAuthHeaders(request)) {
                returnAuthenticationError(response, "缺失或无效的认证头");
                return;
            }

            String appId = request.getHeader(HEADER_APP_ID);
            String timestamp = request.getHeader(HEADER_TIMESTAMP);
            String signature = request.getHeader(HEADER_SIGNATURE);

            // 验证时间戳
            if (!validateTimestamp(timestamp)) {
                returnAuthenticationError(response, "无效或已经过期");
                return;
            }

            // 验证AppId
            if (!validateAppId(appId)) {
                returnAuthenticationError(response, "无效的APP ID");
                return;
            }

            // 验证签名
            if (!validateSignature(appId, timestamp, signature)) {
                returnAuthenticationError(response, "无效签名");
                return;
            }

            //需要设置认证信息到请求属性
            request.setAttribute("AUTHENTICATED", true);
            request.setAttribute("APP_ID", appId);
            request.setAttribute("AUTH_TIMESTAMP", timestamp);
            request.setAttribute("AUTH_SIGNATURE", signature);

            // 设置认证信息
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(appId, null, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            logger.debug("认证成功，APP ID: {}", appId);

        } catch (Exception e) {
            logger.error("Authentication error", e);
            returnAuthenticationError(response, "认证失败: " + e.getMessage());
            return;
        }

        logger.debug("认证处理完成，继续执行过滤器链，URI: {}", requestUri);
        filterChain.doFilter(request, response);
        logger.debug("过滤器链执行完成，URI: {}", requestUri);
    }

    /**
     * 判断是否跳过认证
     * 使用动态配置来判断是否跳过认证
     */
    private boolean skipAuthentication(String requestUri) {
        return CONFIG.getSecurity().getPermitAllPatterns().stream()
                .anyMatch(pattern -> {
                    if (pattern.endsWith("/**")) {
                        // 处理通配符模式，如 /monitor/**
                        String prefix = pattern.substring(0, pattern.length() - 3);
                        return requestUri.startsWith(prefix);
                    } else if (pattern.endsWith("/*")) {
                        // 处理单级通配符模式，如 /suppliers/*
                        String prefix = pattern.substring(0, pattern.length() - 2);
                        return requestUri.startsWith(prefix + "/") && 
                               requestUri.indexOf('/', prefix.length() + 1) == -1;
                    } else {
                        // 精确匹配
                        return requestUri.equals(pattern);
                    }
                });
    }

    /**
     * 验证认证头部是否存在
     */
    private boolean validateAuthHeaders(HttpServletRequest request) {
        String appId = request.getHeader(HEADER_APP_ID);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String signature = request.getHeader(HEADER_SIGNATURE);

        return appId != null && !appId.trim().isEmpty() &&
                timestamp != null && !timestamp.trim().isEmpty() &&
                signature != null && !signature.trim().isEmpty();
    }

    /**
     * 验证时间戳
     */
    private boolean validateTimestamp(String timestampStr) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            long currentTime = System.currentTimeMillis() / 1000;
            long timeDiff = Math.abs(currentTime - timestamp);

            return timeDiff <= MAX_TIME_SKEW_SECONDS;
        } catch (NumberFormatException e) {
            logger.warn("无效的时间戳格式: {}", timestampStr);
            return false;
        }
    }

    /**
     * 验证AppId
     */
    private boolean validateAppId(String appId) {
        return CONFIG.getAuthorization().getAppId().equals(appId);
    }

    /**
     * 验证MD5签名
     */
    private boolean validateSignature(String appId, String timestamp, String signature) {
        try {
            String expectedSignature = SignUtil.generateSignature(appId, timestamp, CONFIG.getAuthorization().getSecretKey());
            return expectedSignature.equalsIgnoreCase(signature);
        } catch (Exception e) {
            logger.error("验证签名时出错", e);
            return false;
        }
    }


    /**
     * 发送认证错误响应
     */
    private void returnAuthenticationError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = Map.of(
                "code", 401,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
