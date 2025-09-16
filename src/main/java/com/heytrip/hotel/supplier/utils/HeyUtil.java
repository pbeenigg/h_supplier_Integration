package com.heytrip.hotel.supplier.utils;

import com.heytrip.common.enums.XEnumCurrency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Optional;

/**
 * ID 工具类
 * 提供生成 SHA-256 十六进制字符串的方法
 *
 * @author Pax
 */
public class HeyUtil {

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
     * 字符串非空检查
     *
     * @param s
     * @return
     */
    public static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * 字符串空检查
     *
     * @param s
     * @return
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 字符串安全处理，null转为空字符串
     *
     * @param s
     * @return
     */
    public static String safe(String s) {
        return s == null ? "" : s;
    }
}
