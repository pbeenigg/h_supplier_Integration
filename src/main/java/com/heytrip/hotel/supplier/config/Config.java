package com.heytrip.hotel.supplier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置类
 * 映射application.yml中的自定义配置
 *
 * @author Pax
 */
@Component
@ConfigurationProperties(prefix = "app")
public class Config {
    
    private Supplier supplier = new Supplier();
    private Authorization authorization = new Authorization();
    private Encryption encryption = new Encryption();
    private Api api = new Api();
    
    // Getter和Setter方法
    public Supplier getSupplier() {
        return supplier;
    }
    
    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }
    
    public Authorization getAuthorization() {
        return authorization;
    }
    
    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }
    
    public Encryption getEncryption() {
        return encryption;
    }
    
    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }
    
    public Api getApi() {
        return api;
    }
    
    public void setApi(Api api) {
        this.api = api;
    }
    
    /**
     * 供应商配置
     */
    public static class Supplier {
        private Integer timeout = 30000;
        private Retry retry = new Retry();
        
        public Integer getTimeout() {
            return timeout;
        }
        
        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
        
        public Retry getRetry() {
            return retry;
        }
        
        public void setRetry(Retry retry) {
            this.retry = retry;
        }
        
        /**
         * 重试配置
         */
        public static class Retry {
            private Integer maxAttempts = 3;
            private Long delay = 1000L;
            private Integer multiplier = 2;
            
            public Integer getMaxAttempts() {
                return maxAttempts;
            }
            
            public void setMaxAttempts(Integer maxAttempts) {
                this.maxAttempts = maxAttempts;
            }
            
            public Long getDelay() {
                return delay;
            }
            
            public void setDelay(Long delay) {
                this.delay = delay;
            }
            
            public Integer getMultiplier() {
                return multiplier;
            }
            
            public void setMultiplier(Integer multiplier) {
                this.multiplier = multiplier;
            }
        }
    }
    
    /**
     * 认证配置
     */
    public static class Authorization {
        private String appId = "heytrip_supplier_integration_pax";
        private String secretKey = "HeyTrip@Pax#SupplierIntegration!2025";
        
        public String getAppId() {
            return appId;
        }
        
        public void setAppId(String appId) {
            this.appId = appId;
        }
        
        public String getSecretKey() {
            return secretKey;
        }
        
        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }
    
    /**
     * 加密配置
     */
    public static class Encryption {
        private String key = "427ae41e4649b934ca495991b7852b855";
        
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
    }
    
    /**
     * API配置
     */
    public static class Api {
        private RateLimit rateLimit = new RateLimit();
        
        public RateLimit getRateLimit() {
            return rateLimit;
        }
        
        public void setRateLimit(RateLimit rateLimit) {
            this.rateLimit = rateLimit;
        }
        
        /**
         * 限流配置
         */
        public static class RateLimit {
            private Integer requestsPerMinute = 1000;
            
            public Integer getRequestsPerMinute() {
                return requestsPerMinute;
            }
            
            public void setRequestsPerMinute(Integer requestsPerMinute) {
                this.requestsPerMinute = requestsPerMinute;
            }
        }
    }
}
