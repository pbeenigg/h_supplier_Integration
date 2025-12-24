package com.heytrip.hotel.supplier.adapter.tasks.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.heytrip.common.enums.XEnumNoSmoking;
import com.heytrip.common.response.base.XHotel;
import com.heytrip.hotel.supplier.adapter.impl.AsianOverlandAdapter;
import com.heytrip.hotel.supplier.adapter.parser.StaticDataParser;
import com.heytrip.hotel.supplier.adapter.service.StaticDataQueryService;
import com.heytrip.hotel.supplier.config.CacheEvictor;
import com.heytrip.hotel.supplier.dto.qtech.req.QTechSearchRequest;
import com.heytrip.hotel.supplier.dto.qtech.resp.QTechSearchResponse;
import com.heytrip.hotel.supplier.entity.primary.SupplierConfig;
import com.heytrip.hotel.supplier.entity.primary.SyncLog;
import com.heytrip.hotel.supplier.entity.supplier.Hotel;
import com.heytrip.hotel.supplier.entity.supplier.HotelBookable;
import com.heytrip.hotel.supplier.entity.supplier.Room;
import com.heytrip.hotel.supplier.repository.primary.SupplierConfigRepository;
import com.heytrip.hotel.supplier.repository.primary.SyncLogRepository;
import com.heytrip.hotel.supplier.repository.supplier.HotelBookableRepository;
import com.heytrip.hotel.supplier.repository.supplier.HotelRepository;
import com.heytrip.hotel.supplier.repository.supplier.RatePlanRepository;
import com.heytrip.hotel.supplier.repository.supplier.RoomRepository;
import com.heytrip.hotel.supplier.utils.HeyUtil;
import com.heytrip.hotel.supplier.utils.MD5Util;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.heytrip.hotel.supplier.constant.SyncTypeNames.HOTEL_BOOKABLE;
import static java.util.stream.Collectors.*;

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

    // 批处理配置
    private static final int BATCH_SIZE = 100; // 每批次处理100个酒店
    private static final int MAX_THREADS = 10; // 最大线程数

    // 并发间隔控制配置
    private static final long DEFAULT_BATCH_INTERVAL_MS = 500; // 默认批次间隔500毫秒
    private static final long MIN_BATCH_INTERVAL_MS = 100;     // 最小批次间隔100毫秒
    private static final long MAX_BATCH_INTERVAL_MS = 5000;    // 最大批次间隔5秒
    private static final int ADAPTIVE_THRESHOLD = 3;           // 连续失败阈值，超过后增加间隔

    // 线程池（懒加载）
    private volatile ExecutorService threadPool;

    // 并发间隔控制状态
    private volatile long currentBatchInterval = DEFAULT_BATCH_INTERVAL_MS;
    private volatile int consecutiveFailures = 0;

    @Resource
    private SupplierConfigRepository supplierConfigRepo;
    @Resource
    private HotelRepository hotelRepo;
    @Resource
    private RoomRepository roomRepo;
    @Resource
    private HotelBookableRepository hotelBookableRepository;
    @Resource
    private RatePlanRepository ratePlanRepo;
    @Resource
    private SyncLogRepository syncLogRepo;
    @Resource
    private CacheEvictor cacheEvictor;

    @Resource
    private AsianOverlandAdapter asianOverlandAdapter;

    @Resource
    private StaticDataQueryService staticDataQueryService;

    @Resource
    private StaticDataParser staticDataParser;

    @PersistenceContext
    private EntityManager entityManager;

    @Resource
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    /**
     * 初始化 TransactionTemplate
     */
    @PostConstruct
    public void init() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        logger.info("[HotelSyncSyncService.init] TransactionTemplate 初始化完成");
    }

    /**
     * 初始化线程池（懒加载）
     */
    private void initializeThreadPool() {
        if (threadPool == null) {
            synchronized (this) {
                if (threadPool == null) {
                    threadPool = Executors.newFixedThreadPool(MAX_THREADS, r -> {
                        Thread t = new Thread(r, "HotelSync-Thread-" + System.currentTimeMillis());
                        t.setDaemon(true);
                        return t;
                    });
                    logger.info("[HotelSyncSyncService.initializeThreadPool] 线程池初始化完成，最大线程数：{}", MAX_THREADS);
                }
            }
        }
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            logger.info("[HotelSyncSyncService.shutdown] 线程池已关闭");
        }
    }


    /**
     * 同步指定供应商的酒店详情数据，包括房型、房价、可售状态
     * 注意：不使用 @Transactional，因为需要访问多个数据源（primary 和 aos）
     */
    public void syncAllForSupplier(Long supplierId, String supplierCode) {
        SupplierConfig sc = supplierConfigRepo.findById(supplierId).orElse(null);
        if (sc == null || sc.getFtpConfig() == null) {
            logger.warn("供应商{} 无 FTP 配置，跳过静态同步", supplierId);
            return;
        }
        logger.info("开始酒店数据同步，supplierId={}, supplierCode={}", supplierId, supplierCode);
        syncOne(() -> syncHotels(supplierId, supplierCode), supplierId, supplierCode, HOTEL_BOOKABLE);
        logger.info("酒店数据同步完成，supplierId={}, supplierCode={}", supplierId, supplierCode);
        // 同步完成后，清理静态数据相关缓存，避免读取到陈旧数据
        try {
            cacheEvictor.evictAllStaticCaches();
        } catch (Exception e) {
            logger.warn("清理静态缓存失败: {}", e.getMessage());
        }

    }


    /**
     * 同步酒店详情数据（优化版本）
     * 优化项：
     * 1. 酒店可售数据分离到 hotel_bookable 表
     * 2. 分批次处理，每批100个酒店
     * 3. 线程池并发处理
     * 4. 按批次清理可售酒店数据
     * 5. 完善的错误处理和日志记录
     */
    @DS("aos")
    private SyncStats syncHotels(Long supplierId, String supplierCode) {
        logger.info("[HotelSyncSyncService.syncHotels] 开始同步酒店详情数据，supplierId={}, supplierCode={}", supplierId, supplierCode);

        // 重置间隔控制状态，开始新的同步任务
        resetIntervalControl();

        // 统计信息
        AtomicLong totalHotels = new AtomicLong(0);
        AtomicLong availableHotels = new AtomicLong(0);
        AtomicInteger totalBatches = new AtomicInteger(0);
        AtomicInteger processedBatches = new AtomicInteger(0);
        AtomicLong savedHotels = new AtomicLong(0);
        AtomicLong savedRooms = new AtomicLong(0);
        AtomicLong totalErrors = new AtomicLong(0);

        /**
         * 入离时间代表酒店的：入住时间 和 离店时间
         * 2天内要跑完一次全量酒店， 数据大约 10万左右
         * 采用4个入离日期。当前时间（1、一周后某个工作日入住一天，2、两周后的某个工作日连住2天的入离，3、三周后入住一天, 4、一周后某个周5日入住2天），
         * 注意：只要有一个入离有价就算有价，后面的获取规则就不用跑了可以跳过，一定要记录好采集到有价的日期时间；
         * 渠道服务通过可售酒店编号接口来拉取有价酒店的时候，就根据最近7天采集到有价的（最近7天是指采集时间，不是入离日期）； （Hotel 表增加一个采集更新时间）
         * 如果供应商接口有明确的有价酒店接口，也可以直接采用供应商的标识。一周中周5周6入住算周末，因为工作日入住没那么容易满房，如果QPS充足也可以加一个周末入住的有价采集。
         */

        // 计算4组入离日期
        List<LocalDate[]> datePairs = new ArrayList<>();
        LocalDate today = LocalDate.now();
        // 1. 一周后某个工作日入住1天
        LocalDate checkIn1 = HeyUtil.nextWeekday(today.plusDays(7));
        datePairs.add(new LocalDate[]{checkIn1, checkIn1.plusDays(1)});
        // 2. 两周后的某个工作日连住2天
        LocalDate checkIn2 = HeyUtil.nextWeekday(today.plusDays(14));
        datePairs.add(new LocalDate[]{checkIn2, checkIn2.plusDays(2)});
        // 3. 三周后入住1天
        LocalDate checkIn3 = HeyUtil.nextWeekday(today.plusDays(21));
        datePairs.add(new LocalDate[]{checkIn3, checkIn3.plusDays(1)});
        // 4. 一周后某个周五或者周六入住2天
        LocalDate checkIn4 = HeyUtil.nextWeekend(today.plusDays(7));
        datePairs.add(new LocalDate[]{checkIn4, checkIn4.plusDays(2)});

        logger.info("[HotelSyncSyncService.syncHotels] 计算得到4组入离日期：{}",
                datePairs.stream().map(pair -> pair[0] + " - " + pair[1]).collect(toList()));

        try {
            // 初始化线程池
            initializeThreadPool();

            // 分页处理所有酒店
            int page = 0;
            int pageSize = 1000;
            Page<XHotel> hotelPage;

            // 收集所有批次任务
            List<CompletableFuture<BatchProcessResult>> batchFutures = new ArrayList<>();

            do {
                hotelPage = staticDataQueryService.pageHotels(supplierId, supplierCode, page, pageSize);
                totalHotels.addAndGet(hotelPage.getContent().size());

                logger.info("[HotelSyncSyncService.syncHotels] 处理第{}页，共{}页，当前页酒店数：{}，总酒店数：{}",
                        page + 1, hotelPage.getTotalPages(), hotelPage.getContent().size(), hotelPage.getTotalElements());

                // 将酒店分批处理，每批100个
                List<List<XHotel>> batches = ListUtil.partition(hotelPage.getContent(), BATCH_SIZE);
                totalBatches.addAndGet(batches.size());

                // 为每个批次创建异步任务，增加间隔控制
                for (List<XHotel> batch : batches) {
                    int currentBatchIndex = processedBatches.incrementAndGet();

                    CompletableFuture<BatchProcessResult> batchFuture = CompletableFuture.supplyAsync(() -> {
                        // 执行批次间隔控制，防止API过载
                        executeBatchInterval(currentBatchIndex, totalBatches.get());

                        // 处理批次并记录结果用于自适应调整
                        try {
                            BatchProcessResult result = processBatch(batch, currentBatchIndex, totalBatches.get(),
                                    supplierId, supplierCode, datePairs);
                            // 记录批次成功，用于自适应间隔调整
                            recordBatchResult(result.getErrorCount() == 0);
                            return result;
                        } catch (Exception ex) {
                            // 记录批次失败，用于自适应间隔调整
                            recordBatchResult(false);
                            throw ex;
                        }
                    }, threadPool).exceptionally(ex -> {
                        logger.error("[HotelSyncSyncService.syncHotels] 批次 {}/{} 处理异常",
                                currentBatchIndex, totalBatches.get(), ex);
                        totalErrors.incrementAndGet();
                        // 记录批次失败
                        recordBatchResult(false);
                        return new BatchProcessResult(0, 0, 0, 1, "批次处理异常: " + ex.getMessage());
                    });

                    batchFutures.add(batchFuture);
                }

                page++;
            } while (hotelPage.hasNext());

            // 等待所有批次完成并汇总结果
            logger.info("[HotelSyncSyncService.syncHotels] 等待 {} 个批次任务完成...", batchFutures.size());

            for (CompletableFuture<BatchProcessResult> future : batchFutures) {
                try {
                    BatchProcessResult result = future.get(30, TimeUnit.MINUTES); // 每批次最多等待30分钟
                    availableHotels.addAndGet(result.getAvailableHotels());
                    savedHotels.addAndGet(result.getSavedHotels());
                    savedRooms.addAndGet(result.getSavedRooms());
                    if (result.getErrorCount() > 0) {
                        totalErrors.addAndGet(result.getErrorCount());
                    }
                } catch (TimeoutException e) {
                    logger.error("[HotelSyncSyncService.syncHotels] 批次任务超时", e);
                    totalErrors.incrementAndGet();
                } catch (Exception e) {
                    logger.error("[HotelSyncSyncService.syncHotels] 批次任务执行异常", e);
                    totalErrors.incrementAndGet();
                }
            }

            // 返回统计结果
            logger.info("[HotelSyncSyncService.syncHotels] 同步完成：总酒店{}个，有价酒店{}个，保存酒店{}个，保存房型{}个，错误{}个",
                    totalHotels.get(), availableHotels.get(), savedHotels.get(), savedRooms.get(), totalErrors.get());
            logger.info("[HotelSyncSyncService.syncHotels] 间隔控制状态：{}", getIntervalControlStatus());

            return new SyncStats(
                    totalHotels.get(),
                    savedHotels.get(),
                    totalHotels.get() - savedHotels.get(),
                    totalErrors.get(),
                    String.format("酒店:%d/%d, 房型:%d, 有价:%d",
                            savedHotels.get(), totalHotels.get(), savedRooms.get(), availableHotels.get())
            );

        } catch (Exception ex) {
            logger.error("[HotelSyncSyncService.syncHotels] 同步酒店数据失败", ex);
            return new SyncStats(totalHotels.get(), 0, totalHotels.get(), 1, "同步失败: " + ex.getMessage());
        }
    }

    /**
     * 处理单个批次的酒店数据（线程池中执行）
     * 优化项：
     * 1. 按批次清理可售酒店数据
     * 2. 分离酒店静态数据和可售数据
     * 3. 完善的错误处理和日志记录
     */
    public BatchProcessResult processBatch(List<XHotel> batch, int batchIndex, int totalBatches,
                                           Long supplierId, String supplierCode, List<LocalDate[]> datePairs) {
        logger.info("[HotelSyncSyncService.processBatch] 开始处理批次 {}/{}, 酒店数：{}",
                batchIndex, totalBatches, batch.size());

        long availableHotels = 0;
        long savedHotels = 0;
        long savedRooms = 0;
        long errorCount = 0;

        try {
            // 1. 按批次清理可售酒店数据
            List<String> batchHotelCodes = batch.stream()
                    .map(XHotel::getHotelId)
                    .collect(toList());

            // 直接调用删除操作，使用编程式事务
            clearBookableHotels(supplierId, supplierCode, batchHotelCodes);

            logger.info("[HotelSyncSyncService.processBatch] 批次 {}/{} 清理可售酒店数据完成，酒店数：{}",
                    batchIndex, totalBatches, batchHotelCodes.size());

            // 2. 批量保存的数据集合
            List<Hotel> batchHotelsToSave = new ArrayList<>();
            List<Room> batchRoomsToSave = new ArrayList<>();

            // 构建酒店和房型的映射关系
            Map<String, List<QTechSearchResponse.RoomRate>> hotelRoomMap = new HashMap<>();

            // 存储酒店数据，用于后续构建可售酒店数据
            Map<String, QTechSearchResponse.Hotel> hotelDataMap = new HashMap<>();

            // 用于记录本批次中有价可用的酒店，实现早期跳出
            Set<String> availableHotelIds = new HashSet<>();

            // 3. 遍历每组入离日期，调用酒店搜索接口
            for (int dateIndex = 0; dateIndex < datePairs.size(); dateIndex++) {
                LocalDate[] pair = datePairs.get(dateIndex);
                LocalDate checkIn = pair[0];
                LocalDate checkOut = pair[1];

                logger.info("[HotelSyncSyncService.processBatch] 批次 {}/{} 处理日期组 {}/{}：{} - {}",
                        batchIndex, totalBatches, dateIndex + 1, datePairs.size(), checkIn, checkOut);

                // 过滤掉已经有价的酒店，实现早期跳出优化
                List<XHotel> remainingHotels = batch.stream()
                        .filter(h -> !availableHotelIds.contains(h.getHotelId()))
                        .collect(toList());

                if (remainingHotels.isEmpty()) {
                    logger.info("[HotelSyncSyncService.processBatch] 批次 {}/{} 所有酒店都已有价，跳过剩余日期组",
                            batchIndex, totalBatches);
                    break;
                }

                // 构造酒店ID字符串 - 逗号分隔的字符串，最多100个酒店ID
                String remainingHotelIds = remainingHotels.stream()
                        .map(XHotel::getHotelId)
                        .collect(Collectors.joining(","));

                // 构造搜索请求
                QTechSearchRequest req = buildSearchRequest(remainingHotelIds, checkIn, checkOut);

                // 调用 QTECH 搜索
                QTechSearchResponse resp = asianOverlandAdapter.searchHotels(req)
                        .doOnError(e -> logger.error("[HotelSyncSyncService.processBatch] 批次 {}/{} 酒店搜索失败",
                                batchIndex, totalBatches, e))
                        .block();

                if (resp == null || !"success".equalsIgnoreCase(resp.getMessage())) {
                    logger.warn("[HotelSyncSyncService.processBatch] 批次 {}/{} QTECH响应异常: message={}, info={}",
                            batchIndex, totalBatches,
                            resp != null ? resp.getMessage() : "null",
                            resp != null ? resp.getMessageInfo() : "null");
                    continue;
                }

                List<QTechSearchResponse.Hotel> hotelList = resp.getHotelList();
                if (CollUtil.isEmpty(hotelList)) {
                    logger.warn("[HotelSyncSyncService.processBatch] 批次 {}/{} 返回成功但无酒店数据",
                            batchIndex, totalBatches);
                    continue;
                }

                // 处理有效的酒店数据
                List<QTechSearchResponse.Hotel> validHotels = hotelList.stream()
                        .filter(h -> StrUtil.isNotBlank(h.getHotelId()) && StrUtil.isNotBlank(h.getHotelName()))
                        .filter(h -> CollUtil.isNotEmpty(h.getHotelProperty()))
                        .collect(toList());

                logger.info("[HotelSyncSyncService.processBatch] 批次 {}/{} 有效酒店数：{}/{}",
                        batchIndex, totalBatches, validHotels.size(), hotelList.size());

                // 更新酒店和房型的映射关系
                for (QTechSearchResponse.Hotel hotel : validHotels) {
                    String hotelId = hotel.getHotelId();
                    availableHotelIds.add(hotelId); // 记录有效酒店ID
                    availableHotels++; // 统计有效酒店数

                    // 同步酒店静态数据
                    Hotel hotelEntity = buildHotelEntity(hotel, supplierId, supplierCode, checkIn, checkOut);
                    batchHotelsToSave.add(hotelEntity);

                    // 存储酒店数据，用于后续构建可售酒店数据（需要hotel_id）
                    hotelDataMap.put(hotelId, hotel);

                    // 收集房型数据，稍后处理
                    List<QTechSearchResponse.RoomRate> roomRates = new ArrayList<>();
                    List<QTechSearchResponse.HotelProperty> properties = hotel.getHotelProperty();
                    for (QTechSearchResponse.HotelProperty property : properties) {
                        if (CollUtil.isEmpty(property.getRoomRates())) {
                            continue;
                        }

                        for (QTechSearchResponse.RoomRate roomRate : property.getRoomRates()) {
                            // 过滤无效房型
                            if (roomRate.getAvailable() == null || roomRate.getAvailable() != 1 ||
                                    roomRate.getRoomRate() == null || roomRate.getRoomRate().compareTo(BigDecimal.ZERO) <= 0) {
                                continue;
                            }
                            roomRates.add(roomRate);
                        }
                    }

                    if (!roomRates.isEmpty()) {
                        hotelRoomMap.put(hotelId, roomRates);
                    }
                }
            }

            // 4. 批次处理完成后，立即保存到数据库（避免大事务）
            if (!batchHotelsToSave.isEmpty()) {
                // 先保存酒店静态数据，获得保存后的实体（包含ID）
                Map<String, Long> hotelIdMap = saveBatchHotelsAndGetIds(batchHotelsToSave, supplierId, supplierCode);
                savedHotels = hotelIdMap.size();

                // 构建并保存可售酒店数据（现在有了hotel_id）
                List<HotelBookable> batchBookableHotelsToSave = new ArrayList<>();
                for (Map.Entry<String, Long> entry : hotelIdMap.entrySet()) {
                    String hotelCode = entry.getKey();
                    Long hotelEntityId = entry.getValue();

                    // 获取对应的酒店数据
                    QTechSearchResponse.Hotel hotelData = hotelDataMap.get(hotelCode);
                    if (hotelData != null) {
                        HotelBookable bookableHotel = buildBookableHotelEntity(hotelData, supplierId, supplierCode, hotelEntityId);
                        batchBookableHotelsToSave.add(bookableHotel);
                    }
                }

                if (!batchBookableHotelsToSave.isEmpty()) {
                    hotelBookableRepository.saveAll(batchBookableHotelsToSave);
                    logger.info("[HotelSyncSyncService.processBatch] 批次 {}/{} 保存可售酒店数据完成：{}个",
                            batchIndex, totalBatches, batchBookableHotelsToSave.size());
                }

                // 然后构建和保存房型
                for (Map.Entry<String, Long> entry : hotelIdMap.entrySet()) {
                    String hotelCode = entry.getKey();
                    Long hotelEntityId = entry.getValue();

                    List<QTechSearchResponse.RoomRate> roomRates = hotelRoomMap.get(hotelCode);

                    // 过滤有效的房型
                    List<QTechSearchResponse.RoomRate> filterRoomRates = roomRates.stream()
                            .filter(rr -> StrUtil.isNotBlank(rr.getRoomCategory()) && StrUtil.isNotBlank(rr.getRoomType()))
                            .filter(rr -> rr.getRoomRate() != null && rr.getRoomRate().compareTo(BigDecimal.ZERO) > 0)
                            .filter(rr -> rr.getAvailable() != null && rr.getAvailable() == 1)
                            .collect(toList());


                    if (filterRoomRates != null) {
                        //去掉房型名称相同记录，保留价格最低的
                        List<Room> roomsToAdd = filterRoomRates.stream()
                                .collect(groupingBy(QTechSearchResponse.RoomRate::getRoomCategory,
                                        collectingAndThen(
                                                minBy(Comparator.comparing(QTechSearchResponse.RoomRate::getRoomRate)),
                                                Optional::get
                                        )
                                ))
                                .values()
                                .stream()
                                .map(rr -> buildRoomEntity(rr, hotelCode, supplierId, supplierCode, hotelEntityId))
                                .filter(Objects::nonNull) // 过滤掉null值
                                .collect(toList());

                        // 累计待保存的房型
                        batchRoomsToSave.addAll(roomsToAdd);
                    }
                }

                // 保存房型
                if (!batchRoomsToSave.isEmpty()) {
                    savedRooms = upsertRooms(batchRoomsToSave, supplierId, supplierCode);
                }

                logger.info("[HotelSyncSyncService.processBatch] 批次 {}/{} 保存完成：酒店{}个，房型{}个，可售酒店{}个",
                        batchIndex, totalBatches, savedHotels, savedRooms, batchBookableHotelsToSave.size());
            }

            return new BatchProcessResult(availableHotels, savedHotels, savedRooms, errorCount,
                    String.format("批次 %d/%d 处理成功", batchIndex, totalBatches));

        } catch (Exception e) {
            logger.error("[HotelSyncSyncService.processBatch] 批次 {}/{} 处理失败",
                    batchIndex, totalBatches, e);
            errorCount = 1;
            return new BatchProcessResult(availableHotels, savedHotels, savedRooms, errorCount,
                    String.format("批次 %d/%d 处理失败: %s", batchIndex, totalBatches, e.getMessage()));
        }
    }

    /**
     * 构建可售酒店实体
     */
    private HotelBookable buildBookableHotelEntity(QTechSearchResponse.Hotel hotel, Long supplierId, String supplierCode, Long hotelId) {
        HotelBookable bookable = new HotelBookable();
        bookable.setSupplierId(supplierId);
        bookable.setSupplierCode(supplierCode);

        // 当供应商酒店ID超过64字符，使用原始ID的SHA-256（64位十六进制）作为 hotelCodeMd5；否则直接使用原始ID
        String hotelCodeMd5 = hotel.getHotelId().length() > 32 ? MD5Util.string2MD5(hotel.getHotelId()) : hotel.getHotelId();
        bookable.setHotelCode(hotel.getHotelId());
        bookable.setHotelCodeMd5(hotelCodeMd5);


        bookable.setHotelId(hotelId);  // 设置酒店表主键ID
        bookable.setName(hotel.getHotelName());
        bookable.setIsBookable(true);

        // 计算最低价格
        BigDecimal minPrice = null;
        if (CollUtil.isNotEmpty(hotel.getHotelProperty())) {
            for (QTechSearchResponse.HotelProperty property : hotel.getHotelProperty()) {
                if (CollUtil.isNotEmpty(property.getRoomRates())) {
                    for (QTechSearchResponse.RoomRate roomRate : property.getRoomRates()) {
                        if (roomRate.getRoomRate() != null && roomRate.getRoomRate().compareTo(BigDecimal.ZERO) > 0) {
                            if (minPrice == null || roomRate.getRoomRate().compareTo(minPrice) < 0) {
                                minPrice = roomRate.getRoomRate();
                            }
                        }
                    }
                }
            }
        }
        bookable.setMinPrice(minPrice);

        return bookable;
    }

    /**
     * 同步单个业务数据，并记录日志
     *
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

    private void syncOne(Supplier<SyncStats> task, Long supplierId, String supplierCode, String biz) {
        syncOne(task, supplierId, supplierCode, biz, null);
    }

    /**
     * 构建搜索请求
     */
    private QTechSearchRequest buildSearchRequest(String hotelIds, LocalDate checkIn, LocalDate checkOut) {
        QTechSearchRequest req = new QTechSearchRequest();

        // 酒店ID（必需） - 逗号分隔的字符串，最多100个酒店ID
        req.setHotelIds(hotelIds);

        // 日期格式转换 yyyy-MM-dd -> dd/MM/yyyy
        req.setCheckinDate(checkIn.format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
        req.setCheckoutDate(checkOut.format(HeyUtil.DATE_FORMATTER_DDMMYYYY));

        // 币种，默认 MYR   马来西亚  货币
        req.setSelCurrency("MYR");
        req.setSelNationality("119");
        req.setCountryOfResidence("119");

        // 房间明细与房间数 默认 2 个成人
        List<QTechSearchRequest.RoomDetail> details = HeyUtil.buildQTechRoomDetails("2");
        req.setRoomDetails(details);
        req.setNumberOfRooms(details != null ? details.size() : 0);

        // 可根据需要设置静态信息、limit、availableonly 等
        req.setAvailableonly(1);
        req.setStaticData(1);

        return req;
    }

    /**
     * 构建酒店实体对象
     */
    private Hotel buildHotelEntity(QTechSearchResponse.Hotel hotel, Long supplierId, String supplierCode,
                                   LocalDate checkIn, LocalDate checkOut) {
        Hotel hotelEntity = new Hotel();
        hotelEntity.setSupplierId(supplierId);
        hotelEntity.setSupplierCode(supplierCode);

        String hotelCode = hotel.getHotelId();
        hotelEntity.setHotelCode(hotelCode);
        // 当供应商酒店ID超过64字符，使用原始ID的SHA-256（64位十六进制）作为 hotelCodeMd5；否则直接使用原始ID
        String hotelCodeMd5 = hotelCode.length() > 32 ? MD5Util.string2MD5(hotelCode) : hotelCode;
        hotelEntity.setHotelCodeMd5(hotelCodeMd5);

        hotelEntity.setHotelName(hotel.getHotelName());
        hotelEntity.setDescription(hotel.getHotelName());
        hotelEntity.setAddress(hotel.getAddress());
        hotelEntity.setLatitude(hotel.getLatitude());
        hotelEntity.setLongitude(hotel.getLongitude());
        hotelEntity.setHeroImg(hotel.getThumbNailUrl());
        hotelEntity.setRating(hotel.getPropertyRating());

        // 计算最低价格（如果是多间房，需要除以房间数）
        BigDecimal totalCharges = hotel.getTotalCharges();
        if (totalCharges != null && totalCharges.compareTo(BigDecimal.ZERO) > 0) {
            hotelEntity.setMinPrice(totalCharges);

            // 设置为可预订（有价格即可预订）
            hotelEntity.setIsBookable(true);
        }

        // 设置同步时间（用于7天内有价酒店的判断）
        hotelEntity.setSyncAt(LocalDateTime.now());

        return hotelEntity;
    }

    /**
     * 构建房型实体对象
     */
    private Room buildRoomEntity(QTechSearchResponse.RoomRate roomRate,
                                 String hotelId, Long supplierId, String supplierCode, Long hotelEntityId) {
        Room roomEntity = new Room();
        roomEntity.setSupplierId(supplierId);
        roomEntity.setSupplierCode(supplierCode);
        roomEntity.setHotelCode(hotelId);
        roomEntity.setHotelId(hotelEntityId); // 直接使用传入的Hotel实体ID


        //房型编码 ：通过酒店ID+ 房型名称   例如：  OT000005002+SUPERIOR, KING BED, BALCONY   （处理掉特殊字符，用_连接）
        String roomName = roomRate.getRoomCategory();

        // 生成房型编码： 房型名称，处理特殊字符用下划线连接
        // 提取真实的房型名称（移除动态标识、括号内容、特殊分隔符等）
        String realRoomName = asianOverlandAdapter.extractRealRoomName(roomName);
        logger.debug("[HotelSyncSyncService.buildRoomEntity] 原始房型名称: {}, 处理后房型名称: {}", roomName, realRoomName);

        // 处理特殊字符：保留字母、数字、中文，其他字符替换为下划线，连续的下划线合并为一个
      /*  String roomCode = realRoomName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]+", "_")
                .replaceAll("_+", "_")  // 合并连续的下划线
                .replaceAll("^_|_$", ""); // 去掉首尾的下划线*/

        // 如果处理后的编码超过64字符，使用MD5
        //String finalRoomCode = roomCode;
        String roomCodeMd5 = MD5Util.string2MD5(realRoomName);

        roomEntity.setRoomCode(realRoomName);
        roomEntity.setRoomCodeMd5(roomCodeMd5);
        roomEntity.setRoomName(realRoomName);
        roomEntity.setRoomNameEn(realRoomName);
        roomEntity.setDescription(roomRate.getRoomType());
        //roomEntity.setBedTypeDesc(roomRate.getRoomCategory());
        //roomEntity.setBedTypeDescEn(roomRate.getRoomCategory());

        // 设置价格信息
        if (roomRate.getRoomRate() != null) {
            // 计算单间最低价格，保留2位小数，使用四舍五入
            BigDecimal minPrice = NumberUtil.div(roomRate.getRoomRate(), 1, 2, RoundingMode.HALF_UP);

            roomEntity.setMinPrice(minPrice);
            roomEntity.setMinBasePrice(minPrice);
        }

        // 禁烟信息判断
        String roomType = roomRate.getRoomType();
        String roomCategory = roomRate.getRoomCategory();
        if ((roomType != null && (roomType.contains("Non Smoking") || roomType.contains("Smoking"))) ||
                (roomCategory != null && (roomCategory.contains("Non Smoking") || roomCategory.contains("Smoking")))) {
            roomEntity.setNoSmoking(XEnumNoSmoking.NON_SMOKING.name());
        }

        return roomEntity;
    }


    /**
     * 批量保存酒店并返回酒店ID映射
     * 每批次独立事务，避免大事务回滚
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Long> saveBatchHotelsAndGetIds(List<Hotel> hotels, Long supplierId, String supplierCode) {
        Map<String, Long> hotelIdMap = new HashMap<>();

        for (Hotel hotel : hotels) {
            try {
                // 根据唯一约束查询是否存在
                Optional<Hotel> existingHotel = hotelRepo.findBySupplierIdAndSupplierCodeAndHotelCode(
                        supplierId, supplierCode, hotel.getHotelCode());

                Hotel savedHotel;
                if (existingHotel.isPresent()) {
                    // 更新现有记录
                    Hotel existing = existingHotel.get();
                    updateHotelFields(existing, hotel);
                    savedHotel = hotelRepo.save(existing);
                } else {
                    // 新增记录
                    savedHotel = hotelRepo.save(hotel);
                }

                // 记录酒店代码和ID的映射关系
                hotelIdMap.put(hotel.getHotelCode(), savedHotel.getId());

            } catch (Exception e) {
                logger.error("[HotelSyncSyncService.saveBatchHotelsAndGetIds] 保存酒店失败: hotelCode={}", hotel.getHotelCode(), e);
            }
        }

        return hotelIdMap;
    }


    /**
     * Upsert房型数据（批量优化版本，增强防重复机制）
     * 性能优化：按酒店分组批量查询 + 批量保存，确保房型编码在酒店维度的唯一性
     * 约束：(supplier_id, supplier_code, hotel_code, room_code) 唯一
     */
    private long upsertRooms(List<Room> rooms, Long supplierId, String supplierCode) {
        if (CollUtil.isEmpty(rooms)) {
            return 0;
        }

        long saved = 0;


        try {
            // 0. 按酒店编码分组处理房型数据
            Map<String, List<Room>> roomsByHotel = rooms.stream()
                    .collect(groupingBy(Room::getHotelCode));

            logger.debug("[HotelSyncSyncService.upsertRooms] 原始房型数: {}, 涉及酒店数: {}",
                    rooms.size(), roomsByHotel.size());

            // 1. 按酒店分组处理，确保房型编码在酒店维度的唯一性
            for (Map.Entry<String, List<Room>> entry : roomsByHotel.entrySet()) {
                String hotelCode = entry.getKey();


                List<Room> roomsForHotel = entry.getValue();
                //根据酒店房型编码去重，保留价格最低的房型
                List<Room> hotelRooms = roomsForHotel.stream().collect(groupingBy(Room::getRoomCode,
                        collectingAndThen(
                                minBy(Comparator.comparing(Room::getMinPrice)),
                                Optional::get
                        )
                )).values().stream().collect(toList());

                logger.debug("[HotelSyncSyncService.upsertRooms] 酒店{}: 原始房型数{}, 去重后{}",
                        hotelCode, roomsForHotel.size(), hotelRooms.size());

                // 1.2 批量查询该酒店的现有房型数据
                List<String> roomCodes = hotelRooms.stream()
                        .map(Room::getRoomCode)
                        .collect(toList());

                List<Room> existingRooms = roomRepo.findBySupplierIdAndSupplierCodeAndHotelCodeAndRoomCodeIn(supplierId, supplierCode, hotelCode, roomCodes);

                logger.debug("[HotelSyncSyncService.upsertRooms] 酒店{} 批量查询结果: 查询{}个roomCode，找到{}个现有记录",
                        hotelCode, roomCodes.size(), existingRooms.size());

                // 1.3 构建现有数据映射表
                Map<String, Room> existingRoomMap = existingRooms.stream()
                        .collect(toMap(Room::getRoomCode, r -> r));

                // 1.4 分离新增和更新数据
                List<Room> toInsert = new ArrayList<>();
                List<Room> toUpdate = new ArrayList<>();

                for (Room room : hotelRooms) {
                    Room existing = existingRoomMap.get(room.getRoomCode());
                    if (existing != null) {
                        // 更新现有记录
                        updateRoomFields(existing, room);
                        toUpdate.add(existing);
                    } else {
                        // 新增记录
                        toInsert.add(room);
                    }
                }

                // 1.5 批量保存该酒店的房型数据（分两阶段：先处理更新，再处理新增）
                List<Room> additionalInserts = new ArrayList<>();

                // 第一阶段：处理更新实体
                if (!toUpdate.isEmpty()) {
                    List<Room> validUpdates = new ArrayList<>();
                    for (Room room : toUpdate) {
                        if (room.getId() == null) {
                            logger.error("[HotelSyncSyncService.upsertRooms] 酒店{}更新房型实体ID为null: roomCode={}",
                                    hotelCode, room.getRoomCode());
                            // 将ID为null的实体转为新增
                            room.setId(null);
                            additionalInserts.add(room);
                        } else {
                            validUpdates.add(room);
                        }
                    }

                    if (!validUpdates.isEmpty()) {
                        // 批量更新
                        List<Room> updatedRooms = roomRepo.saveAll(validUpdates);
                        saved += updatedRooms.size();
                        logger.debug("[HotelSyncSyncService.upsertRooms] 酒店{} 批量更新房型: {}个",
                                hotelCode, updatedRooms.size());
                    }
                }

                // 第二阶段：处理所有新增实体（包括原始新增 + 从更新转换的）
                List<Room> allInserts = new ArrayList<>(toInsert);
                allInserts.addAll(additionalInserts);

                if (!allInserts.isEmpty()) {
                    // 验证新增实体的ID应该为null
                    for (Room room : allInserts) {
                        if (room.getId() != null) {
                            logger.warn("[HotelSyncSyncService.upsertRooms] 酒店{}新增房型实体ID不为null: roomCode={}, id={}",
                                    hotelCode, room.getRoomCode(), room.getId());
                            room.setId(null); // 强制设置为null，让数据库自动生成
                        }
                    }
                    // 批量新增
                    List<Room> insertedRooms = roomRepo.saveAll(allInserts);
                    saved += insertedRooms.size();
                    logger.debug("[HotelSyncSyncService.upsertRooms] 酒店{} 批量新增房型: {}个",
                            hotelCode, insertedRooms.size());
                }
            }

        } catch (Exception e) {
            logger.error("[HotelSyncSyncService.upsertRooms] 批量保存房型失败", e);

            // 如果是唯一约束冲突，尝试更智能的处理
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry") &&
                    e.getMessage().contains("uk_room_supplier_code")) {
                logger.warn("[HotelSyncSyncService.upsertRooms] 检测到唯一约束冲突，尝试智能恢复");
                return upsertRoomsWithConstraintHandling(rooms, supplierId, supplierCode);
            }

            // 其他错误，降级到逐个保存
            return upsertRoomsOneByOne(rooms, supplierId, supplierCode);
        }

        return saved;
    }

    /**
     * 智能处理唯一约束冲突的房型保存方案
     */
    private long upsertRoomsWithConstraintHandling(List<Room> rooms, Long supplierId, String supplierCode) {
        long saved = 0;

        logger.info("[HotelSyncSyncService.upsertRoomsWithConstraintHandling] 开始智能处理{}个房型的唯一约束冲突", rooms.size());

        // 按酒店分组处理房型数据
        Map<String, List<Room>> roomsByHotel = rooms.stream()
                .collect(groupingBy(Room::getHotelCode));

        // 对每个酒店的房型进行单独的upsert处理，跳过重复记录
        for (Map.Entry<String, List<Room>> entry : roomsByHotel.entrySet()) {
            String hotelCode = entry.getKey();
            List<Room> hotelRooms = entry.getValue();

            for (Room room : hotelRooms) {
                try {
                    // 查询现有记录（使用包含酒店编码的查询）
                    List<Room> existingRooms = roomRepo.findBySupplierIdAndSupplierCodeAndHotelCodeAndRoomCodeIn(
                            supplierId, supplierCode, hotelCode, Arrays.asList(room.getRoomCode()));

                    if (!existingRooms.isEmpty()) {
                        // 更新现有记录
                        Room existing = existingRooms.get(0);
                        updateRoomFields(existing, room);
                        roomRepo.save(existing);
                        saved++;
                        logger.debug("[HotelSyncSyncService.upsertRoomsWithConstraintHandling] 酒店{} 更新房型: {}",
                                hotelCode, room.getRoomCode());
                    } else {
                        // 新增记录
                        room.setId(null); // 确保ID为null
                        roomRepo.save(room);
                        saved++;
                        logger.debug("[HotelSyncSyncService.upsertRoomsWithConstraintHandling] 酒店{} 新增房型: {}",
                                hotelCode, room.getRoomCode());
                    }
                } catch (Exception e) {
                    // 如果仍然有唯一约束冲突，说明可能是并发问题，跳过这条记录
                    if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                        logger.warn("[HotelSyncSyncService.upsertRoomsWithConstraintHandling] 酒店{} 跳过重复房型: roomCode={}, 错误: {}",
                                hotelCode, room.getRoomCode(), e.getMessage());
                    } else {
                        logger.error("[HotelSyncSyncService.upsertRoomsWithConstraintHandling] 酒店{} 保存房型失败: roomCode={}",
                                hotelCode, room.getRoomCode(), e);
                    }
                }
            }
        }

        logger.info("[HotelSyncSyncService.upsertRoomsWithConstraintHandling] 智能处理完成，成功保存{}个房型", saved);
        return saved;
    }

    /**
     * 降级方案：逐个保存房型（当批量保存失败时使用，支持酒店维度查询）
     */
    private long upsertRoomsOneByOne(List<Room> rooms, Long supplierId, String supplierCode) {
        long saved = 0;

        // 按酒店分组处理房型数据
        Map<String, List<Room>> roomsByHotel = rooms.stream()
                .collect(groupingBy(Room::getHotelCode));

        for (Map.Entry<String, List<Room>> entry : roomsByHotel.entrySet()) {
            String hotelCode = entry.getKey();
            List<Room> hotelRooms = entry.getValue();

            for (Room room : hotelRooms) {
                try {
                    // 查询现有记录（使用包含酒店编码的查询）
                    List<Room> existingRooms = roomRepo.findBySupplierIdAndSupplierCodeAndHotelCodeAndRoomCodeIn(
                            supplierId, supplierCode, hotelCode, Arrays.asList(room.getRoomCode()));

                    if (!existingRooms.isEmpty()) {
                        Room existing = existingRooms.get(0);
                        updateRoomFields(existing, room);
                        roomRepo.save(existing);
                        logger.debug("[HotelSyncSyncService.upsertRoomsOneByOne] 酒店{} 更新房型: {}",
                                hotelCode, room.getRoomCode());
                    } else {
                        room.setId(null); // 确保ID为null
                        roomRepo.save(room);
                        logger.debug("[HotelSyncSyncService.upsertRoomsOneByOne] 酒店{} 新增房型: {}",
                                hotelCode, room.getRoomCode());
                    }
                    saved++;
                } catch (Exception e) {
                    logger.error("[HotelSyncSyncService.upsertRoomsOneByOne] 酒店{} 保存房型失败: roomCode={}",
                            hotelCode, room.getRoomCode(), e);
                }
            }
        }

        return saved;
    }

    /**
     * 更新酒店字段
     */
    private void updateHotelFields(Hotel existing, Hotel newHotel) {
        existing.setHotelName(newHotel.getHotelName());
        existing.setDescription(newHotel.getDescription());
        existing.setAddress(newHotel.getAddress());
        existing.setLatitude(newHotel.getLatitude());
        existing.setLongitude(newHotel.getLongitude());
        existing.setHeroImg(newHotel.getHeroImg());
        existing.setRating(newHotel.getRating());
        existing.setMinPrice(newHotel.getMinPrice());
        existing.setIsBookable(newHotel.getIsBookable());
        existing.setSyncAt(newHotel.getSyncAt()); // 更新同步时间
    }

    /**
     * 更新房型字段（安全更新，保护ID字段）
     */
    private void updateRoomFields(Room existing, Room newRoom) {
        // 验证existing实体必须有有效的ID
        if (existing.getId() == null) {
            logger.error("[HotelSyncSyncService.updateRoomFields] existing房型实体ID为null: roomCode={}",
                    existing.getRoomCode());
            throw new IllegalArgumentException("existing房型实体ID不能为null");
        }

        // 保存原始ID，确保不被意外覆盖
        Long originalId = existing.getId();

        // 更新业务字段
        existing.setRoomName(newRoom.getRoomName());
        existing.setRoomNameEn(newRoom.getRoomNameEn());
        existing.setDescription(newRoom.getDescription());
        //existing.setBedTypeDesc(newRoom.getBedTypeDesc());
        //existing.setBedTypeDescEn(newRoom.getBedTypeDescEn());
        existing.setMinPrice(newRoom.getMinPrice());
        existing.setMinBasePrice(newRoom.getMinBasePrice());
        existing.setNoSmoking(newRoom.getNoSmoking());

        // 确保ID没有被意外修改
        if (!originalId.equals(existing.getId())) {
            logger.warn("[HotelSyncSyncService.updateRoomFields] 房型实体ID被意外修改，恢复原值: {} -> {}",
                    existing.getId(), originalId);
            existing.setId(originalId);
        }
    }


    /**
     * 日志统计结构
     */
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


    /**
     * 批次处理结果结构
     */
    @Data
    private static class BatchProcessResult {

        private final long availableHotels;  // 有价酒店数量
        private final long savedHotels;      // 保存的酒店数量
        private final long savedRooms;       // 保存的房型数量
        private final long errorCount;       // 错误数量
        private final String message;        // 处理消息

        public BatchProcessResult(long availableHotels, long savedHotels, long savedRooms, long errorCount, String message) {
            this.availableHotels = availableHotels;
            this.savedHotels = savedHotels;
            this.savedRooms = savedRooms;
            this.errorCount = errorCount;
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format("BatchProcessResult{有价酒店=%d, 保存酒店=%d, 保存房型=%d, 错误=%d, 消息='%s'}",
                    availableHotels, savedHotels, savedRooms, errorCount, message);
        }

    }


    /**
     * 使用TransactionTemplate手动管理事务清理可售酒店数据
     * 解决线程池中事务代理失效的问题
     */
    @DS("aos")
    private void clearBookableHotels(Long supplierId, String supplierCode, List<String> hotelCodes) {
        transactionTemplate.execute(status -> {
            try {
                // 执行删除操作  根据(supplier_id, supplier_code, hotel_code) 删除
                int deletedCount = hotelBookableRepository.deleteBySupplierIdAndSupplierCodeAndHotelCodeIn(supplierId, supplierCode, hotelCodes);
                logger.debug("[HotelSyncSyncService.clearBookableHotels] 删除可售酒店数据 {} 条", deletedCount);
                return deletedCount;
            } catch (Exception ex) {
                logger.error("[HotelSyncSyncService.clearBookableHotels] 删除可售酒店数据失败", ex);
                status.setRollbackOnly();
                throw ex;
            }
        });
    }

    /**
     * 执行批次间隔控制
     * 在启动新批次前等待指定时间，防止API过载
     *
     * @param batchIndex   当前批次索引
     * @param totalBatches 总批次数
     */
    private void executeBatchInterval(int batchIndex, int totalBatches) {
        if (batchIndex > 1) { // 第一个批次不需要等待
            try {
                long intervalMs = getCurrentBatchInterval();
                logger.debug("[HotelSyncSyncService.executeBatchInterval] 批次 {}/{} 等待 {}ms 后启动",
                        batchIndex, totalBatches, intervalMs);
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("[HotelSyncSyncService.executeBatchInterval] 批次间隔等待被中断", e);
            }
        }
    }

    /**
     * 获取当前批次间隔时间
     * 支持自适应调整：连续失败时增加间隔，成功时逐渐恢复
     */
    private synchronized long getCurrentBatchInterval() {
        return currentBatchInterval;
    }

    /**
     * 记录批次处理结果，用于自适应调整间隔
     *
     * @param success 批次是否成功
     */
    private synchronized void recordBatchResult(boolean success) {
        if (success) {
            // 成功时重置连续失败计数，并逐渐减少间隔
            consecutiveFailures = 0;
            if (currentBatchInterval > MIN_BATCH_INTERVAL_MS) {
                currentBatchInterval = Math.max(MIN_BATCH_INTERVAL_MS,
                        (long) (currentBatchInterval * 0.9));
                logger.debug("[HotelSyncSyncService.recordBatchResult] 批次成功，间隔调整为 {}ms",
                        currentBatchInterval);
            }
        } else {
            // 失败时增加连续失败计数
            consecutiveFailures++;
            if (consecutiveFailures >= ADAPTIVE_THRESHOLD) {
                // 连续失败超过阈值，增加间隔
                currentBatchInterval = Math.min(MAX_BATCH_INTERVAL_MS,
                        (long) (currentBatchInterval * 1.5));
                logger.warn("[HotelSyncSyncService.recordBatchResult] 连续失败 {} 次，间隔调整为 {}ms",
                        consecutiveFailures, currentBatchInterval);
            }
        }
    }

    /**
     * 重置间隔控制状态
     * 在开始新的同步任务时调用
     */
    private synchronized void resetIntervalControl() {
        currentBatchInterval = DEFAULT_BATCH_INTERVAL_MS;
        consecutiveFailures = 0;
        logger.info("[HotelSyncSyncService.resetIntervalControl] 重置间隔控制，当前间隔: {}ms",
                currentBatchInterval);
    }

    /**
     * 获取间隔控制状态信息
     */
    public String getIntervalControlStatus() {
        return String.format("当前批次间隔: %dms, 连续失败次数: %d",
                currentBatchInterval, consecutiveFailures);
    }

}
