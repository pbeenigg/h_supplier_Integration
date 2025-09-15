package com.heytrip.hotel.supplier.client;

import com.heytrip.hotel.supplier.config.FtpClientConfig;
import jakarta.annotation.Resource;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 简单FTP客户端（仅支持 FTP，不支持 SFTP）。
 * 功能：带重试的下载文件为 InputStream（调用方负责关闭）。
 */
@Component
public class FtpClientService {

    private static final Logger logger = LoggerFactory.getLogger(FtpClientService.class);

    @Resource
    private FtpClientConfig ftpProps;

    /**
     * 从 FTP 下载文件（带最多3次重试，10秒指数退避）。
     * @param host 主机
     * @param port 端口
     * @param username 用户名
     * @param password 密码
     * @param remotePath 远程路径（包含文件名）
     * @return 输入流（调用方关闭），失败返回 null
     */
    public InputStream downloadWithRetry(String host, int port, String username, String password, String remotePath) {
        int maxRetries = ftpProps.getRetryCount();
        long backoffSeconds = ftpProps.getBackoffSeconds();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                InputStream is = downloadSmart(host, port, username, password, remotePath);
                if (is != null) {
                    return is;
                }
            } catch (Exception e) {
                logger.warn("FTP 下载失败，第{}次，路径:{}，原因:{}", attempt, remotePath, e.getMessage());
            }
            try {
                Thread.sleep(Duration.ofSeconds(backoffSeconds * attempt).toMillis());
            } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        return null;
    }

    /**
     * 智能下载：
     * 1) 直接按 remotePath 读取
     * 2) 若失败：在 remotePath 同目录按文件名搜索
     * 3) 若仍失败：遍历“remotePath 所在目录”下的日期文件夹(yyyy-MM-dd)按时间降序，在其中搜索同名文件
     */
    private InputStream downloadSmart(String host, int port, String username, String password, String remotePath) throws IOException {
        FTPClient ftp = new FTPClient();
        try {
            ftp.setControlEncoding(StandardCharsets.UTF_8.name());
            ftp.connect(host, port);
            if (!ftp.login(username, password)) {
                throw new IOException("FTP 登录失败");
            }
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            // 大文件稳定性配置（可调）
            ftp.setBufferSize(ftpProps.getBufferSize());
            ftp.setDataTimeout(ftpProps.getDataTimeoutMillis());
            ftp.setConnectTimeout(ftpProps.getConnectTimeoutMillis());
            ftp.setControlKeepAliveTimeout(ftpProps.getControlKeepAliveSeconds());

            // 1) 直接尝试远程路径
            InputStream direct = tryRetrieve(ftp, remotePath);
            if (direct != null) return direct;

            // 解析目录与文件名
            String dir = extractDir(remotePath);
            String fileName = extractFileName(remotePath);

            // 2) 在同目录按文件名搜索
            InputStream fromSameDir = tryFindInDirectory(ftp, dir, fileName);
            if (fromSameDir != null) return fromSameDir;

            // 3) 在同级目录的日期文件夹(yyyy-MM-dd)按时间降序搜索
            InputStream fromDateFolders = tryFindInDateFolders(ftp, dir, fileName);
            if (fromDateFolders != null) return fromDateFolders;

            // 4) 根目录直接搜索（最后兜底）
            InputStream fromRoot = tryFindInDirectory(ftp, "/", fileName);
            if (fromRoot != null) return fromRoot;

            throw new FileNotFoundException("FTP 未找到目标文件：" + remotePath + "（同目录与日期目录均未命中）");
        } finally {
            if (ftp.isConnected()) {
                try { ftp.logout(); } catch (Exception ignored) {}
                try { ftp.disconnect(); } catch (Exception ignored) {}
            }
        }
    }


    /**
     * 尝试直接按远程路径拉取文件
     * @param ftp
     * @param remotePath
     * @return
     */
    private InputStream tryRetrieve(FTPClient ftp, String remotePath) {
        File tempFile = null;
        long start = System.currentTimeMillis();
        try {
            String name = extractFileName(remotePath);
            String suffix = "_" + (name == null ? "download" : name);
            // 选择可配置临时目录
            File tmpDir = null;
            try {
                if (ftpProps != null && ftpProps.getTempDir() != null && !ftpProps.getTempDir().isBlank()) {
                    tmpDir = new File(ftpProps.getTempDir());
                    if (!tmpDir.exists()) {
                        boolean mk = tmpDir.mkdirs();
                        if (!mk) {
                            logger.debug("创建临时目录失败: {}，将回退系统临时目录", tmpDir.getAbsolutePath());
                            tmpDir = null;
                        }
                    }
                    if (tmpDir != null && (!tmpDir.isDirectory() || !tmpDir.canWrite())) {
                        logger.debug("临时目录不可写: {}，将回退系统临时目录", tmpDir.getAbsolutePath());
                        tmpDir = null;
                    }
                }
            } catch (Exception ex) {
                logger.debug("检查临时目录异常: {}", ex.getMessage());
                tmpDir = null;
            }

            if (tmpDir != null) {
                tempFile = File.createTempFile("ftp_", suffix, tmpDir);
                logger.info("[FTP临时目录] 使用配置目录: {}", tmpDir.getAbsolutePath());
            } else {
                tempFile = File.createTempFile("ftp_", suffix);
                logger.info("[FTP临时目录] 使用系统临时目录: {}", tempFile.getParent());
            }
            try (InputStream in = ftp.retrieveFileStream(remotePath);
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                if (in == null) {
                    return null;
                }
                byte[] buf = new byte[Math.max(8 * 1024, ftpProps.getStreamBufferSize())];
                int n;
                long total = 0;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    total += n;
                }
                out.flush();
                boolean completed = ftp.completePendingCommand();
                if (completed) {
                    long cost = System.currentTimeMillis() - start;
                    double speed = cost > 0 ? ((double) total) / cost : -1; // bytes/ms
                    logger.info("[FTP下载完成] path={}, size={} bytes, cost={} ms, avgSpeed={} bytes/ms, tmp=\"{}\"",
                            remotePath, total, cost, String.format(java.util.Locale.ROOT, "%.2f", speed), tempFile.getAbsolutePath());
                    // 这里不再在 close() 时删除临时文件，统一由同步完成后的目录清理负责
                    return new BufferedInputStream(new FileInputStream(tempFile));
                }
            }
            // 未完成或失败
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                logger.debug("清理失败(未完成): {}", tempFile.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.debug("直接拉取失败: {} -> {}", remotePath, e.getMessage());
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                logger.debug("清理失败(异常): {}", tempFile.getAbsolutePath());
            }
        }
        return null;
    }

    private InputStream tryFindInDirectory(FTPClient ftp, String dir, String fileName) throws IOException {
        if (dir == null || dir.isBlank()) dir = "/";
        if (!ftp.changeWorkingDirectory(dir)) {
            logger.debug("切换目录失败: {}", dir);
            return null;
        }
        FTPFile[] files = ftp.listFiles();
        if (files == null) return null;
        for (FTPFile f : files) {
            if (f.isFile() && f.getName().equals(fileName)) {
                String path = dir.endsWith("/") ? (dir + fileName) : (dir + "/" + fileName);
                return tryRetrieve(ftp, path);
            }
        }
        return null;
    }

    private InputStream tryFindInDateFolders(FTPClient ftp, String baseDir, String fileName) throws IOException {
        if (baseDir == null || baseDir.isBlank()) baseDir = "/";
        if (!ftp.changeWorkingDirectory(baseDir)) {
            logger.debug("切换目录失败: {}", baseDir);
            return null;
        }
        FTPFile[] children = ftp.listFiles();
        if (children == null || children.length == 0) return null;

        // 收集日期型文件夹
        List<String> dateDirs = new ArrayList<>();
        for (FTPFile c : children) {
            if (c.isDirectory() && isDateFolder(c.getName())) {
                dateDirs.add(c.getName());
            }
        }
        if (dateDirs.isEmpty()) return null;

        // 按日期降序
        dateDirs.sort((a, b) -> parseDate(b).compareTo(parseDate(a)));

        // 策略调整：按日期降序分窗口查找
        // 仅遍历前6个目录；若未命中，窗口右移5个目录继续，直到结束
        int windowSize = ftpProps.getWindowSize();
        int step = ftpProps.getStep();
        for (int start = 0; start < dateDirs.size(); start += step) {
            int end = Math.min(start + windowSize, dateDirs.size());
            if (start >= end) break;
            for (int i = start; i < end; i++) {
                String d = dateDirs.get(i);
                String dirPath = baseDir.endsWith("/") ? (baseDir + d) : (baseDir + "/" + d);
                if (!ftp.changeWorkingDirectory(dirPath)) {
                    continue;
                }
                FTPFile[] files = ftp.listFiles();
                if (files == null) continue;
                for (FTPFile f : files) {
                    if (f.isFile() && f.getName().equals(fileName)) {
                        String fullPath = dirPath.endsWith("/") ? (dirPath + fileName) : (dirPath + "/" + fileName);
                        // 命中日志：目录与文件时间
                        String tsStr = (f.getTimestamp() != null) ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(f.getTimestamp().getTime()) : "未知";
                        logger.info("[FTP命中] 目录={}, 文件={}, 修改时间={}", dirPath, fileName, tsStr);
                        return tryRetrieve(ftp, fullPath);
                    }
                }
            }
        }
        return null;
    }

    private boolean isDateFolder(String name) {
        return parseDateOptional(name) != null;
    }

    private LocalDate parseDate(String name) {
        LocalDate d = parseDateOptional(name);
        if (d == null) throw new DateTimeParseException("非日期目录", name, 0);
        return d;
    }

    private LocalDate parseDateOptional(String name) {
        if (name == null) return null;
        List<DateTimeFormatter> fmts = Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyyMMdd"),
                DateTimeFormatter.ofPattern("yyyy_MM_dd"),
                DateTimeFormatter.ofPattern("dd_MM_yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("ddMMyyyy")
        );
        for (DateTimeFormatter fmt : fmts) {
            try {
                return LocalDate.parse(name, fmt);
            } catch (DateTimeParseException ignore) {}
        }
        return null;
    }

    /**
     * 提取远程路径中的目录部分，若无目录则返回根目录"/"
     * 示例：/folder/sub/file.csv -> /folder/sub
     */
    private String extractDir(String remotePath) {
        if (remotePath == null || remotePath.isBlank()) return "/";
        int idx = remotePath.lastIndexOf('/');
        if (idx <= 0) return "/"; // 无前置目录或仅为根
        return remotePath.substring(0, idx);
    }

    /**
     * 提取远程路径中的文件名部分
     * 示例：/folder/sub/file.csv -> file.csv
     */
    private String extractFileName(String remotePath) {
        if (remotePath == null || remotePath.isBlank()) return remotePath;
        int idx = remotePath.lastIndexOf('/');
        if (idx < 0 || idx == remotePath.length() - 1) return remotePath;
        return remotePath.substring(idx + 1);
    }
}
