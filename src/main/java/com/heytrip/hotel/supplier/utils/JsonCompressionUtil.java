package com.heytrip.hotel.supplier.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.*;

/**
 * JSON压缩工具类
 * 支持多种压缩算法对JSON字符串进行压缩和解压缩
 *
 * @author HeyTrip
 * @since 1.0.0
 */
public class JsonCompressionUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonCompressionUtil.class);

    // 压缩标识前缀
    private static final String GZIP_PREFIX = "GZIP:";
    private static final String DEFLATE_PREFIX = "DEFLATE:";
    private static final String LZ4_PREFIX = "LZ4:";

    /**
     * 压缩算法枚举
     */
    public enum CompressionAlgorithm {
        GZIP("gzip"),
        DEFLATE("deflate"),
        LZ4("lz4");

        private final String name;

        CompressionAlgorithm(String name) {
            this.name = name;
        }

        public static CompressionAlgorithm fromString(String name) {
            for (CompressionAlgorithm algorithm : values()) {
                if (algorithm.name.equalsIgnoreCase(name)) {
                    return algorithm;
                }
            }
            return GZIP; // 默认使用GZIP
        }
    }

    /**
     * 智能压缩JSON字符串
     * 根据长度阈值决定是否压缩
     *
     * @param jsonContent JSON字符串
     * @param threshold 压缩阈值
     * @param algorithm 压缩算法
     * @param level 压缩级别
     * @return 压缩后的字符串（如果未达到阈值则返回原字符串）
     */
    public static String compressIfNeeded(String jsonContent, int threshold,
                                        CompressionAlgorithm algorithm, int level) {
        if (!StringUtils.hasText(jsonContent)) {
            return jsonContent;
        }

        int originalLength = jsonContent.length();

        // 添加详细的调试日志
        logger.debug("压缩检查: 内容长度={}, 压缩阈值={}, 算法={}",
                    originalLength, threshold, algorithm.name);

        // 如果长度未超过阈值，直接返回
        if (originalLength <= threshold) {
            logger.debug("内容长度未超过压缩阈值，跳过压缩");
            return jsonContent;
        }

        try {
            logger.debug("开始压缩JSON内容，算法={}, 级别={}", algorithm.name, level);
            String compressed = compress(jsonContent, algorithm, level);

            // 记录压缩效果
            int compressedSize = compressed.length();
            double compressionRatio = (double) compressedSize / originalLength;
            double savedPercentage = (1 - compressionRatio) * 100;

            logger.info("JSON压缩完成: 原始大小={}字符, 压缩后={}字符, 节省={:.1f}%, 算法={}",
                       originalLength, compressedSize, savedPercentage, algorithm.name);

            return compressed;

        } catch (Exception e) {
            logger.warn("JSON压缩失败，返回原始内容: 原始长度={}, 错误={}", originalLength, e.getMessage(), e);
            return jsonContent;
        }
    }

    /**
     * 压缩JSON字符串
     *
     * @param jsonContent JSON字符串
     * @param algorithm 压缩算法
     * @param level 压缩级别
     * @return 压缩后的Base64编码字符串（带算法标识前缀）
     */
    public static String compress(String jsonContent, CompressionAlgorithm algorithm, int level)
            throws IOException {
        if (!StringUtils.hasText(jsonContent)) {
            return jsonContent;
        }

        byte[] inputBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
        byte[] compressedBytes;
        String prefix;

        switch (algorithm) {
            case GZIP:
                compressedBytes = compressWithGzip(inputBytes, level);
                prefix = GZIP_PREFIX;
                break;
            case DEFLATE:
                compressedBytes = compressWithDeflate(inputBytes, level);
                prefix = DEFLATE_PREFIX;
                break;
            case LZ4:
                // LZ4压缩需要额外依赖，这里使用DEFLATE作为fallback
                logger.debug("LZ4压缩暂未实现，使用DEFLATE算法");
                compressedBytes = compressWithDeflate(inputBytes, level);
                prefix = DEFLATE_PREFIX;
                break;
            default:
                compressedBytes = compressWithGzip(inputBytes, level);
                prefix = GZIP_PREFIX;
        }

        // Base64编码并添加算法标识前缀
        String base64Compressed = Base64.getEncoder().encodeToString(compressedBytes);
        return prefix + base64Compressed;
    }

    /**
     * 解压缩JSON字符串
     *
     * @param compressedContent 压缩后的字符串（带算法标识前缀）
     * @return 解压缩后的JSON字符串
     */
    public static String decompress(String compressedContent) throws IOException {
        if (!StringUtils.hasText(compressedContent)) {
            return compressedContent;
        }

        // 检查是否为压缩内容
        if (!isCompressed(compressedContent)) {
            return compressedContent;
        }

        try {
            // 识别压缩算法并解压
            if (compressedContent.startsWith(GZIP_PREFIX)) {
                String base64Data = compressedContent.substring(GZIP_PREFIX.length());
                byte[] compressedBytes = Base64.getDecoder().decode(base64Data);
                return decompressWithGzip(compressedBytes);

            } else if (compressedContent.startsWith(DEFLATE_PREFIX)) {
                String base64Data = compressedContent.substring(DEFLATE_PREFIX.length());
                byte[] compressedBytes = Base64.getDecoder().decode(base64Data);
                return decompressWithDeflate(compressedBytes);

            } else if (compressedContent.startsWith(LZ4_PREFIX)) {
                // LZ4解压缩
                String base64Data = compressedContent.substring(LZ4_PREFIX.length());
                byte[] compressedBytes = Base64.getDecoder().decode(base64Data);
                return decompressWithDeflate(compressedBytes); // 使用DEFLATE作为fallback

            } else {
                logger.warn("未识别的压缩格式: {}", compressedContent.substring(0, Math.min(20, compressedContent.length())));
                return compressedContent;
            }

        } catch (Exception e) {
            logger.error("JSON解压缩失败: {}", e.getMessage(), e);
            throw new IOException("JSON解压缩失败", e);
        }
    }

    /**
     * 检查字符串是否为压缩内容
     */
    public static boolean isCompressed(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }

        return content.startsWith(GZIP_PREFIX) ||
               content.startsWith(DEFLATE_PREFIX) ||
               content.startsWith(LZ4_PREFIX);
    }

    /**
     * GZIP压缩
     */
    private static byte[] compressWithGzip(byte[] input, int level) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos) {{
                 def.setLevel(level);
             }}) {

            gzipOut.write(input);
            gzipOut.finish();
            return baos.toByteArray();
        }
    }

    /**
     * GZIP解压缩
     */
    private static String decompressWithGzip(byte[] compressed) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }

            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Deflate压缩
     */
    private static byte[] compressWithDeflate(byte[] input, int level) throws IOException {
        Deflater deflater = new Deflater(level);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DeflaterOutputStream deflateOut = new DeflaterOutputStream(baos, deflater)) {

            deflateOut.write(input);
            deflateOut.finish();
            return baos.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /**
     * Deflate解压缩
     */
    private static String decompressWithDeflate(byte[] compressed) throws IOException {
        Inflater inflater = new Inflater();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
             InflaterInputStream inflateIn = new InflaterInputStream(bais, inflater);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inflateIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }

            return baos.toString(StandardCharsets.UTF_8);
        } finally {
            inflater.end();
        }
    }

    /**
     * 计算压缩率
     *
     * @param originalSize 原始大小
     * @param compressedSize 压缩后大小
     * @return 压缩率百分比
     */
    public static double calculateCompressionRatio(int originalSize, int compressedSize) {
        if (originalSize == 0) {
            return 0.0;
        }
        return (1.0 - (double) compressedSize / originalSize) * 100;
    }
}
