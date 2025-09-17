package com.heytrip.hotel.supplier.adapter.tasks.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.adapter.parser.StaticDataParser;
import com.heytrip.hotel.supplier.client.FtpClientService;
import com.heytrip.hotel.supplier.config.CacheEvictor;
import com.heytrip.hotel.supplier.config.FtpClientConfig;
import com.heytrip.hotel.supplier.entity.*;
import com.heytrip.hotel.supplier.repository.*;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 酒店相关信息同步服务
 * - 同步酒店详细信息，包括房型和房价
 * - 同步酒店可售状态
 * - 同步酒店基础信息增量数据
 * - 同步酒店房型基础信息增量数据
 */
@Service
public class HotelSyncSyncService {
    private static final Logger logger = LoggerFactory.getLogger(HotelSyncSyncService.class);

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
        logger.info("开始酒店数据同步，supplierId={}, supplierCode={}", supplierId, supplierCode);
        syncOne(() -> syncHotels(supplierId, supplierCode), supplierId, supplierCode, "hotelsDetail");
        logger.info("酒店同步完成，supplierId={}, supplierCode={}", supplierId, supplierCode);
        // 同步完成后，清理静态数据相关缓存，避免读取到陈旧数据
        try {
            cacheEvictor.evictAllStaticCaches();
        } catch (Exception e) {
            logger.warn("清理静态缓存失败: {}", e.getMessage());
        }

    }

    /**
     * 同步酒店详情数据，包括房型和房价
     * Hotel：酒店
     * Room: 房型
     * RatePlan: 房价
     *
     * @param supplierId
     * @param supplierCode
     * @return
     */
    private SyncStats syncHotels(Long supplierId, String supplierCode) {
        return  null;
    }


    /**
     * 同步可售酒店
     * @param supplierId
     * @param supplierCode
     * @return
     */
    private SyncStats syncBookableHotel(Long supplierId, String supplierCode) {
        return  null;
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
                    String existed = log.getErrorMessage();
                    if (existed == null || existed.isBlank()) {
                        log.setErrorMessage(stats.errorMessage);
                    } else {
                        log.setErrorMessage(existed + " | " + stats.errorMessage);
                    }
                }
            }
            log.setIsSuccess(true);
        } catch (Exception e) {
            log.setErrorMessage(e.getMessage());
            logger.warn("同步 {} 失败: {}", biz, e.getMessage());
        } finally {
            log.setEndTime(LocalDateTime.now());
            syncLogRepo.save(log);
        }
    }

    private void syncOne(Supplier<SyncStats> task, Long supplierId, String supplierCode, String biz) {
        syncOne(task, supplierId, supplierCode, biz, null);
    }












    // =========== 批量Upsert（JPA saveAll） ==========



    private SyncStats batchUpsertHotels(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        int deleted = deleteBySupplier(Hotel.class, supplierId, supplierCode);
        logger.info("已清理旧酒店数据，supplierId={}, supplierCode={}, 删除行数={}", supplierId, supplierCode, deleted);
        List<Hotel> list = aoStaticDataParser.parseHotels(rows, supplierId, supplierCode);
        SaveResult sr = saveInBatchesReturnCount(list, hotelRepo);
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


}
