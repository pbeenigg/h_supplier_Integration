package com.heytrip.hotel.supplier.adapter.tasks.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.StrUtil;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.common.enums.XEnumCurrency;
import com.heytrip.common.enums.XEnumNoSmoking;
import com.heytrip.common.response.base.XHotel;
import com.heytrip.common.response.base.XRatePlan;
import com.heytrip.common.response.base.XRatePlanDaily;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.adapter.impl.AsianOverlandAdapter;
import com.heytrip.hotel.supplier.adapter.parser.StaticDataParser;
import com.heytrip.hotel.supplier.adapter.service.StaticDataQueryService;
import com.heytrip.hotel.supplier.client.FtpClientService;
import com.heytrip.hotel.supplier.config.CacheEvictor;
import com.heytrip.hotel.supplier.config.FtpClientConfig;
import com.heytrip.hotel.supplier.dto.qtech.req.QTechSearchRequest;
import com.heytrip.hotel.supplier.dto.qtech.resp.QTechSearchResponse;
import com.heytrip.hotel.supplier.entity.*;
import com.heytrip.hotel.supplier.repository.*;
import com.heytrip.hotel.supplier.utils.HeyUtil;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.heytrip.hotel.supplier.constant.SyncBusinessTypeNames.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    @Resource
    private SupplierConfigRepository supplierConfigRepo;
    @Resource
    private HotelRepository hotelRepo;
    @Resource
    private RoomRepository roomRepo;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 同步指定供应商的酒店详情数据，包括房型、房价、可售状态
     */
    @Transactional
    public void syncAllForSupplier(Long supplierId, String supplierCode) {
        SupplierConfig sc = supplierConfigRepo.findById(supplierId).orElse(null);
        if (sc == null || sc.getFtpConfig() == null) {
            logger.warn("供应商{} 无 FTP 配置，跳过静态同步", supplierId);
            return;
        }
        logger.info("开始酒店数据同步，supplierId={}, supplierCode={}", supplierId, supplierCode);
        syncOne(() -> syncHotels(supplierId, supplierCode), supplierId, supplierCode, HOTELS_DETAIL);
        logger.info("酒店数据同步完成，supplierId={}, supplierCode={}", supplierId, supplierCode);
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
        logger.info("[HotelSyncSyncService.syncHotels] 开始同步酒店详情数据，supplierId={}, supplierCode={}", supplierId, supplierCode);
        
        // 统计信息
        AtomicLong totalHotels = new AtomicLong(0);
        AtomicLong processedHotels = new AtomicLong(0);
        AtomicLong availableHotels = new AtomicLong(0);
        AtomicInteger totalBatches = new AtomicInteger(0);
        AtomicInteger processedBatches = new AtomicInteger(0);
        
        // 批量保存的数据集合
        List<Hotel> hotelsToSave = new ArrayList<>();
        List<Room> roomsToSave = new ArrayList<>();
        List<RatePlan> ratePlansToSave = new ArrayList<>();

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
            // 分页处理所有酒店
            int page = 0;
            int pageSize = 1000;
            Page<XHotel> hotelPage;
            
            do {
                hotelPage = staticDataQueryService.pageHotels(supplierId, supplierCode, page, pageSize);
                totalHotels.addAndGet(hotelPage.getContent().size());
                
                logger.info("[HotelSyncSyncService.syncHotels] 处理第{}页，共{}页，当前页酒店数：{}，总酒店数：{}", 
                    page + 1, hotelPage.getTotalPages(), hotelPage.getContent().size(), hotelPage.getTotalElements());

                // 将酒店分批处理，每批100个
                List<List<XHotel>> batches = ListUtil.partition(hotelPage.getContent(), 100);
                totalBatches.addAndGet(batches.size());
                
                for (List<XHotel> batch : batches) {
                    processedBatches.incrementAndGet();
                    
                    logger.info("[HotelSyncSyncService.syncHotels] 处理批次 {}/{}, 酒店数：{}",
                        processedBatches.get(), totalBatches.get(), batch.size());

                    // 用于记录本批次中有价可用的酒店，实现早期跳出
                    Set<String> availableHotelIds = new HashSet<>();
                    
                    // 遍历每组入离日期，调用酒店搜索接口
                    for (int dateIndex = 0; dateIndex < datePairs.size(); dateIndex++) {
                        LocalDate[] pair = datePairs.get(dateIndex);
                        LocalDate checkIn = pair[0];
                        LocalDate checkOut = pair[1];
                        
                        logger.info("[HotelSyncSyncService.syncHotels] 处理日期组 {}/{}：{} - {}", 
                            dateIndex + 1, datePairs.size(), checkIn, checkOut);

                        // 过滤掉已经有价的酒店，实现早期跳出优化
                        List<XHotel> remainingHotels = batch.stream()
                            .filter(h -> !availableHotelIds.contains(h.getHotelId()))
                            .collect(toList());
                            
                        if (remainingHotels.isEmpty()) {
                            logger.info("[HotelSyncSyncService.syncHotels] 本批次所有酒店都已有价，跳过剩余日期组");
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
                                .doOnError(e -> logger.error("[HotelSyncSyncService.syncHotels] 酒店搜索失败", e))
                                .block();

                        if (resp == null || !"success".equalsIgnoreCase(resp.getMessage())) {
                            logger.warn("[HotelSyncSyncService.syncHotels] QTECH响应异常: message={}, info={}", 
                                resp != null ? resp.getMessage() : "null", 
                                resp != null ? resp.getMessageInfo() : "null");
                            continue;
                        }

                        List<QTechSearchResponse.Hotel> hotelList = resp.getHotelList();
                        if (CollUtil.isEmpty(hotelList)) {
                            logger.warn("[HotelSyncSyncService.syncHotels] 返回成功但无酒店数据");
                            continue;
                        }

                        // 处理有效的酒店数据
                        List<QTechSearchResponse.Hotel> validHotels = hotelList.stream()
                            .filter(h -> StrUtil.isNotBlank(h.getHotelId()) && StrUtil.isNotBlank(h.getHotelName()))
                            .filter(h -> CollUtil.isNotEmpty(h.getHotelProperty()))
                            .collect(toList());

                        logger.info("[HotelSyncSyncService.syncHotels] 有效酒店数：{}/{}", validHotels.size(), hotelList.size());

                        for (QTechSearchResponse.Hotel hotel : validHotels) {
                            String hotelId = hotel.getHotelId();
                            availableHotelIds.add(hotelId); // 记录有效酒店ID
                            availableHotels.incrementAndGet(); // 统计有效酒店数
                            
                            // 同步酒店详情
                            Hotel hotelEntity = buildHotelEntity(hotel, supplierId, supplierCode, checkIn, checkOut);
                            hotelsToSave.add(hotelEntity); // 酒店信息，后续批量保存

                            // 同步房型和房价
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

                                    // 同步房型
                                    Room roomEntity = buildRoomEntity(roomRate, property, hotelId, supplierId, supplierCode);
                                    roomsToSave.add(roomEntity);

                                    // 同步房价计划
                                    //RatePlan ratePlanEntity = buildRatePlanEntity(roomRate, property, hotelId, supplierId, supplierCode);
                                    //ratePlansToSave.add(ratePlanEntity);
                                }
                            }
                            
                            processedHotels.incrementAndGet();
                        }
                    }
                }
                
                page++;
            } while (hotelPage.hasNext());

            // 批量保存到数据库
            logger.info("[HotelSyncSyncService.syncHotels] 开始批量保存数据：酒店{}个，房型{}个，房价计划{}个", 
                hotelsToSave.size(), roomsToSave.size(), ratePlansToSave.size());
                
            SaveResult hotelSaveResult = saveInBatchesReturnCount(hotelsToSave, hotelRepo);
            SaveResult roomSaveResult = saveInBatchesReturnCount(roomsToSave, roomRepo);
            SaveResult ratePlanSaveResult = saveInBatchesReturnCount(ratePlansToSave, ratePlanRepo);

            logger.info("[HotelSyncSyncService.syncHotels] 数据保存完成：酒店保存{}个，房型保存{}个，房价计划保存{}个", 
                hotelSaveResult.saved, roomSaveResult.saved, ratePlanSaveResult.saved);

            // 返回统计结果
            return new SyncStats(
                totalHotels.get(),
                availableHotels.get(),
                totalHotels.get() - availableHotels.get(),
                hotelSaveResult.errors + roomSaveResult.errors + ratePlanSaveResult.errors,
                String.format("酒店:%d/%d, 房型:%d, 房价:%d", 
                    hotelSaveResult.saved, totalHotels.get(), roomSaveResult.saved, ratePlanSaveResult.saved)
            );

        } catch (Exception ex) {
            logger.error("[HotelSyncSyncService.syncHotels] 同步酒店数据失败", ex);
            return new SyncStats(totalHotels.get(), 0, totalHotels.get(), 1, "同步失败: " + ex.getMessage());
        }
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
        
        // 币种，默认 USD
        req.setSelCurrency("USD");
        req.setSelNationality("1");
        req.setCountryOfResidence("1");

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
        String hotelCodeMd5 = hotelCode.length() > 64 ? HeyUtil.sha256Hex(hotelCode) : hotelCode;
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
        }

        // 设置为可预订（有价格即可预订）
        hotelEntity.setIsBookable(true);
        
        // 设置同步时间（用于7天内有价酒店的判断）
        hotelEntity.setSyncAt(LocalDateTime.now());

        return hotelEntity;
    }

    /**
     * 构建房型实体对象
     */
    private Room buildRoomEntity(QTechSearchResponse.RoomRate roomRate, QTechSearchResponse.HotelProperty property,
                                String hotelId, Long supplierId, String supplierCode) {
        Room roomEntity = new Room();
        roomEntity.setSupplierId(supplierId);
        roomEntity.setSupplierCode(supplierCode);
        roomEntity.setHotelCode(hotelId);

        String roomCode = roomRate.getClassUniqueId();
        roomEntity.setRoomCode(roomCode);
        // 当供应商ID超过64字符，使用原始ID的SHA-256（64位十六进制）作为 roomCodeMd5；否则直接使用原始ID
        String roomCodeMd5 = roomCode.length() > 64 ? HeyUtil.sha256Hex(roomCode) : roomCode;
        roomEntity.setRoomCodeMd5(roomCodeMd5);

        roomEntity.setRoomName(roomRate.getRoomCategory());
        roomEntity.setRoomNameEn(roomRate.getRoomCategory());
        roomEntity.setDescription(roomRate.getRoomType());
        roomEntity.setBedTypeDesc(roomRate.getRoomCategory());
        roomEntity.setBedTypeDescEn(roomRate.getRoomCategory());
        
        // 设置价格信息
        if (roomRate.getRoomRate() != null) {
            roomEntity.setMinPrice(roomRate.getRoomRate());
            roomEntity.setMinBasePrice(roomRate.getRoomRate());
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
     * 构建房价计划实体对象
     */
    private RatePlan buildRatePlanEntity(QTechSearchResponse.RoomRate roomRate, QTechSearchResponse.HotelProperty property,
                                        String hotelId, Long supplierId, String supplierCode) {
        RatePlan ratePlanEntity = new RatePlan();
        ratePlanEntity.setSupplierId(supplierId);
        ratePlanEntity.setSupplierCode(supplierCode);
        ratePlanEntity.setHotelCode(hotelId);
        ratePlanEntity.setRoomCode(roomRate.getClassUniqueId());

        String ratePlanCode = property.getSectionUniqueId() + "_" + roomRate.getClassUniqueId();
        ratePlanEntity.setRatePlanCode(ratePlanCode);
        // 当供应商ID超过64字符，使用原始ID的SHA-256（64位十六进制）作为 ratePlanCodeMd5；否则直接使用原始ID
        String ratePlanCodeMd5 = ratePlanCode.length() > 64 ? HeyUtil.sha256Hex(ratePlanCode) : ratePlanCode;
        ratePlanEntity.setRatePlanCodeMd5(ratePlanCodeMd5);

        ratePlanEntity.setName(roomRate.getRoomType());
        
        // 设置餐食信息
        String mealBasis = roomRate.getMealBasis();
        String roomType = roomRate.getRoomType();
        StringBuilder mealInfo = new StringBuilder();
        if (mealBasis != null || roomType != null) {
            if ((mealBasis != null && mealBasis.toLowerCase().contains("breakfast")) || 
                (roomType != null && roomType.toLowerCase().contains("breakfast"))) {
                mealInfo.append("早餐,");
            }
            if ((mealBasis != null && mealBasis.toLowerCase().contains("lunch")) || 
                (roomType != null && roomType.toLowerCase().contains("lunch"))) {
                mealInfo.append("午餐,");
            }
            if ((mealBasis != null && mealBasis.toLowerCase().contains("dinner")) || 
                (roomType != null && roomType.toLowerCase().contains("dinner"))) {
                mealInfo.append("晚餐,");
            }
        }
        if (mealInfo.length() > 0) {
            ratePlanEntity.setMeal(mealInfo.substring(0, mealInfo.length() - 1));
        }

        // 设置取消政策
        if (property.getPolicies() != null && CollUtil.isNotEmpty(property.getPolicies().getCancellationPolicy())) {
            try {
                String cancellationPolicyJson = MAPPER.writeValueAsString(property.getPolicies().getCancellationPolicy());
                ratePlanEntity.setCancellationPolicy(cancellationPolicyJson);
            } catch (Exception e) {
                logger.warn("序列化取消政策失败: {}", e.getMessage());
            }
        }

        return ratePlanEntity;
    }


    // =========== 批量Upsert（JPA saveAll） ==========


    private SyncStats batchUpsertHotels(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        int deleted = deleteBySupplier(Hotel.class, supplierId, supplierCode);
        logger.info("已清理旧酒店数据，supplierId={}, supplierCode={}, 删除行数={}", supplierId, supplierCode, deleted);
        List<Hotel> list = staticDataParser.parseHotels(rows, supplierId, supplierCode);
        SaveResult sr = saveInBatchesReturnCount(list, hotelRepo);
        return new SyncStats(rows.size(), sr.saved, rows.size() - list.size(), sr.errors, sr.errorMsg);
    }


    /**
     * 分批入库，避免单次数据量过大
     *
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
     * 批量保存结果
     */
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
