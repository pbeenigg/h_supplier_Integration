package com.heytrip.hotel.supplier.util;

import com.heytrip.hotel.supplier.exception.HttpClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

/**
 * URL构建与编码工具
 * 统一提供：
 * 1) 仅对看起来像JSON的查询参数值进行编码
 * 2) 将相对endpoint与baseUrl规范化拼接为绝对URL
 *
 * 所有注释均为中文，便于团队理解与维护。
 */
public final class UrlUtils {

    private UrlUtils() {}

    /**
     * 构建最终请求 URL：
     * 1) 仅对看起来像 JSON 的参数值进行编码；
     * 2) 若 endpoint 为相对路径，则与 baseUrl 进行规范化拼接；
     * 3) 若 endpoint 为绝对 URL，则直接返回。
     */
    public static String buildFinalUrl(String baseUrl, String endpoint) {
        String safeEndpoint = encodeQueryParamValues(endpoint);
        if (safeEndpoint.startsWith("http://") || safeEndpoint.startsWith("https://")) {
            return safeEndpoint;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new HttpClientException("Base URL 为空，且 endpoint 为相对路径，无法构建完整请求 URL: " + safeEndpoint, null);
        }
        boolean baseEnds = baseUrl.endsWith("/");
        boolean epStarts = safeEndpoint.startsWith("/");
        if (baseEnds && epStarts) {
            return baseUrl + safeEndpoint.substring(1);
        } else if (!baseEnds && !epStarts) {
            return baseUrl + "/" + safeEndpoint;
        } else {
            return baseUrl + safeEndpoint;
        }
    }

    /**
     * 仅对查询参数的值进行 URL 编码，保留 key、路径、分隔符原样
     * 示例：
     * /ws/index.php?username=Heytrip_Test&password=Welcome@@123&roomDetails=[{"numberOfAdults":2}]
     * -> /ws/index.php?username=Heytrip_Test&password=Welcome@@123&roomDetails=%5B%7B%22numberOfAdults%22%3A2%7D%5D
     */
    public static String encodeQueryParamValues(String endpoint) {
        return encodeQueryParamValues(endpoint, Collections.emptySet(), true);
    }

    /**
     * 仅对查询参数值进行选择性编码
     * @param endpoint 原始URL（可包含查询串）
     * @param forceKeys 需要强制编码的参数名集合（仅编码其值），通用场景请传空集合
     * @param encodeJsonValues 是否对“看起来像JSON”的值进行编码（以 { 或 [ 开头，且包含 ':' 或 ','）
     */
    public static String encodeQueryParamValues(String endpoint, Set<String> forceKeys, boolean encodeJsonValues) {
        if (endpoint == null || !endpoint.contains("?")) {
            return endpoint;
        }
        int idx = endpoint.indexOf('?');
        String base = endpoint.substring(0, idx);
        String query = endpoint.substring(idx + 1);

        StringBuilder sb = new StringBuilder();
        sb.append(base).append('?');
        String[] pairs = query.split("&", -1);
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq < 0) {
                // 没有值，仅保留 key
                sb.append(pair);
            } else {
                String key = pair.substring(0, eq);
                String value = pair.substring(eq + 1);
                // 兼容异常情况：如果值里错误地包含了“key=”前缀（如 roomDetails=roomDetails=[...])，去掉重复前缀
                if (value.startsWith(key + "=")) {
                    value = value.substring(key.length() + 1);
                }
                String encodedValue = value;
                boolean looksJson = false;
                if (value != null) {
                    String vtrim = value.trim();
                    // 更稳健判定：以 { 或 [ 开头，并且包含 ':'（对象）或 ','（数组）
                    boolean startsJsonToken = vtrim.startsWith("{") || vtrim.startsWith("[");
                    boolean hasJsonLikeSignals = vtrim.contains(":") || vtrim.contains(",");
                    looksJson = startsJsonToken && hasJsonLikeSignals;
                }
                boolean shouldEncode = (forceKeys != null && !forceKeys.isEmpty() && forceKeys.contains(key)) || (encodeJsonValues && looksJson);
                if (shouldEncode) {
                    // 如果看起来已经包含百分号编码，尽量避免二次编码（简单判断）
                    if (!value.contains("%")) {
                        encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8)
                                .replace("+", "%20"); // 空格使用%20
                    }
                }
                sb.append(key).append('=').append(encodedValue);
            }
            if (i < pairs.length - 1) {
                sb.append('&');
            }
        }
        return sb.toString();
    }
}
