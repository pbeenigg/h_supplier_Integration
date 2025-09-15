package com.heytrip.hotel.supplier.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FTP 客户端可调参数（支持 application.yml 配置）
 * 
 * 配置示例：
 * ftp:
 *   client:
 *     window-size: 6
 *     step: 5
 *     retry-count: 3
 *     backoff-seconds: 10
 *     buffer-size: 65536         # FTPClient bufferSize（字节）
 *     stream-buffer-size: 131072 # 读取流的缓冲大小（字节）
 *     data-timeout-millis: 300000
 *     connect-timeout-millis: 30000
 *     control-keep-alive-seconds: 60
 */
@Configuration
@ConfigurationProperties(prefix = "ftp.client")
@Data
public class FtpClientConfig {

    // 日期目录滑动窗口
    private int windowSize = 6;
    private int step = 5;

    // 重试与退避
    private int retryCount = 3;
    private long backoffSeconds = 10L;

    // FTPClient 底层参数
    private int bufferSize = 64 * 1024;           // 64KB
    private int streamBufferSize = 128 * 1024;    // 128KB
    private int dataTimeoutMillis = 5 * 60 * 1000; // 5分钟
    private int connectTimeoutMillis = 30 * 1000;  // 30秒
    private int controlKeepAliveSeconds = 60;       // 60秒

    /**
     * 本地读取时，未显式指定 classpath: 前缀时，默认尝试的 classpath 目录前缀
     * 例如："csv/" -> 将优先尝试从 classpath:csv/{addr} 读取
     */
    private String localClasspathPrefix = "csv/";

    /**
     * FTP 下载落地临时文件目录（用于大文件流式下载时的中转目录）
     * 默认 tmp/，如不可写将自动回退到系统临时目录
     */
    private String tempDir = "tmp/";


}
