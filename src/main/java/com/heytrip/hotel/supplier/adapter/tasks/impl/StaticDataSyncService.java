package com.heytrip.hotel.supplier.adapter.tasks.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.adapter.parser.StaticDataParser;
import com.heytrip.hotel.supplier.client.FtpClientService;
import com.heytrip.hotel.supplier.config.CacheEvictor;
import com.heytrip.hotel.supplier.config.FtpClientConfig;
import com.heytrip.hotel.supplier.constant.SyncBusinessTypeNames;
import com.heytrip.hotel.supplier.dto.supplier.SupplierFtp;
import com.heytrip.hotel.supplier.entity.*;
import com.heytrip.hotel.supplier.repository.*;
import com.heytrip.hotel.supplier.utils.CsvStreamReaderUtil;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.function.Supplier;

import static com.heytrip.hotel.supplier.constant.SyncBusinessTypeNames.*;

/**
 * 静态数据同步服务
 * - 读取 SupplierConfig.ftpConfig
 * - 通过 FTP 下载 CSV / 或从本地 classpath 读取 GIATA
 * - 解析并批量 upsert
 * - 记录 static_sync_log
 */
@Service
public class StaticDataSyncService {
    private static final Logger logger = LoggerFactory.getLogger(StaticDataSyncService.class);

    @Resource private SupplierConfigRepository supplierConfigRepository;
    @Resource private CountryRepository countryRepo;
    @Resource private CityRepository cityRepo;
    @Resource private HotelRepository hotelRepo;
    @Resource private NationalityRepository nationalityRepo;
    @Resource private HotelGiataRepository giataRepo;
    @Resource private SyncLogRepository syncLogRepo;
    @Resource private CacheEvictor cacheEvictor;

    @Resource private FtpClientService ftpClientService;
    @Resource private FtpClientConfig ftpClientConfig;
    @Resource private StaticDataParser aoStaticDataParser; // 仅先支持 AO

    @PersistenceContext
    private EntityManager entityManager;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 同步指定供应商的全部静态数据
     */
    @Transactional
    public void syncAllForSupplier(Long supplierId, String supplierCode) {
        SupplierConfig sc = supplierConfigRepository.findById(supplierId).orElse(null);
        if (sc == null || sc.getFtpConfig() == null) {
            logger.warn("供应商{} 无 FTP 配置，跳过静态同步", supplierId);
            return;
        }
        SupplierFtp ftp = parseFtpConfig(sc.getFtpConfig());
        logger.info("开始静态数据同步，supplierId={}, supplierCode={}", supplierId, supplierCode);
        syncOne(() -> syncCountries(ftp, supplierId, supplierCode), supplierId, supplierCode, COUNTRIES, ftp.getCountriesPath());
        syncOne(() -> syncCities(ftp, supplierId, supplierCode), supplierId, supplierCode, CITIES, ftp.getCitiesPath());
        syncOne(() -> syncNationalities(ftp, supplierId, supplierCode), supplierId, supplierCode, NATIONALITY, ftp.getNationalityPath());
        syncOne(() -> syncGiata(ftp, supplierId, supplierCode), supplierId, supplierCode, GIATA, ftp.getGiataLocalPath());
        syncOne(() -> syncHotels(ftp, supplierId, supplierCode), supplierId, supplierCode, HOTELS, ftp.getHotelsPath());
        logger.info("静态数据同步完成，supplierId={}, supplierCode={}", supplierId, supplierCode);
        // 同步完成后，清理静态数据相关缓存，避免读取到陈旧数据
        try {
            cacheEvictor.evictAllStaticCaches();
        } catch (Exception e) {
            logger.warn("清理静态缓存失败: {}", e.getMessage());
        }
        // 同步完成后，清理FTP临时目录下的所有文件
        try {
            cleanTempDir();
        } catch (Exception e) {
            logger.warn("清理FTP临时目录失败: {}", e.getMessage());
        }
    }


