package com.heytrip.hotel.supplier.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 响应式安全上下文助手类
 * 用于在响应式编程中正确传播和访问SecurityContext
 * 
 * @author Pax
 */
public class ReactiveSecurityContextHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(ReactiveSecurityContextHelper.class);
    
    private static final String SECURITY_CONTEXT_KEY = "SECURITY_CONTEXT";
    private static final String AUTHENTICATED_USER_KEY = "AUTHENTICATED_USER";
    
    /**
     * 从当前请求中获取SecurityContext并包装到Mono的Context中
     * 
     * @param <T> Mono的类型参数
     * @param mono 原始的Mono
     * @return 包含SecurityContext的Mono
     */
    public static <T> Mono<T> withSecurityContext(Mono<T> mono) {
        try {
            // 尝试从当前线程的SecurityContextHolder获取
            SecurityContext securityContext = SecurityContextHolder.getContext();
            
            if (securityContext != null && securityContext.getAuthentication() != null) {
                logger.debug("从SecurityContextHolder获取到SecurityContext: {}", 
                    securityContext.getAuthentication().getName());
                return mono.contextWrite(Context.of(SECURITY_CONTEXT_KEY, securityContext));
            }
            
            // 尝试从请求属性中获取
            SecurityContext requestSecurityContext = getSecurityContextFromRequest();
            if (requestSecurityContext != null) {
                logger.debug("从请求属性获取到SecurityContext: {}", 
                    requestSecurityContext.getAuthentication().getName());
                return mono.contextWrite(Context.of(SECURITY_CONTEXT_KEY, requestSecurityContext));
            }
            
            logger.debug("未找到SecurityContext，返回原始Mono");
            return mono;
            
        } catch (Exception e) {
            logger.warn("获取SecurityContext时发生异常: {}", e.getMessage());
            return mono;
        }
    }
    
    /**
     * 从Mono的Context中获取SecurityContext
     * 
     * @return 包含SecurityContext的Mono
     */
    public static Mono<SecurityContext> getSecurityContext() {
        return Mono.deferContextual(contextView -> {
            try {
                if (contextView.hasKey(SECURITY_CONTEXT_KEY)) {
                    SecurityContext securityContext = contextView.get(SECURITY_CONTEXT_KEY);
                    logger.debug("从Reactor Context获取到SecurityContext: {}", 
                        securityContext.getAuthentication().getName());
                    return Mono.just(securityContext);
                }
                
                // 尝试从当前线程获取
                SecurityContext securityContext = SecurityContextHolder.getContext();
                if (securityContext != null && securityContext.getAuthentication() != null) {
                    logger.debug("从SecurityContextHolder获取到SecurityContext: {}", 
                        securityContext.getAuthentication().getName());
                    return Mono.just(securityContext);
                }
                
                logger.debug("未找到SecurityContext");
                return Mono.empty();
                
            } catch (Exception e) {
                logger.warn("获取SecurityContext时发生异常: {}", e.getMessage());
                return Mono.empty();
            }
        });
    }
    
    /**
     * 从Mono的Context中获取当前认证的用户信息
     * 
     * @return 包含Authentication的Mono
     */
    public static Mono<Authentication> getAuthentication() {
        return getSecurityContext()
            .map(SecurityContext::getAuthentication)
            .filter(auth -> auth != null && auth.isAuthenticated());
    }
    
    /**
     * 从Mono的Context中获取当前认证的用户名
     * 
     * @return 包含用户名的Mono
     */
    public static Mono<String> getAuthenticatedUsername() {
        return getAuthentication()
            .map(Authentication::getName);
    }
    
    /**
     * 检查当前用户是否已认证
     * 
     * @return 包含认证状态的Mono
     */
    public static Mono<Boolean> isAuthenticated() {
        return getAuthentication()
            .map(Authentication::isAuthenticated)
            .defaultIfEmpty(false);
    }
    
    /**
     * 从当前HTTP请求中获取SecurityContext
     * 
     * @return SecurityContext或null
     */
    private static SecurityContext getSecurityContextFromRequest() {
        try {
            ServletRequestAttributes requestAttributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                Object securityContextObj = request.getAttribute(SECURITY_CONTEXT_KEY);
                
                if (securityContextObj instanceof SecurityContext) {
                    return (SecurityContext) securityContextObj;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn("从请求属性获取SecurityContext时发生异常: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从当前HTTP请求中获取认证用户名
     * 
     * @return 用户名或null
     */
    public static String getAuthenticatedUsernameFromRequest() {
        try {
            ServletRequestAttributes requestAttributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                Object userObj = request.getAttribute(AUTHENTICATED_USER_KEY);
                
                if (userObj instanceof String) {
                    return (String) userObj;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn("从请求属性获取认证用户名时发生异常: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建一个带有认证信息的Mono，用于调试和日志记录
     * 
     * @param <T> Mono的类型参数
     * @param mono 原始的Mono
     * @param operation 操作描述
     * @return 带有日志记录的Mono
     */
    public static <T> Mono<T> withAuthenticationLogging(Mono<T> mono, String operation) {
        return withSecurityContext(mono)
            .doOnSubscribe(subscription -> {
                String username = getAuthenticatedUsernameFromRequest();
                if (username != null) {
                    logger.debug("执行操作 '{}' - 认证用户: {}", operation, username);
                } else {
                    logger.debug("执行操作 '{}' - 未认证用户", operation);
                }
            })
            .doOnSuccess(result -> {
                String username = getAuthenticatedUsernameFromRequest();
                logger.debug("操作 '{}' 成功完成 - 用户: {}", operation, username);
            })
            .doOnError(error -> {
                String username = getAuthenticatedUsernameFromRequest();
                logger.error("操作 '{}' 执行失败 - 用户: {}, 错误: {}", operation, username, error.getMessage());
            });
    }
}
