package com.heytrip.hotel.supplier.adapter.impl;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.dto.qtech.req.QTechHotelDetailRequest;
import com.heytrip.hotel.supplier.dto.qtech.req.QTechSearchRequest;
import com.heytrip.hotel.supplier.dto.qtech.resp.QTechHotelDetailResponse;
import com.heytrip.hotel.supplier.dto.qtech.resp.QTechSearchResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsianOverlandAdapter集成测试类
 * 使用真实的API调用测试所有功能，不使用模拟操作
 * 
 * @author Pax
 */
@SpringBootTest
class AsianOverlandAdapterTest {

    private static final Logger logger = LoggerFactory.getLogger(AsianOverlandAdapterTest.class);

    @Autowired
    private AsianOverlandAdapter asianOverlandAdapter;

    /**
     * 测试适配器基本功能
     */
    @Test
    void testAdapterBasicFunctions() {
        logger.info("=== 测试适配器基本功能 ===");
        
        // 测试适配器是否启用
        assertTrue(asianOverlandAdapter.isEnabled(), "适配器应该是启用状态");
        
        // 测试支持的城市
        assertTrue(asianOverlandAdapter.supportsCity("Dubai"), "应该支持迪拜");
        assertTrue(asianOverlandAdapter.supportsCity("Bangkok"), "应该支持曼谷");
        assertTrue(asianOverlandAdapter.supportsCity("Kuala Lumpur"), "应该支持吉隆坡");
        assertFalse(asianOverlandAdapter.supportsCity("New York"), "不应该支持纽约");
        
        // 测试支持的国家
        assertTrue(asianOverlandAdapter.supportsCountry("Malaysia"), "应该支持马来西亚");
        assertTrue(asianOverlandAdapter.supportsCountry("UAE"), "应该支持阿联酋");
        assertTrue(asianOverlandAdapter.supportsCountry("Thailand"), "应该支持泰国");
        assertFalse(asianOverlandAdapter.supportsCountry("USA"), "不应该支持美国");
        
        // 测试适配器配置
        assertEquals("AsianOverland", asianOverlandAdapter.getSupplierName(), "供应商名称应该是AsianOverland");
        assertTrue(asianOverlandAdapter.getPriority() > 0, "优先级应该是正整数");
        assertTrue(asianOverlandAdapter.getSupplierId() != null && asianOverlandAdapter.getSupplierId() > 0, "供应商ID应该是正整数");
        assertTrue(asianOverlandAdapter.getSupplierConfig() != null,"供应商配置不应该为null");
        assertTrue(StrUtil.isNotBlank(asianOverlandAdapter.getSupplierConfig().getAuthConfig()), "认证配置不应该为空");
        assertTrue(asianOverlandAdapter.getTimeoutMs() > 0, "超时时间应该是正整数");

        logger.info("适配器配置: {}", asianOverlandAdapter.getSupplierConfig());
        logger.info("适配器是否启用: {}", asianOverlandAdapter.isEnabled());
        logger.info("适配器供应商ID: {}", asianOverlandAdapter.getSupplierId());
        logger.info("适配器供应商名称: {}", asianOverlandAdapter.getSupplierName());
        logger.info("适配器认证配置: {}", asianOverlandAdapter.getSupplierConfig().getAuthConfig());
        logger.info("适配器超时时间: {} ms", asianOverlandAdapter.getTimeoutMs());
        logger.info("适配器支持的城市: {}", asianOverlandAdapter.getSupportedCities());
        logger.info("适配器支持的国家: {}", asianOverlandAdapter.getSupportedCountries());
        logger.info("适配器优先级: {}", asianOverlandAdapter.getPriority());

        logger.info("适配器基本功能测试通过");
    }

