package com.heytrip.hotel.supplier.utils;

import cn.hutool.core.util.StrUtil;
import com.heytrip.common.enums.XEnumCurrency;
import com.heytrip.hotel.supplier.dto.qtech.req.QTechSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            // 兜底：异常时直接截断或返回原字符串的前64位
            String s = input == null ? "" : input;
            return s.length() <= 64 ? s : s.substring(0, 64);
        }
    }

    /**
     * 字节数组转十六进制小写字符串
     */
    public static String toHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
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
     * @param supplierCode
     * @param hotelId
     * @param roomId
     * @param checkIn
     * @param checkOut
     * @param totalPrice
     * @return
     */
    public  static  String  generateCreateKey( String supplierCode, String hotelId, String roomId, String checkIn, String checkOut, String totalPrice){
        String keySource = String.join("|",
                supplierCode == null ? "" : supplierCode,
                hotelId == null ? "" : hotelId,
                roomId == null ? "" : roomId,
                checkIn == null ? "" : checkIn,
                checkOut == null ? "" : checkOut,
                totalPrice == null ? "" : totalPrice,
                System.currentTimeMillis() + ""
        );
        return sha256Hex(keySource);
    }
}
