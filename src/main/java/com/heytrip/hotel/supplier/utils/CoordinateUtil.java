package com.heytrip.hotel.supplier.utils;


import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 经纬度坐标处理工具类
 */
@Slf4j
public class CoordinateUtil {

    /**
     * 解析经纬度坐标
     * 处理各种异常情况：空值、格式错误、超出范围、特殊符号、科学计数法等
     *
     * @param rawValue 原始坐标值
     * @param coordType 坐标类型（"经度" 或 "纬度"）
     * @param minValue 最小有效值
     * @param maxValue 最大有效值
     * @param hotelCode 酒店编号（用于日志记录）
     * @return 解析后的坐标值，解析失败返回空字符串
     */
    public static String parseCoordinate(String rawValue, String coordType, double minValue, double maxValue, String hotelCode) {
        if (isBlank(rawValue)) {
            return StrUtil.EMPTY;
        }

        try {
            // 第一步：清理字符串（多个连续负号将统一为单个负号）
            String cleaned = cleanCoordinateString(rawValue);

            if (isBlank(cleaned)) {
                log.debug("酒店[{}]的{}为空，已设置为null", hotelCode, coordType);
                return StrUtil.EMPTY;
            }

            // 第二步：尝试解析为Double
            Double value = parseCoordinateValue(cleaned);

            if (value == null) {
                log.warn("酒店[{}]的{}解析失败: {}", hotelCode, coordType, rawValue);
                return StrUtil.EMPTY;
            }

            // 第三步：验证范围
            if (value < minValue || value > maxValue) {
                log.warn("酒店[{}]的{}超出合理范围[{}, {}]: {}", hotelCode, coordType, minValue, maxValue, value);
                return StrUtil.EMPTY;
            }

            // 第四步：验证是否为0（通常无效）
            if (Math.abs(value) < 0.000001) {
                log.debug("酒店[{}]的{}为0，已设置为null", hotelCode, coordType);
                return StrUtil.EMPTY;
            }

            // 第五步：验证是否为NaN或Infinity
            if (Double.isNaN(value) || java.lang.Double.isInfinite(value)) {
                log.warn("酒店[{}]的{}解析为NaN或Infinity: {}", hotelCode, coordType, rawValue);
                return StrUtil.EMPTY;
            }

            // 第六步：检测经纬度可能互换的情况（业务规则：纬度通常小于经度的绝对值）
            // 如果纬度值超过90但在180范围内，可能是经纬度互换
            if ("纬度".equals(coordType) && Math.abs(value) > 90 && Math.abs(value) <= 180) {
                log.warn("酒店[{}]的纬度值[{}]超出范围但在经度范围内，可能经纬度互换，已过滤", hotelCode, value);
                return StrUtil.EMPTY;
            }

            // 第七步：格式化输出，保留6位小数
            String formatted = String.format("%.6f", value);

            // 第八步：验证格式化后的字符串长度（数据库字段限制）
            if (formatted.length() > 20) {
                log.warn("酒店[{}]的{}格式化后长度超过20字符: {}", hotelCode, coordType, formatted);
                return StrUtil.EMPTY;
            }

            return formatted;

        } catch (Exception e) {
            log.warn("酒店[{}]的{}解析异常: {}, 错误: {}", hotelCode, coordType, rawValue, e.getMessage());
            return StrUtil.EMPTY;
        }
    }

    /**
     * 清理坐标字符串，移除各种特殊符号和无效字符
     * 处理示例：
     * - "3.162790, 101.711120,17" -> "3.162790"
     * - "3.0848&deg; N" -> "3.0848"
     * - "  3.0817076 ," -> "3.0817076"
     */
    public static String cleanCoordinateString(String raw) {
        if (raw == null) {
            return "";
        }

        String result = raw.trim();

        // 移除HTML实体和特殊符号（如 &deg;, &nbsp; 等）
        result = result.replaceAll("&[a-zA-Z]+;", "");

        // 移除方向标识（N, S, E, W），但保留科学计数法中的E
        // 方向标识E通常后面跟空格或在末尾，科学计数法E后面跟数字或+/-
        result = result.replaceAll("\\s+[NSEW]\\s*", " ");  // 前有空格的方向标识
        result = result.replaceAll("[NSEW]\\s*$", "");       // 末尾的方向标识
        result = result.replaceAll("^[NSEW]\\s+", "");       // 开头的方向标识

        // 移除度分秒符号（°, ', "）
        result = result.replaceAll("[°'\"′″]", "");

        // 先处理中文小数点（。）直接替换为英文小数点
        result = result.replace("。", ".");

        // 如果包含逗号（英文或中文），只取第一个数字（处理 "3.162790, 101.711120" 这种情况）
        if (result.contains(",") || result.contains("，")) {
            result = result.replace("，", ",");  // 统一为英文逗号
            String[] parts = result.split(",");
            // 找到第一个非空部分
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result = trimmed;
                    break;
                }
            }
        }

        // 移除所有非数字、非小数点、非负号的字符
        result = result.replaceAll("[^0-9.\\-Ee+]", "");

        // 处理多个连续负号的情况：统一为单个负号
        // 例如：--3.162790 -> -3.162790, ---3.162790 -> -3.162790
        result = result.replaceAll("^-+", "-");  // 开头的任意多个负号统一为一个

        // 检测负号在中间位置的异常情况（如 "3.-162" 或 "3-.162"）
        if (result.matches(".*\\d-.*") || result.matches(".*\\.-.*")) {
            log.debug("检测到负号在中间位置的异常格式: {}", result);
            return "";
        }

        // 处理多个小数点的情况，只保留第一个
        int firstDotIndex = result.indexOf('.');
        if (firstDotIndex != -1) {
            String beforeDot = result.substring(0, firstDotIndex + 1);
            String afterDot = result.substring(firstDotIndex + 1).replace(".", "");
            result = beforeDot + afterDot;
        }

        return result.trim();
    }

    /**
     * 将清理后的字符串解析为Double值
     * 支持科学计数法表示
     */
    public static Double parseCoordinateValue(String cleaned) {
        if (isBlank(cleaned)) {
            return null;
        }

        try {
            // 尝试解析（包括科学计数法）
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            // 解析失败
            return null;
        }
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