    /**
     * 同步单个业务数据，并记录日志
     * @param task
     * @param supplierId
     * @param supplierCode
     * @param biz
     * @param fileName
     */
    private void syncOne(Supplier<SyncStats> task, Long supplierId, String supplierCode, String biz, String fileName) {
        SyncLog log = new SyncLog();
        log.setSupplierId(supplierId);
        log.setSupplierCode(supplierCode);
        log.setBusinessType(biz);
        log.setFileName(fileName);
        log.setStartTime(LocalDateTime.now());
        log.setIsSuccess(false);
        log.setTotalCount(0L);
        log.setSuccessCount(0L);
        log.setSkipCount(0L);
        log.setErrorCount(0L);
        try {
            SyncStats stats = task.get();
            if (stats != null) {
                log.setTotalCount(stats.total);
                log.setSuccessCount(stats.success);
                log.setSkipCount(stats.skip);
                log.setErrorCount(stats.error);
                if (stats.errorMessage != null && !stats.errorMessage.isBlank()) {
                    // 将批量写入阶段的错误信息附加到日志
                    String existed = log.getMessage();
                    if (existed == null || existed.isBlank()) {
                        log.setMessage(stats.errorMessage);
                    } else {
                        log.setMessage(existed + " | " + stats.errorMessage);
                    }
                }
            }
            log.setIsSuccess(true);
        } catch (Exception e) {
            log.setMessage(e.getMessage());
            logger.warn("同步 {} 失败: {}", biz, e.getMessage());
        } finally {
            log.setEndTime(LocalDateTime.now());
            syncLogRepo.save(log);
        }
    }


    /**
     * 同步国家数据
     * @param ftp
     * @param supplierId
     * @param supplierCode
     */
    private SyncStats syncCountries(SupplierFtp ftp, Long supplierId, String supplierCode) {
        try (InputStream is = openByConfig(ftp, ftp.getCountriesPath(), true);
             CsvStreamReaderUtil reader = new CsvStreamReaderUtil(is)) {
            return batchUpsertCountries(readAll(reader), supplierId, supplierCode);
        } catch (Exception e) {
            throw new RuntimeException("同步国家失败", e);
        }
    }

    /**
     * 同步城市数据
     * @param ftp
     * @param supplierId
     * @param supplierCode
     */
    private SyncStats syncCities(SupplierFtp ftp, Long supplierId, String supplierCode) {
        try (InputStream is = openByConfig(ftp, ftp.getCitiesPath(), true);
             CsvStreamReaderUtil reader = new CsvStreamReaderUtil(is)) {
            return batchUpsertCities(readAll(reader), supplierId, supplierCode);
        } catch (Exception e) {
            throw new RuntimeException("同步城市失败", e);
        }
    }

    /**
     * 同步酒店数据
     * @param ftp
     * @param supplierId
     * @param supplierCode
     * @return
     */
    private SyncStats syncHotels(SupplierFtp ftp, Long supplierId, String supplierCode) {
        try (InputStream is = openByConfig(ftp, ftp.getHotelsPath(), true);
             CsvStreamReaderUtil reader = new CsvStreamReaderUtil(is)) {
            return batchUpsertHotels(readAll(reader), supplierId, supplierCode);
        } catch (Exception e) {
            throw new RuntimeException("同步酒店失败", e);
        }
    }


    /**
     * 同步国籍数据
     * @param ftp
     * @param supplierId
     * @param supplierCode
     * @return
     */
    private SyncStats syncNationalities(SupplierFtp ftp, Long supplierId, String supplierCode) {
        try (InputStream is = openByConfig(ftp, ftp.getNationalityPath(), true);
             CsvStreamReaderUtil reader = new CsvStreamReaderUtil(is)) {
            return batchUpsertNationalities(readAll(reader), supplierId, supplierCode);
        } catch (Exception e) {
            throw new RuntimeException("同步国籍失败", e);
        }
    }

    /**
     * 同步 GIATA 映射数据
     * @param ftp
     * @param supplierId
     * @param supplierCode
     */
    private SyncStats syncGiata(SupplierFtp ftp, Long supplierId, String supplierCode) {
        String path = ftp.getGiataLocalPath();
        if (path == null) return null;
        try (InputStream is = openByConfig(ftp, path, false);
             CsvStreamReaderUtil reader = new CsvStreamReaderUtil(is)) {
            return batchUpsertGiata(readAll(reader), supplierId, supplierCode);
        } catch (Exception e) {
            throw new RuntimeException("同步GIATA失败", e);
        }
    }

