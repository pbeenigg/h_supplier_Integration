package com.heytrip.hotel.supplier.adapter.tasks;

import com.heytrip.hotel.supplier.adapter.service.impl.StaticDataSyncService;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 静态数据同步调度任务
 * - 启动后10分钟内执行一次同步（异步）
 * - 每15天凌晨2点定时同步
 */
@Component
public class StaticDataSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(StaticDataSyncScheduler.class);

    private final StaticDataSyncService syncService;
    private final SupplierConfigRepository supplierConfigRepository;

    // AsianOverland 供应商代码
    private static final String AO_SUPPLIER_CODE = "AsianOverland";

    public StaticDataSyncScheduler(StaticDataSyncService syncService,
                                   SupplierConfigRepository supplierConfigRepository) {
        this.syncService = syncService;
        this.supplierConfigRepository = supplierConfigRepository;
    }

    /**
     * 应用启动后，延迟10分钟执行一次同步，避免影响启动性能
     */
    @EventListener(ApplicationReadyEvent.class)
    public void scheduleInitialSync() {

        // 使用单线程调度，设置为守护线程
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "static-data-initial-sync");
            t.setDaemon(true);
            return t;
        });

        // 延迟10分钟执行
        ses.schedule(() -> {
            try {
                logger.info("[初始] 开始执行 AsianOverland 静态数据同步（延迟10分钟）");
                runForAOQ();
            } catch (Exception e) {
                logger.warn("[初始] 静态数据同步失败: {}", e.getMessage());
            }
        }, 10, TimeUnit.SECONDS);
    }

    /**
     * 每15天凌晨2点执行
     *
     * TODO 目前固定为AOQ，后续可扩展为多供应商
     */
    @Scheduled(cron = "0 0 2 */15 * ?")
    public void schedulePeriodicSync() {
        try {
            logger.info("[定时] 开始执行 AsianOverland 静态数据同步");
            runForAOQ();
        } catch (Exception e) {
            logger.warn("[定时] 静态数据同步失败: {}", e.getMessage());
        }
    }

    /**
     * 执行 AOQ 供应商 静态数据同步
     */
    private void runForAOQ() {
        Optional<SupplierConfig> opt = supplierConfigRepository.findBySupplierCode(AO_SUPPLIER_CODE);
        if (opt.isEmpty()) {
            logger.warn("未找到 AOQ 供应商配置，supplierCode={}", AO_SUPPLIER_CODE);
            return;
        }
        SupplierConfig sc = opt.get();
        if (Boolean.FALSE.equals(sc.getIsActive())) {
            logger.info("AOQ 供应商未启用，跳过静态同步");
            return;
        }
        syncService.syncAllForSupplier(sc.getId(), sc.getSupplierCode());
    }
}
