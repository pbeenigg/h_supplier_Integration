package com.heytrip.hotel.supplier.adapter.builder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * QTECH 查询参数构建器：
 * - 从 DTO 反射出 key=value
 * - 对标注 @JsonParam 的字段或 List/Map/数组做 JSON 紧凑序列化（不 URL 编码）
 * - 日期(LocalDate)按 dd/MM/yyyy 格式化
 * - 输出形如：/ws/index.php?key1=val1&key2={...json...}
 */
public class QTechQueryBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY); // 忽略 null、空字符串、空集合

    public static String buildEndpoint(Object dto) {
        if (dto == null) return "/ws/index.php";
        Map<String, String> params = extractParams(dto);
        StringBuilder endpoint = new StringBuilder("/ws/index.php?");
        for (Map.Entry<String, String> e : params.entrySet()) {
            endpoint.append(e.getKey()).append("=").append(e.getValue()).append("&");
        }
        if (endpoint.charAt(endpoint.length() - 1) == '&') {
            endpoint.setLength(endpoint.length() - 1);
        }
        return endpoint.toString();
    }

    private static Map<String, String> extractParams(Object dto) {
        Map<String, String> params = new HashMap<>();
        Class<?> clazz = dto.getClass();
        try {
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object v = f.get(dto);
                if (v == null) continue;

                String key = f.getName();
                JsonProperty jp = f.getAnnotation(JsonProperty.class);
                if (jp != null && !jp.value().isEmpty()) key = jp.value();

                boolean toJson = f.isAnnotationPresent(JsonParam.class)
                        || v instanceof java.util.List
                        || v instanceof java.util.Map
                        || v.getClass().isArray();

                String value;
                if (v instanceof java.time.LocalDate) {
                    value = ((java.time.LocalDate) v).format(DATE_FORMATTER);
                } else if (toJson) {
                    value = toCompactJson(v);
                } else {
                    value = v.toString();
                }
                // 过滤空值：如果序列化/转字符串后为空，则不放入查询参数
                if (value != null && !value.trim().isEmpty()) {
                    params.put(key, value);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("构建QTECH查询参数失败", e);
        }
        return params;
    }


    /**
     * 转Json
     * @param v
     * @return
     */
    private static String toCompactJson(Object v) {
        try {
            Object cleaned = prune(v);
            // 若清洗后为空，则不返回任何内容（由调用方过滤空字符串，从而不上送该参数）
            if (isEmptyValue(cleaned)) {
                return "";
            }
            return OBJECT_MAPPER.writeValueAsString(cleaned);
        } catch (JsonProcessingException e) {
            // 降级：使用 toString，避免阻塞主流程
            return String.valueOf(v);
        }
    }

    /**
     * 递归清洗对象：
     * - 对 List：清洗每个元素，移除清洗后为空的元素
     * - 对 Map：清洗 value，移除 value 为空的条目
     * - 对 数组：转为 List 处理
     * - 对 POJO：反射为 Map<fieldName, value>，移除空值
     * - 对 基础类型/字符串：原样返回
     */
    private static Object prune(Object v) {
        if (v == null) return null;

        // 字符串：去除空白判断
        if (v instanceof String) {
            String s = ((String) v).trim();
            return s.isEmpty() ? null : s;
        }

        // List
        if (v instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) v;
            java.util.List<Object> out = new java.util.ArrayList<>(list.size());
            for (Object item : list) {
                Object cleaned = prune(item);
                if (!isEmptyValue(cleaned)) {
                    out.add(cleaned);
                }
            }
            return out.isEmpty() ? null : out;
        }

        // 数组
        if (v.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(v);
            java.util.List<Object> out = new java.util.ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                Object cleaned = prune(java.lang.reflect.Array.get(v, i));
                if (!isEmptyValue(cleaned)) {
                    out.add(cleaned);
                }
            }
            return out.isEmpty() ? null : out;
        }

        // Map
        if (v instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) v;
            java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                Object cleaned = prune(e.getValue());
                if (!isEmptyValue(cleaned)) {
                    out.put(key, cleaned);
                }
            }
            return out.isEmpty() ? null : out;
        }

        // POJO -> 反射为 Map（仅导出有值的字段，并应用 @JsonProperty 映射）
        if (!(v instanceof Number) && !(v instanceof Boolean) && !(v instanceof Character)) {
            java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
            Class<?> clazz = v.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object fv = f.get(v);
                    Object cleaned = prune(fv);
                    if (!isEmptyValue(cleaned)) {
                        String key = f.getName();
                        JsonProperty jp = f.getAnnotation(JsonProperty.class);
                        if (jp != null && !jp.value().isEmpty()) key = jp.value();
                        out.put(key, cleaned);
                    }
                } catch (IllegalAccessException ignore) {
                }
            }
            return out.isEmpty() ? null : out;
        }

        // 其他基础类型，直接返回
        return v;
    }

    private static boolean isEmptyValue(Object v) {
        if (v == null) return true;
        if (v instanceof String) return ((String) v).trim().isEmpty();
        if (v instanceof java.util.Collection) return ((java.util.Collection<?>) v).isEmpty();
        if (v instanceof java.util.Map) return ((java.util.Map<?, ?>) v).isEmpty();
        if (v.getClass().isArray()) return java.lang.reflect.Array.getLength(v) == 0;
        return false;
    }
}
