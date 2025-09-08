package com.heytrip.hotel.supplier.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Map;

/**
 * MD5签名认证过滤器
 * 实现基于MD5签名的API认证机制
 * 
 * @author  Pax
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);
    
    private static final String HEADER_APP_ID = "X-App-Id";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_SIGNATURE = "X-Signature";
    
    private static final long MAX_TIME_SKEW_SECONDS = 300; // 5分钟时间偏差
    
    @Value("${app.supplier.authorization.app-id:pax}")
    private String validAppId;
    
    @Value("${app.supplier.authorization.secret-key:pax123456}")
    private String secretKey;
    
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
                sendAuthenticationError(response, "Missing or invalid authentication headers");
                return;
            }
            
            String appId = request.getHeader(HEADER_APP_ID);
            String timestamp = request.getHeader(HEADER_TIMESTAMP);
            String signature = request.getHeader(HEADER_SIGNATURE);
            
            // 验证时间戳
            if (!validateTimestamp(timestamp)) {
                sendAuthenticationError(response, "Invalid or expired timestamp");
                return;
            }
            
            // 验证AppId
            if (!validateAppId(appId)) {
                sendAuthenticationError(response, "Invalid app ID");
                return;
            }
            
            // 验证签名
            if (!validateSignature(appId, timestamp, signature)) {
                sendAuthenticationError(response, "Invalid signature");
                return;
            }
            
            // 设置认证信息
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(appId, null, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            logger.debug("Authentication successful for app ID: {}", appId);
            
        } catch (Exception e) {
            logger.error("Authentication error", e);
            sendAuthenticationError(response, "Authentication failed: " + e.getMessage());
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 判断是否跳过认证
     */
    private boolean skipAuthentication(String requestUri) {
        return requestUri.startsWith("/actuator/") ||
               requestUri.startsWith("/swagger-ui/") ||
               requestUri.equals("/version") ||
               requestUri.startsWith("/monitor/") ||
               requestUri.equals("/v1/api-docs") ||
               requestUri.startsWith("/v1/api-docs/");
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
            logger.warn("Invalid timestamp format: {}", timestampStr);
            return false;
        }
    }
    
    /**
     * 验证AppId
     */
    private boolean validateAppId(String appId) {
        return validAppId.equals(appId);
    }
    
    /**
     * 验证MD5签名
     */
    private boolean validateSignature(String appId, String timestamp, String signature) {
        try {
            String expectedSignature = generateSignature(appId, timestamp, secretKey);
            return expectedSignature.equalsIgnoreCase(signature);
        } catch (Exception e) {
            logger.error("Error validating signature", e);
            return false;
        }
    }
    
    /**
     * 生成MD5签名
     */
    private String generateSignature(String appId, String timestamp, String secretKey) {
        try {
            String data = appId + timestamp + secretKey;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(data.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Error generating signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
    
    /**
     * 发送认证错误响应
     */
    private void sendAuthenticationError(HttpServletResponse response, String message) throws IOException {
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
