package com.heytrip.hotel.supplier.adapter.impl;

import com.heytrip.hotel.supplier.adapter.AbstractSupplierAdapter;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
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
    

    
    @Override
    protected void addAuthHeaders(HttpHeaders headers) {
        if (supplierConfig != null && supplierConfig.getAuthConfig() != null) {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String appId = extractFromAuthConfig("appId");
            String secretKey = extractFromAuthConfig("secretKey");
            
            if (appId != null && secretKey != null) {
                String signature = generateSignature(appId, secretKey, timestamp);
                headers.add("X-App-Id", appId);
                headers.add("X-Timestamp", timestamp);
                headers.add("X-Signature", signature);
                headers.add("Content-Type", "application/json");
            }
        }
    }
    
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
            logger.error("Failed to extract {} from authorization config", key, e);
        }
        return null;
    }
    
    private String generateSignature(String appId, String secretKey, String timestamp) {
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
            logger.error("Failed to generate signature", e);
            return "";
        }
    }

}
