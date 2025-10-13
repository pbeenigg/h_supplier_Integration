package com.heytrip.hotel.supplier.utils;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.exception.BusinessException;
import org.slf4j.Logger;

import java.security.MessageDigest;


/**
 * 签名工具类
 * 提供生成MD5签名的方法
 *
 * @author  Pax
 */
public class SignUtil {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SignUtil.class);


    /**
     * 生成MD5签名
     */
    public static String generateSignature(String appId, String timestamp, String secretKey) {
        try {

            String data = StrUtil.format("app{}secret{}timestamp{}", appId, secretKey,timestamp);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(data.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("生成签名时出错! ", e);
            throw  BusinessException.signError("签名生成失败!");
        }
    }
}
