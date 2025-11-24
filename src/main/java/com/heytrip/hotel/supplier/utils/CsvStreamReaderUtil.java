package com.heytrip.hotel.supplier.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 简易 CSV 流式读取器（UTF-8），支持首行 Header，按列名取值。
 * 说明：
 * - 仅支持逗号分隔，双引号包裹，双引号转义为两个双引号的常见格式。
 * - 为避免引入第三方 CSV 依赖，采用轻量实现，满足供应商静态文件读取需求。
 */
public class CsvStreamReaderUtil implements Closeable, Iterable<Map<String, String>> {

    private static final Logger logger = LoggerFactory.getLogger(CsvStreamReaderUtil.class);

    private final BufferedReader reader;
    private final List<String> headers;

    public CsvStreamReaderUtil(InputStream is) throws IOException {
        this.reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String headerLine = this.reader.readLine();
        if (headerLine == null) {
            throw new IOException("CSV 文件为空");
        }
        this.headers = parseCsvLine(headerLine);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }


    /**
     * 返回行迭代器，每行按列名映射为 Map
     */
    @Override
    public Iterator<Map<String, String>> iterator() {
        return new Iterator<>() {
            String nextLine = readNextCsvRecord();

            @Override
            public boolean hasNext() {
                return nextLine != null;
            }

            @Override
            public Map<String, String> next() {
                if (nextLine == null) throw new NoSuchElementException();
                List<String> values = parseCsvLine(nextLine);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String key = headers.get(i).trim();
                    String val = i < values.size() ? values.get(i) : null;
                    row.put(key, val);
                }
                nextLine = readNextCsvRecord();
                return row;
            }

            /**
             * 读取下一条完整的CSV记录（支持字段内换行符）
             */
            private String readNextCsvRecord() {
                try {
                    StringBuilder record = new StringBuilder();
                    String line;
                    boolean inQuotes = false;
                    int quoteCount = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        if (record.length() > 0) {
                            record.append('\n'); // 保留字段内的换行符
                        }
                        record.append(line);
                        
                        // 统计当前累积记录中的双引号数量（排除转义的双引号）
                        String temp = record.toString();
                        quoteCount = 0;
                        for (int i = 0; i < temp.length(); i++) {
                            if (temp.charAt(i) == '"') {
                                // 检查是否是转义的双引号
                                if (i + 1 < temp.length() && temp.charAt(i + 1) == '"') {
                                    i++; // 跳过转义的引号
                                } else {
                                    quoteCount++;
                                }
                            }
                        }
                        
                        // 如果双引号数量为偶数，说明所有引号都已配对，记录完整
                        if (quoteCount % 2 == 0) {
                            return record.toString();
                        }
                        // 否则继续读取下一行
                    }
                    
                    // 文件结束，如果有未完成的记录，返回它
                    if (record.length() > 0) {
                        logger.warn("CSV记录未正确闭合，可能存在格式问题: {}", 
                            record.substring(0, Math.min(100, record.length())));
                        return record.toString();
                    }
                    
                    return null;
                } catch (IOException e) {
                    logger.error("读取CSV记录失败: {}", e.getMessage(), e);
                    return null;
                }
            }
        };
    }

    /**
     * 解析CSV行，支持双引号包裹和转义
     * @param line CSV记录（可能包含多行）
     * @return 解析后的字段列表
     */
    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null) return tokens;
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(normalizeFieldValue(sb.toString()));
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(normalizeFieldValue(sb.toString()));
        return tokens;
    }
    
    /**
     * 规范化字段值：清理多余的换行符和空白字符
     * @param value 原始字段值
     * @return 规范化后的值
     */
    private String normalizeFieldValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        // 将字段内的换行符替换为空格，避免数据库存储问题
        // 如果需要保留换行符，可以注释掉这行
        value = value.replace('\n', ' ').replace('\r', ' ');
        // 清理多余的连续空格
        value = value.replaceAll("\\s+", " ");
        return value.trim();
    }
}
