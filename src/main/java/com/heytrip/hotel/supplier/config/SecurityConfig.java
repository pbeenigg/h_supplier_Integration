package com.heytrip.hotel.supplier.config;

import com.heytrip.hotel.supplier.authorization.SecurityFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置类
 * 配置API认证和授权规则
 * 
 * @author  Pax
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Resource
    private Config config;
    
    @Bean
    public SecurityFilter securityFilter() {
        return new SecurityFilter();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // 禁用CSRF保护
            .csrf(csrf -> csrf.disable())
            
            // 配置会话管理为无状态
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 配置授权规则
            .authorizeHttpRequests(authz -> {
                // 动态配置不需要认证的接口
                String[] permitAllPatterns = config.getSecurity().getPermitAllPatterns().toArray(new String[0]);
                authz.requestMatchers(permitAllPatterns).permitAll();
                
                // 动态配置需要认证的接口
                String[] authenticatedPatterns = config.getSecurity().getAuthenticatedPatterns().toArray(new String[0]);
                authz.requestMatchers(authenticatedPatterns).authenticated();
                
                // 其他请求拒绝访问
                authz.anyRequest().denyAll();
            })
            
            // 添加自定义认证过滤器
            .addFilterBefore(securityFilter(), UsernamePasswordAuthenticationFilter.class)
            
            .build();
    }
}