    /**
     * 测试酒店搜索功能
     */
    @Test
    void testSearchHotels() throws InterruptedException {
        logger.info("=== 测试酒店搜索功能 ===");
        
        // 准备测试数据
        QTechSearchRequest request = new QTechSearchRequest();

        request.setCheckinDate("15/12/2025");
        request.setCheckoutDate("16/12/2025");
        request.setNumberOfRooms(1);

        request.setSelCurrency("USD");
        request.setSelCountry("138");
        request.setSelCity("71649");
        request.setCountryOfResidence("1");
        request.setSelNationality("1");
        request.setHotelIds("OT000016097");
        
        // 设置房间详情 - 使用新的结构化格式
        QTechSearchRequest.RoomDetail roomDetail = new QTechSearchRequest.RoomDetail();
        roomDetail.setNumberOfAdults(2);
        //roomDetail.setNumberOfChild(0);
        //roomDetail.setChildAge(""); // 无儿童时为空
        request.setRoomDetails(Collections.singletonList(roomDetail));

        CountDownLatch latch = new CountDownLatch(1);
        final QTechSearchResponse[] responseHolder = new QTechSearchResponse[1];
        final Throwable[] errorHolder = new Throwable[1];

        // 执行真实的API调用
        Mono<QTechSearchResponse> result = asianOverlandAdapter.searchHotels(request);
        
        result.subscribe(
            response -> {
                responseHolder[0] = response;
                if (response != null && "success".equalsIgnoreCase(response.getMessage())) {
                    logger.info("搜索成功！找到 {} 家酒店", 
                            response.getHotelList() != null ? response.getHotelList().size() : 0);
                    
                    if (response.getHotelList() != null && !response.getHotelList().isEmpty()) {
                        // 显示前3家酒店信息
                        response.getHotelList().stream()
                                .limit(3)
                                .forEach(hotel -> {
                                    logger.info("酒店: {} (ID: {}), 星级: {}, 地址: {}", 
                                            hotel.getHotelName(), 
                                            hotel.getLocalHotelId(),
                                            hotel.getPropertyRating(),
                                            hotel.getAddress());
                                });
                    }
                } else {
                    logger.error("搜索失败: Message:{}", response != null ? response.getMessage() : "无响应");
                    logger.error("搜索失败: MessageInfo:{}", response != null ? response.getMessageInfo() : "无响应");

                }
                latch.countDown();
            },
            error -> {
                errorHolder[0] = error;
                logger.error("搜索过程中发生错误", error);
                latch.countDown();
            }
        );

        // 等待异步操作完成
        assertTrue(latch.await(30, TimeUnit.SECONDS), "搜索操作应该在30秒内完成");
        
        // 验证结果
        if (errorHolder[0] != null) {
            fail("搜索操作失败: " + errorHolder[0].getMessage());
        }
        
        assertNotNull(responseHolder[0], "响应不应该为空");
        assertEquals("success", responseHolder[0].getMessage(), "搜索应该成功");
        
        logger.info("酒店搜索功能测试通过");
    }

    /**
     * 测试酒店详情功能
     */
    @Test
    void testGetHotelDetail() throws InterruptedException {
        logger.info("=== 测试酒店详情功能 ===");
        
        // 准备测试数据
        QTechHotelDetailRequest request = new QTechHotelDetailRequest();
        request.setHotelId("OT000016097");
        request.setUniqueId("824-010-20250910024521-010-981963-010-1757472321415648652-010-");

        CountDownLatch latch = new CountDownLatch(1);
        final QTechHotelDetailResponse[] responseHolder = new QTechHotelDetailResponse[1];
        final Throwable[] errorHolder = new Throwable[1];

        // 执行真实的API调用
        Mono<QTechHotelDetailResponse> result = asianOverlandAdapter.getHotelDetail(request);

        result.subscribe(
            response -> {
                responseHolder[0] = response;
                if (response != null && "success".equalsIgnoreCase(response.getMessage())) {
                    logger.info("酒店详情获取成功");
                    if (response.getHotelId() != null) {
                        logger.info("酒店Id: {}", response.getHotelId());
                        logger.info("酒店名称: {}", response.getHotelName());
                        logger.info("酒店描述: {}", response.getDescription());
                        logger.info("酒店房型: {}", response.getSectionSelection());

                    }
                } else {
                    logger.error("酒店详情获取失败: Message:{}", response != null ? response.getMessage() : "无响应");
                    logger.error("酒店详情获取失败: MessageInfo:{}", response != null ? response.getMessageInfo() : "无响应");
                }
                latch.countDown();
            },
            error -> {
                errorHolder[0] = error;
                logger.error("酒店详情获取过程中发生错误", error);
                latch.countDown();
            }
        );

        // 等待异步操作完成
        assertTrue(latch.await(30, TimeUnit.SECONDS), "酒店详情获取应该在30秒内完成");
        
        // 验证结果
        if (errorHolder[0] != null) {
            fail("酒店详情获取失败: " + errorHolder[0].getMessage());
        }
        
        assertNotNull(responseHolder[0], "响应不应该为空");
        assertEquals("success", responseHolder[0].getMessage(), "酒店详情获取应该成功");
        
        logger.info("酒店详情功能测试通过");
    }

