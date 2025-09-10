package com.heytrip.hotel.supplier.adapter.impl;

import com.heytrip.hotel.supplier.adapter.AbstractSupplierAdapter;
import com.heytrip.hotel.supplier.utils.SignUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Asianoverland Via QTECH 供应商适配器实现
 * 
 * @author  Pax
 */
@Component
public class AsianOverlandAdapter extends AbstractSupplierAdapter {

    private static final String SUPPLIER_NAME = "AsianOverland";
    private static final List<String> SUPPORTED_CITIES = Arrays.asList(
            "Kuala Lumpur", "Penang", "Johor Bahru", "Malacca", "Ipoh", "Kota Kinabalu", "Kuching"
    );

    @PostConstruct
    public void init() {
        initialize();
    }

    @Override
    public String getSupplierName() {
        return SUPPLIER_NAME;
    }

    @Override
    public boolean supportsCity(String city) {
        return SUPPORTED_CITIES.stream()
                .anyMatch(supportedCity -> supportedCity.equalsIgnoreCase(city));
    }



    @Override
    public int getPriority() {
        return 10; // 高优先级
    }


    /**
     * 构建头部信息，包含MD5签名认证
     * @param headers
     */
    @Override
    protected void addAuthHeaders(HttpHeaders headers) {
        if (supplierConfig != null && supplierConfig.getAuthConfig() != null) {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String appId = extractFromAuthConfig("appId");
            String secretKey = extractFromAuthConfig("secretKey");
            
            if (appId != null && secretKey != null) {
                String signature = SignUtil.generateSignature(appId, secretKey, timestamp);
                headers.add("X-App-Id", appId);
                headers.add("X-Timestamp", timestamp);
                headers.add("X-Signature", signature);
                headers.add("Content-Type", "application/json");
            }
        }
    }


    /**
     * 从供应商配置中提取认证参数
     * @param key
     * @return
     */
    private String extractFromAuthConfig(String key) {
        try {
            String authConfig = supplierConfig.getAuthConfig();
            // 简单的JSON解析，实际项目中应使用Jackson或其他JSON库
            if (authConfig.contains("\"" + key + "\"")) {
                int start = authConfig.indexOf("\"" + key + "\"") + key.length() + 3;
                int end = authConfig.indexOf("\"", start + 1);
                return authConfig.substring(start + 1, end);
            }
        } catch (Exception e) {
            logger.error("提取失败 {} 从授权配置", key, e);
        }
        return null;
    }




}
