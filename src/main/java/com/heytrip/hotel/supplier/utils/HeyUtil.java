package com.heytrip.hotel.supplier.utils;

import cn.hutool.core.util.StrUtil;
import com.heytrip.common.enums.XEnumCurrency;
import com.heytrip.hotel.supplier.dto.qtech.req.QTechSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
     * @param dateTimeStr  输入时间字符串，格式：2025-12-29 03:00:01 +0530
     * @return 转换后的时间
     */
    public static ZonedDateTime convertTimeZone(String dateTimeStr) {
        return convertTimeZone(dateTimeStr,  ZoneId.of("Asia/Shanghai"));
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

    public static void main(String[] args) {
        // 测试数据
        String startTime = "2025-12-29 03:00:01 +0530";
        String endTime = "2026-01-04 05:30:00 +0530";

        // 你所在时区（示例：上海）
        //ZoneId targetZone = ZoneId.of("Asia/Shanghai");
        // 或者使用系统默认时区
        ZoneId targetZone = ZoneId.systemDefault();

        try {
            // 转换开始时间
            ZonedDateTime startConverted = convertTimeZone(startTime, targetZone);
            System.out.println("原始开始时间: " + startTime);
            System.out.println("转换后开始时间: " +
                    formatZonedDateTimeZone(startConverted));

            // 转换结束时间
            ZonedDateTime endConverted = convertTimeZone(endTime, targetZone);
            System.out.println("原始结束时间: " + endTime);
            System.out.println("转换后结束时间: " +
                    formatZonedDateTimeZone(endConverted));


            // 转换开始时间
            OffsetDateTime startConverted2 = convertToTargetZone(startTime, targetZone);
            System.out.println("转换后开始时间2: " + formatOffsetDateTimeZone(startConverted2));

            // 转换结束时间
            OffsetDateTime endConverted2 = convertToTargetZone(endTime, targetZone);
            System.out.println("转换后结束时间2: " + formatOffsetDateTimeZone(endConverted2));

        } catch (Exception e) {
            System.err.println("转换失败: " + e.getMessage());
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
}