    /**
     * 打开本地文件，支持 classpath: 前缀
     */
    private InputStream openLocal(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            String p = path.substring("classpath:".length());
            ClassPathResource res = new ClassPathResource(p);
            return res.getInputStream();
        }
        return new java.io.FileInputStream(path);
    }

    /**
     * 优化的本地读取：允许不写 classpath:
     * 规则：
     * 1) 以 classpath:/file:/绝对路径 开头 -> 直接按其处理
     * 2) 否则优先按 classpath:csv/{addr} 尝试，其次 classpath:{addr}，最后按文件路径尝试
     */
    private InputStream openLocalResolved(String addr) throws Exception {
        if (addr == null) return null;
        String a = addr.trim();
        if (a.startsWith("classpath:")) {
            return openLocal(a);
        }
        if (a.startsWith("file:")) {
            String p = a.substring("file:".length());
            return new java.io.FileInputStream(p);
        }
        // 绝对路径（Linux/Unix）或 Windows 盘符路径
        if (a.startsWith("/") || a.matches("^[A-Za-z]:\\\\.*")) {
            return new java.io.FileInputStream(a);
        }
        // 优先尝试 classpath: <configurable prefix>
        String prefix = ftpClientConfig != null ? ftpClientConfig.getLocalClasspathPrefix() : "csv/";
        if (prefix == null) prefix = "";
        ClassPathResource resCsv = new ClassPathResource(prefix + a);
        if (resCsv.exists()) {
            return resCsv.getInputStream();
        }
        // 再尝试 classpath: 根
        ClassPathResource res = new ClassPathResource(a);
        if (res.exists()) {
            return res.getInputStream();
        }
        // 最后尝试作为文件路径（相对路径）
        return new java.io.FileInputStream(a);
    }

    /**
     * 通用打开方式：支持“类型,地址”的配置
     * - ftp,/path/to/file.csv -> 走 FTP 下载
     * - local,classpath:csv/file.csv 或 local,/data/file.csv -> 走本地读取
     * - 容错：支持 "locad" 作为 local 的别名
     * - 兼容：不含逗号时，defaultFtp=true 则按 FTP 拉取，否则按本地读取
     */
    private InputStream openByConfig(SupplierFtp ftp, String value, boolean defaultFtp) throws Exception {
        if (value == null) return null;
        String type;
        String addr;
        int idx = value.indexOf(',');
        if (idx > 0) {
            type = value.substring(0, idx).trim().toLowerCase();
            addr = value.substring(idx + 1).trim();
        } else {
            // 兼容老配置：没有类型前缀
            type = defaultFtp ? "ftp" : "local";
            addr = value.trim();
        }

        if ("ftp".equals(type)) {
            return ftpClientService.downloadWithRetry(ftp.getHost(), ftp.getPort(), ftp.getUsername(), ftp.getPassword(), addr);
        }
        if ("local".equals(type) || "locad".equals(type)) { // 容错 locad
            return openLocalResolved(addr);
        }
        // 未知类型，按默认策略
        if (defaultFtp) {
            return ftpClientService.downloadWithRetry(ftp.getHost(), ftp.getPort(), ftp.getUsername(), ftp.getPassword(), addr);
        } else {
            return openLocalResolved(addr);
        }
    }

    private List<Map<String, String>> readAll(CsvStreamReaderUtil reader) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Map<String, String> row : reader) {
            rows.add(row);
        }
        return rows;
    }

    // =========== 批量Upsert（JPA saveAll，主键缺失跳过） ==========

    private SyncStats batchUpsertCountries(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        int deleted = deleteBySupplier(Country.class, supplierId, supplierCode);
        logger.info("已清理旧国家数据，supplierId={}, supplierCode={}, 删除行数={}", supplierId, supplierCode, deleted);
        List<Country> list = aoStaticDataParser.parseCountries(rows, supplierId, supplierCode);
        SaveResult sr = saveInBatchesReturnCount(list, countryRepo);
        return new SyncStats(rows.size(), sr.saved, rows.size() - list.size(), sr.errors, sr.errorMsg);
    }

    private SyncStats batchUpsertCities(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        int deleted = deleteBySupplier(City.class, supplierId, supplierCode);
        logger.info("已清理旧城市数据，supplierId={}, supplierCode={}, 删除行数={}", supplierId, supplierCode, deleted);
        List<City> list = aoStaticDataParser.parseCities(rows, supplierId, supplierCode);
        SaveResult sr = saveInBatchesReturnCount(list, cityRepo);
        return new SyncStats(rows.size(), sr.saved, rows.size() - list.size(), sr.errors, sr.errorMsg);
    }

    private SyncStats batchUpsertHotels(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        int deleted = deleteBySupplier(Hotel.class, supplierId, supplierCode);
        logger.info("已清理旧酒店数据，supplierId={}, supplierCode={}, 删除行数={}", supplierId, supplierCode, deleted);
        List<Hotel> list = aoStaticDataParser.parseHotels(rows, supplierId, supplierCode);
        SaveResult sr = saveInBatchesReturnCount(list, hotelRepo);
        return new SyncStats(rows.size(), sr.saved, rows.size() - list.size(), sr.errors, sr.errorMsg);
    }

    private SyncStats batchUpsertNationalities(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        int deleted = deleteBySupplier(Nationality.class, supplierId, supplierCode);
        logger.info("已清理旧国籍数据，supplierId={}, supplierCode={}, 删除行数={}", supplierId, supplierCode, deleted);
        List<Nationality> list = aoStaticDataParser.parseNationalities(rows, supplierId, supplierCode);
        SaveResult sr = saveInBatchesReturnCount(list, nationalityRepo);
        return new SyncStats(rows.size(), sr.saved, rows.size() - list.size(), sr.errors, sr.errorMsg);
    }

    private SyncStats batchUpsertGiata(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        int deleted = deleteBySupplier(HotelGiata.class, supplierId, supplierCode);
        logger.info("已清理旧GIATA数据，supplierId={}, supplierCode={}, 删除行数={}", supplierId, supplierCode, deleted);
        List<HotelGiata> list = aoStaticDataParser.parseGiataMappings(rows, supplierId, supplierCode);
        SaveResult sr = saveInBatchesReturnCount(list, giataRepo);
        return new SyncStats(rows.size(), sr.saved, rows.size() - list.size(), sr.errors, sr.errorMsg);
    }


    /**
     * 分批入库，避免单次数据量过大
     * @param list
     * @param repo
     * @param <T>
     */
    private <T> SaveResult saveInBatchesReturnCount(List<T> list, JpaRepository<T, Long> repo) {
        int batchSize = 1000;
        long saved = 0;
        long errors = 0;
        StringBuilder err = new StringBuilder();
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            List<T> sub = list.subList(i, end);
            try {
                List<T> ret = repo.saveAll(sub);
                saved += (ret != null ? ret.size() : sub.size());
            } catch (Exception ex) {
                errors += sub.size();
                String msg = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
                if (err.length() > 0) err.append("; ");
                err.append(msg);
                logger.warn("批量入库异常，已计为错误条数：{}，原因：{}", sub.size(), ex.getMessage());
            }
        }
        logger.info("批量入库完成，总数={}，成功={}，错误={}", list.size(), saved, errors);
        return new SaveResult(saved, errors, err.toString());
    }

    /** 日志统计结构 */
    private static class SyncStats {
        long total; // 总行数
        long success; // 成功入库数
        long skip; // 跳过数（如主键缺失等）
        long error; // 错误数
        String errorMessage; // 错误汇总

        SyncStats(long total, long success, long skip, long error) {
            this.total = total;
            this.success = success;
            this.skip = Math.max(0, skip);
            this.error = Math.max(0, error);
        }

        SyncStats(long total, long success, long skip, long error, String errorMessage) {
            this(total, success, skip, error);
            this.errorMessage = errorMessage;
        }
    }

    /** 批量保存结果 */
    private static class SaveResult {
        long saved;
        long errors;
        String errorMsg;

        SaveResult(long saved, long errors, String errorMsg) {
            this.saved = saved;
            this.errors = errors;
            this.errorMsg = errorMsg;
        }
    }

    /**
     * 删除指定实体在某个供应商维度下的旧数据
     */
    private int deleteBySupplier(Class<?> entityClass, Long supplierId, String supplierCode) {
        String entityName = entityClass.getSimpleName();
        String jpql = "delete from " + entityName + " e where e.supplierId = :sid and e.supplierCode = :scode";
        return entityManager.createQuery(jpql)
                .setParameter("sid", supplierId)
                .setParameter("scode", supplierCode)
                .executeUpdate();
    }

    /**
     * 解析 FTP 配置
     * @param json
     * @return
     */
    private SupplierFtp parseFtpConfig(String json) {
        try {
            return MAPPER.readValue(json, SupplierFtp.class);
        } catch (Exception e) {
            throw new RuntimeException("解析ftpConfig失败", e);
        }
    }

    /**
     * 清理 FTP 临时目录：删除该目录下的所有文件（不递归删除子目录，不删除目录本身）
     */
    private void cleanTempDir() {
        if (ftpClientConfig == null || ftpClientConfig.getTempDir() == null) return;
        String dirPath = ftpClientConfig.getTempDir();
        java.io.File dir = new java.io.File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.debug("临时目录不存在或不是目录，跳过清理: {}", dir.getAbsolutePath());
            return;
        }
        long start = System.currentTimeMillis();
        int deleted = 0, failed = 0;
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                if (f.isFile()) {
                    try {
                        if (f.delete()) {
                            deleted++;
                        } else {
                            failed++;
                        }
                    } catch (Exception ignore) { failed++; }
                }
            }
        }
        long cost = System.currentTimeMillis() - start;
        logger.info("[FTP临时目录清理] 目录={}, 删除文件数={}, 失败数={}, 用时={}ms", dir.getAbsolutePath(), deleted, failed, cost);
    }
}
