package com.heytrip.hotel.supplier.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.heytrip.common.enums.XEnumCurrency;
import com.heytrip.hotel.supplier.dto.qtech.req.QTechSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * ID 工具类
 * 提供生成 SHA-256 十六进制字符串的方法
 *
 * @author Pax
 */
public class HeyUtil {

    private static final Logger logger = LoggerFactory.getLogger(HeyUtil.class);
    // 日期格式化器
    public static final DateTimeFormatter DATE_FORMATTER_DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER_Z = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");


    /**
     * 将带时区的时间字符串转换为目标时区
     *
     * @param dateTimeStr  输入时间字符串，格式：2025-12-29 03:00:01 +0530
     * @param targetZoneId 目标时区ID
     * @return 转换后的时间
     */
    public static ZonedDateTime convertTimeZone(String dateTimeStr, String targetZoneId) {
        return convertTimeZone(dateTimeStr, ZoneId.of(targetZoneId));
    }

    /**
     * 将带时区的时间字符串转换为目标时区 （默认时区：Asia/Shanghai ）
     *
     * @param dateTimeStr 输入时间字符串，格式：2025-12-29 03:00:01 +0530
     * @return 转换后的时间
     */
    public static ZonedDateTime convertTimeZone(String dateTimeStr) {
        return convertTimeZone(dateTimeStr, ZoneId.of("Asia/Shanghai"));
    }