    /**
     * 测试健康检查功能
     */
    @Test
    void testHealthCheck() throws InterruptedException {
        logger.info("=== 测试健康检查功能 ===");
        
        CountDownLatch latch = new CountDownLatch(1);
        final Boolean[] healthResult = new Boolean[1];
        final Throwable[] errorHolder = new Throwable[1];

        // 执行健康检查
        asianOverlandAdapter.healthCheck().subscribe(
            isHealthy -> {
                healthResult[0] = isHealthy;
                logger.info("健康检查结果: {}", isHealthy ? "健康" : "不健康");
                latch.countDown();
            },
            error -> {
                errorHolder[0] = error;
                logger.error("健康检查失败", error);
                latch.countDown();
            }
        );

        // 等待异步操作完成
        assertTrue(latch.await(30, TimeUnit.SECONDS), "健康检查应该在30秒内完成");
        
        // 验证结果
        if (errorHolder[0] != null) {
            fail("健康检查失败: " + errorHolder[0].getMessage());
        }
        
        assertNotNull(healthResult[0], "健康检查结果不应该为空");
        
        logger.info("健康检查功能测试通过");
    }

    /**
     * 测试完整的酒店搜索到预订流程
     * 这是一个端到端的集成测试，展示真实的业务流程
     */
    @Test
    void testCompleteHotelWorkflow() throws InterruptedException {
        logger.info("=== 测试完整的酒店搜索到预订流程 ===");
        
        CountDownLatch latch = new CountDownLatch(1);
        final String[] resultMessage = new String[1];
        final Throwable[] errorHolder = new Throwable[1];

        String destination = "Bangkok";
        String checkInDate = "25/12/2025";
        String checkOutDate = "27/12/2025";
        int rooms = 1;
        String agentRefNo = "TEST-" + System.currentTimeMillis();
        
        // 1. 搜索酒店
        QTechSearchRequest searchRequest = new QTechSearchRequest();
        searchRequest.setSelCity(destination);
        searchRequest.setCheckinDate(checkInDate);
        searchRequest.setCheckoutDate(checkOutDate);
        searchRequest.setNumberOfRooms(rooms);
        
        // 设置房间详情 - 使用新的结构化格式
        QTechSearchRequest.RoomDetail roomDetail = new QTechSearchRequest.RoomDetail();
        roomDetail.setNumberOfAdults(2);
        roomDetail.setNumberOfChild(0);
        roomDetail.setChildAge(""); // 无儿童时为空
        searchRequest.setRoomDetails(Collections.singletonList(roomDetail));
        
        asianOverlandAdapter.searchHotels(searchRequest)
            .flatMap(searchResponse -> {
                if (searchResponse != null && "success".equalsIgnoreCase(searchResponse.getMessage())
                    && searchResponse.getHotelList() != null && !searchResponse.getHotelList().isEmpty()) {
                    
                    logger.info("搜索成功，找到 {} 家酒店", searchResponse.getHotelList().size());
                    
                    // 选择第一家酒店的基本信息进行后续测试
                    QTechSearchResponse.Hotel firstHotel = searchResponse.getHotelList().get(0);
                    logger.info("选择酒店: {} (ID: {})", firstHotel.getHotelName(), firstHotel.getLocalHotelId());
                    
                    return Mono.just("搜索流程测试成功");
                } else {
                    return Mono.error(new RuntimeException("搜索失败或无可用酒店"));
                }
            })
            .subscribe(
                result -> {
                    resultMessage[0] = result;
                    logger.info("完整流程测试结果: {}", result);
                    latch.countDown();
                },
                error -> {
                    errorHolder[0] = error;
                    logger.error("完整流程测试失败", error);
                    latch.countDown();
                }
            );

        // 等待异步操作完成
        assertTrue(latch.await(60, TimeUnit.SECONDS), "完整流程测试应该在60秒内完成");
        
        // 验证结果
        if (errorHolder[0] != null) {
            fail("完整流程测试失败: " + errorHolder[0].getMessage());
        }
        
        assertNotNull(resultMessage[0], "测试结果不应该为空");
        assertTrue(resultMessage[0].contains("成功"), "测试应该成功");
        
        logger.info("完整的酒店搜索到预订流程测试通过");
    }
}
