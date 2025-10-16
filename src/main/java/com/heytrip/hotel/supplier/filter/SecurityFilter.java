package com.heytrip.hotel.supplier.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.config.Config;
import com.heytrip.hotel.supplier.constant.HeaderNames;
import com.heytrip.hotel.supplier.dto.R;
import com.heytrip.hotel.supplier.service.AppService;
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
import java.util.Optional;

/**
 * MD5签名认证过滤器
 * 实现基于MD5签名的API认证机制
 *
 * @author Pax
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private static final long MAX_TIME_SKEW_SECONDS = 300; // 5分钟时间偏差

    @Resource
    private Config CONFIG;

    @Resource
    private AppService appService;

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

            String appId = request.getHeader(HeaderNames.HEADER_APP_ID);
            String timestamp = request.getHeader(HeaderNames.HEADER_TIMESTAMP);
            String signature = request.getHeader(HeaderNames.HEADER_SIGNATURE);

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
        String appId = request.getHeader(HeaderNames.HEADER_APP_ID);
        String timestamp = request.getHeader(HeaderNames.HEADER_TIMESTAMP);
        String signature = request.getHeader(HeaderNames.HEADER_SIGNATURE);

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
     * 首先尝试从数据库验证，如果失败则回退到配置文件验证（向后兼容）
     */
    private boolean validateAppId(String appId) {
        // 优先从数据库验证
        if (appService.validateApp(appId)) {
            logger.debug("数据库应用验证通过，appId: {}", appId);
            return true;
        }

        // 回退到配置文件验证（向后兼容）
        boolean configValid = CONFIG.getAuthorization().getAppId().equals(appId);
        if (configValid) {
            logger.debug("配置文件应用验证通过，appId: {}", appId);
        } else {
            logger.warn("应用验证失败，appId: {}", appId);
        }

        return configValid;
    }

    /**
     * 验证MD5签名
     * 首先尝试使用数据库中的密钥验证，如果失败则回退到配置文件验证（向后兼容）
     */
    private boolean validateSignature(String appId, String timestamp, String signature) {
        try {
            // 优先尝试数据库中的密钥
            Optional<String> secretKeyOpt = appService.getSecretKey(appId);
            if (secretKeyOpt.isPresent()) {
                String expectedSignature = SignUtil.generateSignature(appId, timestamp, secretKeyOpt.get());
                if (expectedSignature.equalsIgnoreCase(signature)) {
                    logger.debug("数据库密钥签名验证通过，appId: {}", appId);
                    return true;
                }
            }

            // 回退到配置文件验证（向后兼容）
            if (CONFIG.getAuthorization().getAppId().equals(appId)) {
                String expectedSignature = SignUtil.generateSignature(appId, timestamp, CONFIG.getAuthorization().getSecretKey());
                boolean configValid = expectedSignature.equalsIgnoreCase(signature);
                if (configValid) {
                    logger.debug("配置文件密钥签名验证通过，appId: {}", appId);
                } else {
                    logger.warn("签名验证失败，appId: {}", appId);
                }
                return configValid;
            }

            logger.warn("无法找到应用的密钥信息，appId: {}", appId);
            return false;

        } catch (Exception e) {
            logger.error("验证签名时出错，appId: {}", appId, e);
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

        String jsonResponse = objectMapper.writeValueAsString( R.fail(401,message));
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