    /**
     * 将带时区的时间字符串转换为目标时区
     *
     * @param dateTimeStr 输入时间字符串
     * @param targetZone  目标时区
     * @return 转换后的时间
     */
    public static ZonedDateTime convertTimeZone(String dateTimeStr, ZoneId targetZone) {
        try {
            // 分离日期时间和时区部分
            String trimmedStr = dateTimeStr.trim();
            String[] parts = trimmedStr.split(" (?=\\+|-)");

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid date time format: " + dateTimeStr);
            }

            String dateTimePart = parts[0];
            String zoneOffsetPart = parts[1];

            // 解析日期时间部分
            java.time.LocalDateTime localDateTime =
                    java.time.LocalDateTime.parse(dateTimePart, DATE_FORMATTER);

            // 解析时区偏移
            java.time.ZoneOffset zoneOffset = java.time.ZoneOffset.of(zoneOffsetPart);

            // 创建原始ZonedDateTime
            ZonedDateTime originalZoned = ZonedDateTime.of(localDateTime, zoneOffset);

            // 转换到目标时区
            return originalZoned.withZoneSameInstant(targetZone);

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Failed to parse date time: " + dateTimeStr, e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert time zone for: " + dateTimeStr, e);
        }
    }

    /**
     * 将带时区的时间字符串转换为目标时区的 OffsetDateTime
     *
     * @param dateTimeStr
     * @param targetZone
     * @return
     */
    public static OffsetDateTime convertToTargetZone(String dateTimeStr, ZoneId targetZone) {
        // 处理 +0530 格式
        String[] parts = dateTimeStr.trim().split(" (?=\\+|-)");
        String dateTimePart = parts[0];
        String zoneOffsetPart = parts[1].replace("+", "GMT+").replace("-", "GMT-");

        // 解析原始时间
        OffsetDateTime originalTime = OffsetDateTime.of(
                LocalDateTime.parse(dateTimePart, DATE_FORMATTER),
                ZoneOffset.of(zoneOffsetPart.substring(3)) // 提取时区偏移部分
        );

        // 转换为目标时区
        return originalTime.atZoneSameInstant(targetZone).toOffsetDateTime();
    }

    /**
     * 格式化ZonedDateTime为字符串
     *
     * @param zonedDateTime 时间
     * @return 格式化后的字符串
     */
    public static String formatZonedDateTimeZone(ZonedDateTime zonedDateTime) {
        return zonedDateTime.format(DATE_FORMATTER_Z);
    }

    /**
     * 格式化OffsetDateTime为字符串
     *
     * @param offsetDateTime 时间
     * @return 格式化后的字符串
     */
    public static String formatOffsetDateTimeZone(OffsetDateTime offsetDateTime) {
        return offsetDateTime.format(DATE_FORMATTER_Z);
    }

    /**
     * 智能解析多种时间格式并转换为LocalDateTime（默认使用上海时区）
     * <p>
     * 支持的格式：
     * <ul>
     *   <li>2026-01-10（仅日期，时间默认为00:00:00）</li>
     *   <li>2025-10-10 06:30:46（日期+时间，无时区）</li>
     *   <li>2026-01-01 11:30:01 +0800（日期+时间+时区）</li>
     *   <li>2026-01-01 +0800（日期+时区，时间默认为00:00:00）</li>
     * </ul>
     *
     * @param dateTimeStr 时间字符串
     * @return LocalDateTime，如果输入为空则返回null
     */
    public static LocalDateTime parseToLocalDateTime(String dateTimeStr) {
        return parseToLocalDateTime(dateTimeStr, ZoneId.of("Asia/Shanghai"));
    }

    /**
     * 智能解析多种时间格式并转换为LocalDateTime（指定目标时区）
     * <p>
     * 支持的格式：
     * <ul>
     *   <li>2026-01-10（仅日期，时间默认为00:00:00）</li>
     *   <li>2025-10-10 06:30:46（日期+时间，无时区）</li>
     *   <li>2026-01-01 11:30:01 +0800（日期+时间+时区）</li>
     *   <li>2026-01-01 +0800（日期+时区，时间默认为00:00:00）</li>
     * </ul>
     *
     * @param dateTimeStr 时间字符串
     * @param targetZone  目标时区（对于带时区的输入，会先转换到目标时区）
     * @return LocalDateTime，如果输入为空则返回null
     */
    public static LocalDateTime parseToLocalDateTime(String dateTimeStr, ZoneId targetZone) {
        if (StrUtil.isBlank(dateTimeStr)) {
            return null;
        }

        String trimmed = dateTimeStr.trim();

        try {
            // 情况1: 带时区的格式（包含 + 或 - 符号）
            // 使用正则匹配时区偏移：+0800, -0500 等
            if (trimmed.matches(".*[\\s](\\+|-)\\d{4}$")) {
                String[] parts = trimmed.split("\\s+(?=\\+|-)");

                if (parts.length == 2) {
                    String dateTimePart = parts[0].trim();
                    String zoneOffsetPart = parts[1].trim();

                    LocalDateTime localDateTime;

                    // 判断是否包含时间部分（检查是否有冒号）
                    if (dateTimePart.contains(":")) {
                        // 格式：2026-01-01 11:30:01 +0800
                        localDateTime = LocalDateTime.parse(dateTimePart, DATE_FORMATTER);
                    } else {
                        // 格式：2026-01-01 +0800（仅日期+时区）
                        LocalDate date = LocalDate.parse(dateTimePart);
                        localDateTime = date.atStartOfDay();
                    }

                    // 解析时区偏移
                    ZoneOffset zoneOffset = ZoneOffset.of(zoneOffsetPart);

                    // 创建带时区的时间并转换到目标时区
                    ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, zoneOffset);
                    return zonedDateTime.withZoneSameInstant(targetZone).toLocalDateTime();
                }
            }

            // 情况2: 日期+时间（无时区）
            if (trimmed.contains(":")) {
                // 格式：2025-10-10 06:30:46
                return LocalDateTime.parse(trimmed, DATE_FORMATTER);
            }

            // 情况3: 仅日期
            // 格式：2026-01-10
            LocalDate date = LocalDate.parse(trimmed);
            return date.atStartOfDay();

        } catch (DateTimeParseException e) {
            logger.error("[HeyUtil.parseToLocalDateTime] 时间格式解析失败: {}", dateTimeStr, e);
            throw new IllegalArgumentException("无法解析时间格式: " + dateTimeStr + ", 原因: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("[HeyUtil.parseToLocalDateTime] 时间转换失败: {}", dateTimeStr, e);
            throw new IllegalArgumentException("时间转换失败: " + dateTimeStr + ", 原因: " + e.getMessage(), e);
        }
    }




    /**
     * 构造房间明细列表
     */
    public static List<QTechSearchRequest.RoomDetail> buildQTechRoomDetails(String occupancy) {
        try {
            // 兼容两种结构：rooms 列表或整体成人/儿童
            List<QTechSearchRequest.RoomDetail> result = new ArrayList<>();

            // 入住人信息 2-5-3代表2成人2个儿童（1个5岁，1个3岁） 多间房下滑线_分割
            // 例：2-5_1-3_2-4-6 代表三间房，第一间 2成人1儿童5岁， 第二间 1成人1儿童3岁， 第三间 2成人2儿童4岁和6岁
            if (StrUtil.isNotBlank(occupancy)) {
                String[] roomStrs = occupancy.split("_");
                for (String r : roomStrs) {
                    if (r == null || r.isEmpty()) continue;
                    String[] parts = r.split("-");
                    if (parts.length >= 1) {
                        QTechSearchRequest.RoomDetail d = new QTechSearchRequest.RoomDetail();
                        // 成人数
                        int adults = 0;
                        try {
                            adults = Integer.parseInt(parts[0]);
                        } catch (NumberFormatException ignore) {
                        }
                        d.setNumberOfAdults(adults > 0 ? adults : 2);

                        // 儿童数与年龄
                        if (parts.length > 1) {
                            int children = parts.length - 1;
                            d.setNumberOfChild(children);
                            StringBuilder ages = new StringBuilder();
                            for (int i = 1; i < parts.length; i++) {
                                if (ages.length() > 0) ages.append(',');
                                ages.append(parts[i]);
                            }
                            d.setChildAge(ages.toString());
                        }

                        result.add(d);
                    }
                }
            }

            // 为空默认2 成人
            if (StrUtil.isBlank(occupancy)) {
                QTechSearchRequest.RoomDetail d = new QTechSearchRequest.RoomDetail();
                d.setNumberOfAdults(2);
                result.add(d);
            }
            return result;
        } catch (Exception e) {
            logger.error("构造房间明细失败，使用默认2成人", e);
            QTechSearchRequest.RoomDetail d = new QTechSearchRequest.RoomDetail();
            d.setNumberOfAdults(2);
            return Collections.singletonList(d);
        }
    }


    /**
     * 计算字符串的 SHA-256 十六进制（64位小写）
     * 默认长度64
     */
    public static String sha256Hex(String input, int length) {
        try {
            // 获取一个 SHA-256 的消息摘要实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 计算哈希值
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 字符串转 XEnumCurrency 枚举代码
     *
     * @param str
     * @return
     */
    public static Optional<Integer> toXwCurrencyCode(String str) {
        Optional<Integer> code = Arrays.stream(XEnumCurrency.values())
                .filter(c -> c.name().equalsIgnoreCase(str))
                .map(XEnumCurrency::getCode).findFirst();

        return code;
    }

    /**
     * 字符串转 XEnumCurrency 枚举
     *
     * @param str
     * @return
     */
    public static Optional<XEnumCurrency> toXwCurrency(String str) {
        Optional<XEnumCurrency> code = Arrays.stream(XEnumCurrency.values())
                .filter(c -> c.name().equalsIgnoreCase(str))
                .findFirst();

        return code;
    }


    /**
     * 获取指定日期之后的下一个工作日（周一到周五）
     * 如果给定日期本身就是工作日，则返回该日期
     * 如果给定日期是周末，则返回下一个周一
     *
     * @param date 指定日期
     * @return 下一个工作日
     */
    public static LocalDate nextWeekday(LocalDate date) {
        if (date == null) {
            return LocalDate.now();
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // 如果是工作日（周一到周五），直接返回
        if (dayOfWeek.getValue() >= DayOfWeek.MONDAY.getValue() &&
                dayOfWeek.getValue() <= DayOfWeek.FRIDAY.getValue()) {
            return date;
        }

        // 如果是周六，返回下周一（+2天）
        if (dayOfWeek == DayOfWeek.SATURDAY) {
            return date.plusDays(2);
        }

        // 如果是周日，返回下周一（+1天）
        if (dayOfWeek == DayOfWeek.SUNDAY) {
            return date.plusDays(1);
        }

        // 兜底，理论上不会到这里
        return date;
    }

    /**
     * 获取指定日期之后的下一个周五
     * 如果给定日期本身就是周五，则返回该日期
     *
     * @param date 指定日期
     * @return 下一个周五
     */
    public static LocalDate nextFriday(LocalDate date) {
        if (date == null) {
            return LocalDate.now();
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // 如果已经是周五，直接返回
        if (dayOfWeek == DayOfWeek.FRIDAY) {
            return date;
        }

        // 计算到下一个周五需要的天数
        int daysToAdd = (DayOfWeek.FRIDAY.getValue() - dayOfWeek.getValue() + 7) % 7;
        if (daysToAdd == 0) {
            daysToAdd = 7; // 如果是周五，返回下周五
        }

        return date.plusDays(daysToAdd);
    }

    /**
     * 获取指定日期之后的下一个周六
     * 如果给定日期本身就是周六，则返回该日期
     *
     * @param date 指定日期
     * @return 下一个周六
     */
    public static LocalDate nextSaturday(LocalDate date) {
        if (date == null) {
            return LocalDate.now();
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // 如果已经是周六，直接返回
        if (dayOfWeek == DayOfWeek.SATURDAY) {
            return date;
        }

        // 计算到下一个周六需要的天数
        int daysToAdd = (DayOfWeek.SATURDAY.getValue() - dayOfWeek.getValue() + 7) % 7;
        if (daysToAdd == 0) {
            daysToAdd = 7; // 如果是周六，返回下周六
        }

        return date.plusDays(daysToAdd);
    }

    /**
     * 获取指定日期之后的下一个周末（周五或周六）
     * 优先返回周五，如果给定日期已经是周五之后，则返回周六
     *
     * @param date 指定日期
     * @return 下一个周末日期（周五或周六）
     */
    public static LocalDate nextWeekend(LocalDate date) {
        if (date == null) {
            return LocalDate.now();
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // 如果是周一到周四，返回本周五
        if (dayOfWeek.getValue() >= DayOfWeek.MONDAY.getValue() &&
                dayOfWeek.getValue() <= DayOfWeek.THURSDAY.getValue()) {
            return nextFriday(date);
        }

        // 如果是周五，返回周五（当天）
        if (dayOfWeek == DayOfWeek.FRIDAY) {
            return date;
        }

        // 如果是周六，返回周六（当天）
        if (dayOfWeek == DayOfWeek.SATURDAY) {
            return date;
        }

        // 如果是周日，返回下周五
        if (dayOfWeek == DayOfWeek.SUNDAY) {
            return nextFriday(date);
        }

        // 兜底
        return nextFriday(date);
    }


    /**
     * 生成唯一的订单创建请求 Key
     *
     * @param supplierCode
     * @param hotelId
     * @param roomId
     * @param checkIn
     * @param checkOut
     * @param totalPrice
     * @return
     */
    public static String generateCreateKey(String supplierCode, String hotelId, String roomId, String checkIn, String checkOut, String totalPrice) {
        String keySource = String.join("|",
                supplierCode == null ? "" : supplierCode,
                hotelId == null ? "" : hotelId,
                roomId == null ? "" : roomId,
                checkIn == null ? "" : checkIn,
                checkOut == null ? "" : checkOut,
                totalPrice == null ? "" : totalPrice,
                System.currentTimeMillis() + ""
        );
        return MD5Util.string2MD5(keySource);
    }


    /**
     * 自动检测是否为调试环境
     * 检测规则：本地开发环境或包含debug标识
     */
    public static boolean isDebugEnvironment() {
        // 检测JVM参数
        String debugFlag = System.getProperty("debug.mode");
        if ("true".equalsIgnoreCase(debugFlag)) {
            return true;
        }

        // 检测Spring Profile
        String profiles = System.getProperty("spring.profiles.active");
        if (profiles != null && (profiles.contains("dev") || profiles.contains("debug"))) {
            return true;
        }

        // 检测本地开发环境（用户目录包含常见开发标识）
        String userHome = System.getProperty("user.home");
        if (userHome != null && (userHome.contains("dev") || userHome.contains("Dev") ||
                userHome.contains("developer") || userHome.contains("pbeenig"))) {
            return true;
        }

        // 默认为生产环境
        return false;
    }

    /**
     * 安全获取字符串值
     *
     * @param fields
     * @param key
     * @return
     */
    public static String getStringValue(Map<String, Object> fields, String key) {
        if (fields == null || key == null) {
            return null;
        }
        Object value = fields.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 安全获取BigDecimal值
     */
    public static BigDecimal getBigDecimalValue(Map<String, Object> fields, String key) {
        if (fields == null || key == null) {
            return null;
        }
        Object value = fields.get(key);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            String strValue = String.valueOf(value).trim();
            return StringUtils.hasText(strValue) ? new BigDecimal(strValue) : null;
        } catch (NumberFormatException e) {
            logger.warn("无法转换为BigDecimal，key: {}, value: {}", key, value);
            return null;
        }
    }

    /**
     * 安全获取整数值
     */
    public static int getIntValue(Map<String, Object> fields, String key, int defaultValue) {
        if (fields == null || key == null) {
            return defaultValue;
        }
        Object value = fields.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            String strValue = String.valueOf(value).trim();
            return StringUtils.hasText(strValue) ? Integer.parseInt(strValue) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("无法转换为整数，key: {}, value: {}，使用默认值: {}", key, value, defaultValue);
            return defaultValue;
        }
    }


    /**
     * 安全地从Map中获取Long值
     *
     * @param map 数据源Map
     * @param key 键名
     * @return Long值，如果不存在、为null或转换失败则返回null
     */
    public static Long getLongValue(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }

        Object value = map.get(key);
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            } else {
                String strValue = String.valueOf(value).trim();
                if (strValue.isEmpty()) {
                    return null;
                }
                return Long.valueOf(strValue);
            }
        } catch (NumberFormatException e) {
            logger.warn("无法将值转换为Long: key={}, value={}", key, value, e);
            return null;
        }
    }

    /**
     * 安全地从Map中获取Boolean值
     *
     * @param map 数据源Map
     * @param key 键名
     * @return Boolean值，如果不存在或为null则返回null
     */
    public static Boolean getBooleanValue(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }

        Object value = map.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            String strValue = String.valueOf(value).toLowerCase().trim();
            return "true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue);
        }
    }

    // ======================== 时间参数安全转换工具方法 ========================

    /**
     * 安全地从Map中获取时间字符串并转换为LocalDateTime
     * 适用于包含时间信息的字段
     *
     * @param map 数据源Map
     * @param key 键名
     * @return LocalDateTime对象，如果不存在、为null或解析失败则返回null
     */
    public static LocalDate getDateValue(Map<String, Object> map, String key) {
        String dateTimeStr = getStringValue(map, key);
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            return DateUtil.toLocalDateTime(DateUtil.parse(dateTimeStr)).toLocalDate();
        } catch (Exception e) {
            logger.warn("时间解析失败: key={}, value={}", key, dateTimeStr, e);
            return null;
        }
    }

    /**
     * 安全地从Map中获取时间字符串并转换为LocalDateTime
     * 适用于包含时间信息的字段
     *
     * @param map 数据源Map
     * @param key 键名
     * @return LocalDateTime对象，如果不存在、为null或解析失败则返回null
     */
    public static LocalDateTime getDateTimeValue(Map<String, Object> map, String key) {
        String dateTimeStr = getStringValue(map, key);
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            return DateUtil.toLocalDateTime(DateUtil.parse(dateTimeStr));
        } catch (Exception e) {
            logger.warn("时间解析失败: key={}, value={}", key, dateTimeStr, e);
            return null;
        }
    }

    /**
     * 安全地从Map中获取时间字符串并格式化为标准格式
     * 专门用于API日志记录中的时间字段标准化
     *
     * @param map 数据源Map
     * @param key 键名（如：checkInDate、checkOutDate）
     * @return 标准格式的日期字符串（yyyy-MM-dd），如果解析失败则返回原始字符串
     */
    public static String getFormattedDateValue(Map<String, Object> map, String key) {
        String originalValue = getStringValue(map, key);
        if (originalValue == null || originalValue.trim().isEmpty()) {
            return null;
        }

        LocalDate date = getDateValue(map, key);
        if (date != null) {
            return date.toString(); // 返回标准格式 yyyy-MM-dd
        } else {
            // 如果解析失败，返回原始值（用于调试）
            logger.debug("时间格式化失败，返回原始值: key={}, value={}", key, originalValue);
            return originalValue;
        }
    }

    public static String getFormattedDateTimeValue(Map<String, Object> map, String key) {
        String originalValue = getStringValue(map, key);
        if (originalValue == null || originalValue.trim().isEmpty()) {
            return null;
        }

        LocalDateTime dateTime = getDateTimeValue(map, key);
        if (dateTime != null) {
            return dateTime.toString(); // 返回标准格式 yyyy-MM-dd HH:mm:ss
        } else {
            // 如果解析失败，返回原始值（用于调试）
            logger.debug("时间格式化失败，返回原始值: key={}, value={}", key, originalValue);
            return originalValue;
        }
    }

    /**
     * 验证时间参数的有效性
     *
     * @param checkInDate  入住日期
     * @param checkOutDate 离店日期
     * @return 验证结果，true表示有效
     */
    public static boolean validateDateRange(LocalDateTime checkInDate, LocalDateTime checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            return false;
        }

        // 离店日期必须在入住日期之后
        return checkOutDate.isAfter(checkInDate);
    }

    /**
     * 验证时间参数的有效性（从Map中获取）
     *
     * @param map         数据源Map
     * @param checkInKey  入住日期键名
     * @param checkOutKey 离店日期键名
     * @return 验证结果，true表示有效
     */
    public static boolean validateDateRange(Map<String, Object> map, String checkInKey, String checkOutKey) {
        LocalDateTime checkInDate = getDateTimeValue(map, checkInKey);
        LocalDateTime checkOutDate = getDateTimeValue(map, checkOutKey);
        return validateDateRange(checkInDate, checkOutDate);
    }


    /**
     *  计算两个日期之间的天数差（不包含结束日期）
     * @param startDate
     * @param endDate
     * @return
     */
    public static int daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * 根据Occupancy字符串计算入住总人数（成人+儿童）
     * <p>
     * 入住人信息格式说明：
     * <ul>
     *   <li>2-5-3 代表2成人2个儿童（1个5岁，1个3岁）</li>
     *   <li>多间房用下划线分割，如：2-5_1-3_2-4-6</li>
     *   <li>第一个数字是成人数，后续数字是儿童年龄</li>
     * </ul>
     *
     * @param occupancy 入住人信息字符串
     * @return 总人数（成人+儿童），如果输入为空或格式错误则返回0
     */
    public static int calculateTotalGuests(String occupancy) {
        if (StrUtil.isBlank(occupancy)) {
            return 0;
        }

        int totalGuests = 0;

        try {
            // 按下划线分割多间房
            String[] roomStrs = occupancy.trim().split("_");

            for (String roomStr : roomStrs) {
                if (StrUtil.isBlank(roomStr)) {
                    continue;
                }

                // 按连字符分割成人数和儿童年龄
                String[] parts = roomStr.trim().split("-");
                if (parts.length >= 1) {
                    // 第一个数字是成人数
                    try {
                        int adults = Integer.parseInt(parts[0].trim());
                        totalGuests += Math.max(0, adults); // 确保非负数
                    } catch (NumberFormatException e) {
                        logger.warn("解析成人数失败，occupancy: {}, 房间: {}, 成人部分: {}",
                                occupancy, roomStr, parts[0]);
                    }

                    // 后续数字是儿童年龄，数量即为儿童人数
                    if (parts.length > 1) {
                        int children = parts.length - 1; // 儿童数量 = 总部分数 - 成人部分
                        totalGuests += children;
                    }
                }
            }

            logger.debug("计算入住总人数: occupancy={}, totalGuests={}", occupancy, totalGuests);
            return totalGuests;

        } catch (Exception e) {
            logger.error("计算入住总人数失败，occupancy: {}", occupancy, e);
            return 0;
        }
    }

    /**
     * 根据Occupancy字符串分别计算成人和儿童人数
     *
     * @param occupancy 入住人信息字符串
     * @return 数组，[0]为成人总数，[1]为儿童总数
     */
    public static int[] calculateGuestBreakdown(String occupancy) {
        if (StrUtil.isBlank(occupancy)) {
            return new int[]{0, 0};
        }

        int totalAdults = 0;
        int totalChildren = 0;

        try {
            // 按下划线分割多间房
            String[] roomStrs = occupancy.trim().split("_");

            for (String roomStr : roomStrs) {
                if (StrUtil.isBlank(roomStr)) {
                    continue;
                }

                // 按连字符分割成人数和儿童年龄
                String[] parts = roomStr.trim().split("-");
                if (parts.length >= 1) {
                    // 第一个数字是成人数
                    try {
                        int adults = Integer.parseInt(parts[0].trim());
                        totalAdults += Math.max(0, adults);
                    } catch (NumberFormatException e) {
                        logger.warn("解析成人数失败，occupancy: {}, 房间: {}, 成人部分: {}",
                                occupancy, roomStr, parts[0]);
                    }

                    // 后续数字是儿童年龄，数量即为儿童人数
                    if (parts.length > 1) {
                        int children = parts.length - 1;
                        totalChildren += children;
                    }
                }
            }

            logger.debug("计算入住人数明细: occupancy={}, adults={}, children={}",
                    occupancy, totalAdults, totalChildren);
            return new int[]{totalAdults, totalChildren};

        } catch (Exception e) {
            logger.error("计算入住人数明细失败，occupancy: {}", occupancy, e);
            return new int[]{0, 0};
        }
    }

    public static void main(String[] args) {
        System.out.printf("%s%n", HeyUtil.daysBetween(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 5)));

        // 测试Occupancy计算方法
        System.out.println("=== 测试Occupancy计算方法 ===");

        // 测试用例
        String[] testCases = {
            "2-5-3",         // 2成人2儿童，总计4人
            "2-5_1-3_2-4-6", // 三间房：2+1+1, 1+1, 2+2 = 总计10人
            "1-7",           // 1成人1儿童，总计2人
            "2",             // 2成人，总计2人
            "0"              // 空字符串，总计0人
        };

        for (String testCase : testCases) {
            int totalGuests = HeyUtil.calculateTotalGuests(testCase);
            int[] breakdown = HeyUtil.calculateGuestBreakdown(testCase);
            System.out.printf("Occupancy: %-15s | 总人数: %2d | 成人: %2d | 儿童: %2d%n",
                    "'" + testCase + "'", totalGuests, breakdown[0], breakdown[1]);
        }
    }

}
