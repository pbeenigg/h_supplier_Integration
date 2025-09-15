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
            String nextLine = readNextLine();

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
                nextLine = readNextLine();
                return row;
            }

            private String readNextLine() {
                try {
                    return reader.readLine();
                } catch (IOException e) {
                    logger.warn("读取CSV行失败: {}", e.getMessage());
                    return null;
                }
            }
        };
    }

    /**
     * 解析CSV行，支持双引号包裹和转义
     * @param line
     * @return
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
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }
}
