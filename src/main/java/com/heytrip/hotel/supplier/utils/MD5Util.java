package com.heytrip.hotel.supplier.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MD5Util {

    /**
     * 16位 原加密密文
     */
    protected static char[] HEXDIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    /**
     * 摘要密文
     */
    protected static MessageDigest MESSAGEDIGEST = null;

    static {
        try {
            // 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）
            MESSAGEDIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String bufferToHex(byte[] bytes) {
        return bufferToHex(bytes, 0, bytes.length);
    }

    private static String bufferToHex(byte[] bytes, int m, int n) {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++) {
            appendHexPair(bytes[l], stringbuffer);
        }
        return stringbuffer.toString();
    }

    private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
        char c0 = HEXDIGITS[(bt & 0xf0) >> 4];
        char c1 = HEXDIGITS[bt & 0xf];
        stringbuffer.append(c0);
        stringbuffer.append(c1);
    }


    /**
     * 字符串的md5加密
     *
     * @param input 需要加密的字符串
     * @return 字符串加密MD5密钥
     */
    public synchronized static String string2MD5(String input) {
        try {
            // 输入的字符串转换成字节数组
            byte[] inputByteArray = input.getBytes();
            MESSAGEDIGEST.update(inputByteArray);
            // 转换并返回结果
            byte[] resultByteArray = MESSAGEDIGEST.digest();
            // 字符数组转换成字符串
            return bufferToHex(resultByteArray);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

