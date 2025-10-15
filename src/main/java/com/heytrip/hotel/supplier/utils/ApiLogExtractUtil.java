package com.heytrip.hotel.supplier.utils;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * API日志数据提取工具类
 * 负责从请求和响应中提取关键业务字段
 *
 * @author Pax
 * @since 1.0.0
 */
public class ApiLogExtractUtil {

    private static final Logger logger = LoggerFactory.getLogger(ApiLogExtractUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从HTTP请求中提取客户端IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理的情况，取第一个IP
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * 将请求头转换为JSON字符串
     */
    public static String extractRequestHeaders(HttpServletRequest request) {
        try {
            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();

            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);

                // 过滤敏感信息
                if (isSensitiveHeader(headerName)) {
                    headerValue = "***";
                }
                headers.put(headerName, headerValue);
            }

            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            logger.warn("提取请求头失败", e);
            return "{}";
        }
    }

    /**
     * 将请求参数转换为JSON字符串
     */
    public static String extractRequestParams(HttpServletRequest request) {
        try {
            Map<String, String[]> parameterMap = request.getParameterMap();
            Map<String, Object> params = new HashMap<>();

            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();

                if (values.length == 1) {
                    params.put(key, values[0]);
                } else {
                    params.put(key, Arrays.asList(values));
                }
            }

            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            logger.warn("提取请求参数失败", e);
            return "{}";
        }
    }

    /**
     * 从JSON字符串中提取指定字段
     */
    public static Map<String, Object> extractFieldsFromJson(String jsonContent, String[] fieldNames) {
        return extractFieldsFromJson(jsonContent, fieldNames, true);
    }

    /**
     * 从JSON字符串中提取指定字段
     *
     * @param jsonContent JSON字符串内容
     * @param fieldNames 要提取的字段名数组
     * @param ignoreCase 是否忽略大小写匹配字段名
     * @return 提取到的字段Map
     */
    public static Map<String, Object> extractFieldsFromJson(String jsonContent, String[] fieldNames, boolean ignoreCase) {
        Map<String, Object> extractedFields = new HashMap<>();

        if (!StringUtils.hasText(jsonContent) || fieldNames == null || fieldNames.length == 0) {
            return extractedFields;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            for (String fieldName : fieldNames) {
                Object value = extractFieldValue(rootNode, fieldName, ignoreCase);
                if (value != null) {
                    extractedFields.put(fieldName, value);
                }
            }
        } catch (Exception e) {
            logger.warn("从JSON中提取字段失败: {}", jsonContent.length() > 200 ?
                      jsonContent.substring(0, 200) + "..." : jsonContent, e);
        }

        return extractedFields;
    }

    /**
     * 递归提取JSON字段值（支持嵌套字段）
     */
    private static Object extractFieldValue(JsonNode node, String fieldPath) {
        return extractFieldValue(node, fieldPath, true);
    }


    /**
     * 递归提取JSON字段值（支持嵌套字段和忽略大小写）
     *
     * @param node JSON节点
     * @param fieldPath 字段路径，支持点分隔的嵌套字段
     * @param ignoreCase 是否忽略大小写匹配字段名
     * @return 字段值
     */
    private static Object extractFieldValue(JsonNode node, String fieldPath, boolean ignoreCase) {
        return extractFieldValue(node, fieldPath, ignoreCase, 3);
    }

    /**
     * 递归提取JSON字段值（支持嵌套字段、忽略大小写和深度限制）
     *
     * @param node JSON节点
     * @param fieldPath 字段路径，支持点分隔的嵌套字段
     * @param ignoreCase 是否忽略大小写匹配字段名
     * @param maxDepth 最大搜索深度，防止过深递归
     * @return 字段值
     */
    private static Object extractFieldValue(JsonNode node, String fieldPath, boolean ignoreCase, int maxDepth) {
        if (node == null || !StringUtils.hasText(fieldPath) || maxDepth <= 0) {
            return null;
        }

        // 如果字段路径包含点分隔符，按指定路径查找
        if (fieldPath.contains(".")) {
            return extractFieldByPath(node, fieldPath, ignoreCase);
        }

        // 简单字段名，先在当前层级查找
        JsonNode directMatch = findFieldInNode(node, fieldPath, ignoreCase);
        if (directMatch != null) {
            return convertNodeToValue(directMatch);
        }

        // 当前层级未找到，递归搜索子层级（深度优先搜索）
        return searchFieldRecursively(node, fieldPath, ignoreCase, maxDepth);
    }

    /**
     * 按指定路径提取字段值（点分隔的嵌套字段）
     */
    private static Object extractFieldByPath(JsonNode node, String fieldPath, boolean ignoreCase) {
        String[] pathParts = fieldPath.split("\\.");
        JsonNode currentNode = node;

        for (String part : pathParts) {
            if (currentNode.isArray()) {
                // 如果是数组，尝试从第一个元素中提取
                if (currentNode.size() > 0) {
                    currentNode = findFieldInNode(currentNode.get(0), part, ignoreCase);
                } else {
                    return null;
                }
            } else {
                currentNode = findFieldInNode(currentNode, part, ignoreCase);
            }

            if (currentNode == null) {
                return null;
            }
        }

        return convertNodeToValue(currentNode);
    }

    /**
     * 递归搜索字段（深度优先搜索）
     */
    private static Object searchFieldRecursively(JsonNode node, String fieldName, boolean ignoreCase, int remainingDepth) {
        if (node == null || remainingDepth <= 0) {
            return null;
        }

        // 遍历当前节点的所有子字段
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                JsonNode childNode = field.getValue();

                // 递归搜索子节点
                Object result = searchFieldRecursively(childNode, fieldName, ignoreCase, remainingDepth - 1);
                if (result != null) {
                    return result;
                }

                // 检查当前子节点是否匹配目标字段名
                JsonNode targetField = findFieldInNode(childNode, fieldName, ignoreCase);
                if (targetField != null) {
                    return convertNodeToValue(targetField);
                }
            }
        } else if (node.isArray()) {
            // 如果是数组，递归搜索数组元素
            for (JsonNode arrayElement : node) {
                Object result = searchFieldRecursively(arrayElement, fieldName, ignoreCase, remainingDepth - 1);
                if (result != null) {
                    return result;
                }

                // 检查数组元素是否包含目标字段
                JsonNode targetField = findFieldInNode(arrayElement, fieldName, ignoreCase);
                if (targetField != null) {
                    return convertNodeToValue(targetField);
                }
            }
        }

        return null;
    }

    /**
     * 将JsonNode转换为相应的Java值
     */
    private static Object convertNodeToValue(JsonNode node) {
        if (node == null) {
            return null;
        }

        // 根据节点类型返回相应的值
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isDouble() || node.isFloat()) {
            return node.decimalValue();
        } else if (node.isBigDecimal()) {
            return node.decimalValue();
        } else if (node.isLong() || node.isInt() || node.isShort() || node.isBigInteger()) {
            return node.asLong();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isNull()) {
            return null;
        } else if (node.isArray() || node.isObject()) {
            return node.toString();
        } else {
            return node.toString();
        }
    }

    /**
     * 在JSON节点中查找指定字段，支持忽略大小写
     *
     * @param node JSON节点
     * @param fieldName 字段名
     * @param ignoreCase 是否忽略大小写
     * @return 找到的字段节点，如果未找到则返回null
     */
    private static JsonNode findFieldInNode(JsonNode node, String fieldName, boolean ignoreCase) {
        if (node == null || !StringUtils.hasText(fieldName)) {
            return null;
        }

        // 如果不忽略大小写，直接使用原有逻辑
        if (!ignoreCase) {
            return node.get(fieldName);
        }

        // 忽略大小写查找字段
        if (node.isObject()) {
            // 遍历所有字段名，进行大小写不敏感的比较
            var fields = node.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                String actualFieldName = field.getKey();
                if (actualFieldName.equalsIgnoreCase(fieldName)) {
                    logger.debug("字段名大小写匹配成功: 配置字段='{}', JSON字段='{}'", fieldName, actualFieldName);
                    return field.getValue();
                }
            }
        }

        return null;
    }

    /**
     * 生成唯一的traceId
     */
    public static String generateTraceId() {
       return IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * 判断是否为敏感请求头
     */
    private static boolean isSensitiveHeader(String headerName) {
        if (headerName == null) {
            return false;
        }

        String lowerName = headerName.toLowerCase();
        return lowerName.contains("authorization") ||
               lowerName.contains("token") ||
               lowerName.contains("password") ||
               lowerName.contains("secret") ||
               lowerName.contains("sign");
    }

    /**
     * 智能处理内容长度（压缩优先策略）
     * 优先使用压缩，如果压缩失败或不适用则截断
     *
     * @param content 原始内容
     * @param maxLength 最大长度限制
     * @param compressionEnabled 是否启用压缩
     * @param compressionThreshold 压缩阈值
     * @param algorithm 压缩算法
     * @param level 压缩级别
     * @return 处理后的内容
     */
    public static String processContent(String content, int maxLength,
                                      boolean compressionEnabled, int compressionThreshold,
                                      JsonCompressionUtil.CompressionAlgorithm algorithm, int level) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }

        // 策略1：如果启用压缩且内容是JSON格式，优先尝试压缩
        if (compressionEnabled && isJsonContent(content)) {
            try {
                String compressed = JsonCompressionUtil.compressIfNeeded(
                    content, compressionThreshold, algorithm, level);

                // 如果压缩后长度符合要求，直接返回
                if (compressed.length() <= maxLength) {
                    return compressed;
                } else {
                    logger.info("压缩后长度仍超限，压缩前={}, 压缩后={}, 限制={}",
                              content.length(), compressed.length(), maxLength);
                }
            } catch (Exception e) {
                logger.warn("JSON压缩失败，降级为截断处理: {}", e.getMessage());
            }
        }

        // 策略2：压缩失败或不适用时，使用安全截断
        return truncateContentSafely(content, maxLength);
    }

    /**
     * 安全截断内容（保留原有逻辑作为备选方案）
     */
    private static String truncateContentSafely(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }

        // 检查是否是JSON格式
        if (isJsonContent(content)) {
            return truncateJsonSafely(content, maxLength);
        } else {
            // 非JSON内容直接截断
            return content.substring(0, maxLength) + "...";
        }
    }

    /**
     * 检查内容是否为JSON格式
     */
    private static boolean isJsonContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String trimmed = content.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    /**
     * JSON安全截断方法
     * 尝试在保持JSON结构完整性的前提下截断内容
     */
    private static String truncateJsonSafely(String jsonContent, int maxLength) {
        try {
            // 如果长度不超过限制，直接返回
            if (jsonContent.length() <= maxLength) {
                return jsonContent;
            }

            // 尝试解析JSON以验证格式
            objectMapper.readTree(jsonContent);

            // 计算截断位置，确保不会破坏JSON结构
            int truncatePos = findSafeTruncatePosition(jsonContent, maxLength);

            if (truncatePos > 0) {
                String truncated = jsonContent.substring(0, truncatePos);

                // 尝试修复截断后的JSON结构
                String repairedJson = repairTruncatedJson(truncated);

                // 验证修复后的JSON是否有效
                try {
                    objectMapper.readTree(repairedJson);
                    return repairedJson + "...}";
                } catch (Exception e) {
                    // 修复失败，返回简化的JSON
                    return createSimplifiedJson(jsonContent, maxLength);
                }
            } else {
                return createSimplifiedJson(jsonContent, maxLength);
            }

        } catch (Exception e) {
            // JSON解析失败，按普通字符串处理
            logger.warn("JSON截断失败，按普通字符串处理: {}", e.getMessage());
            return jsonContent.substring(0, Math.min(maxLength, jsonContent.length())) + "...";
        }
    }

    /**
     * 找到安全的截断位置
     * 避免在JSON键值对中间截断
     */
    private static int findSafeTruncatePosition(String jsonContent, int maxLength) {
        if (maxLength >= jsonContent.length()) {
            return jsonContent.length();
        }

        // 从目标位置往前查找最近的完整字段结束位置
        int pos = Math.min(maxLength, jsonContent.length() - 1);

        // 寻找最近的 }, ] 或 " 结束位置
        while (pos > 0) {
            char c = jsonContent.charAt(pos);
            if (c == ',' || c == '}' || c == ']') {
                return pos;
            }
            if (c == '"') {
                // 确保这是一个完整的字符串结束
                if (pos > 0 && jsonContent.charAt(pos - 1) != '\\') {
                    return pos + 1;
                }
            }
            pos--;
        }

        return Math.min(maxLength / 2, jsonContent.length());
    }

    /**
     * 修复截断后的JSON结构
     */
    private static String repairTruncatedJson(String truncatedJson) {
        StringBuilder repaired = new StringBuilder(truncatedJson);

        // 移除末尾不完整的字段
        String content = repaired.toString();

        // 移除末尾的不完整内容（如未闭合的引号、逗号等）
        content = content.replaceAll(",\\s*$", "");
        content = content.replaceAll("\"[^\"]*$", "");
        content = content.replaceAll(":\\s*[^,}\\]]*$", "");

        return content;
    }

    /**
     * 创建简化的JSON表示
     * 当无法安全截断时，创建一个包含基本信息的简化JSON
     */
    private static String createSimplifiedJson(String originalJson, int maxLength) {
        try {
            // 尝试提取一些基本信息
            StringBuilder simplified = new StringBuilder();
            simplified.append("{");

            // 添加截断标识
            simplified.append("\"_truncated\":true,");
            simplified.append("\"_originalLength\":").append(originalJson.length()).append(",");
            simplified.append("\"_maxLength\":").append(maxLength).append(",");

            // 尝试提取一些关键字段
            String preview = originalJson.substring(0, Math.min(200, originalJson.length()));
            simplified.append("\"_preview\":\"").append(preview.replace("\"", "\\\"")).append("\"");

            simplified.append("}");

            return simplified.toString();

        } catch (Exception e) {
            // 最后的保险措施
            return "{\"_error\":\"JSON截断失败\",\"_originalLength\":" + originalJson.length() + "}";
        }
    }

    /**
     * 脱敏处理敏感数据
     */
    public static String maskSensitiveData(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }

        // 脱敏手机号
        content = content.replaceAll("(\"phone\"\\s*:\\s*\")\\d{3}(\\d{4})\\d{4}(\")", "$1***$2****$3");

        // 脱敏身份证号
        content = content.replaceAll("(\"idCard\"\\s*:\\s*\")\\d{14}(\\d{4})(\")", "$1**************$2$3");

        // 脱敏邮箱
        content = content.replaceAll("(\"email\"\\s*:\\s*\")[^@]{2,}([^@]{2})(@[^\"]+)(\")", "$1***$2$3$4");

        return content;
    }
}
