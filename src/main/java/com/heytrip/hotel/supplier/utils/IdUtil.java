package com.heytrip.hotel.supplier.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * ID 工具类
 * 提供生成 SHA-256 十六进制字符串的方法
 *
 * @author Pax
 */
public class IdUtil {

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
}
