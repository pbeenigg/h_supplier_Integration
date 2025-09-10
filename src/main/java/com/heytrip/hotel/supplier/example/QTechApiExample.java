package com.heytrip.hotel.supplier.example;

import com.heytrip.hotel.supplier.adapter.impl.AsianOverlandAdapter;
import com.heytrip.hotel.supplier.dto.qtech.QTechCancellationResponse;
import com.heytrip.hotel.supplier.dto.qtech.QTechReservationResponse;
import com.heytrip.hotel.supplier.dto.qtech.QTechSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * QTECH API使用示例
 * 展示如何使用AsianOverlandAdapter进行酒店搜索、预订、取消等操作
 * 
 * 重要提示：AsianOverlandAdapter现在使用HttpClientService提供统一的HTTP请求处理，
 * 包括自动日志记录、重试机制、错误处理等企业级功能。
 * 
 * @author Pax
 */
@Component
public class QTechApiExample {
    
    private static final Logger logger = LoggerFactory.getLogger(QTechApiExample.class);
    
    @Autowired
    private AsianOverlandAdapter asianOverlandAdapter;
    
    /**
     * 酒店搜索示例
     * 
     * @param destination 目的地
     * @param checkInDate 入住日期 (格式: dd/MM/yyyy)
     * @param checkOutDate 离店日期 (格式: dd/MM/yyyy)
     * @param rooms 房间数
     */
    public void searchHotelsExample(String destination, String checkInDate, String checkOutDate, int rooms) {
        logger.info("=== QTECH酒店搜索示例 ===");
        logger.info("目的地: {}, 入住: {}, 离店: {}, 房间数: {}", destination, checkInDate, checkOutDate, rooms);
        
        Mono<QTechSearchResponse> searchResult = asianOverlandAdapter.searchHotels(destination, checkInDate, checkOutDate, rooms);
        
        searchResult.subscribe(
            response -> {
                if (response != null && "success".equals(response.getMessage())) {
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
                    logger.error("搜索失败: {}", response != null ? response.getMessage() : "无响应");
                }
            },
            error -> logger.error("搜索过程中发生错误", error)
        );
    }
    
    /**
     * 酒店预订示例
     * 
     * @param hotelId 酒店ID
     * @param roomId 房间ID
     * @param agentRefNo 代理参考号（订单号）
     */
    public void bookHotelExample(String hotelId, String roomId, String agentRefNo) {
        logger.info("=== QTECH酒店预订示例 ===");
        logger.info("酒店ID: {}, 房间ID: {}, 订单号: {}", hotelId, roomId, agentRefNo);
        
        Mono<QTechReservationResponse> bookingResult = asianOverlandAdapter.bookHotel(hotelId, roomId, agentRefNo);
        
        bookingResult.subscribe(
            response -> {
                if (response != null && "success".equals(response.getStatus())) {
                    logger.info("预订成功！");
                    if (response.getBookingDetail() != null) {
                        QTechReservationResponse.BookingDetail detail = response.getBookingDetail();
                        logger.info("预订ID: {}", detail.getId());
                        logger.info("预订参考号: {}", detail.getBookingReference());
                        logger.info("预订状态: {}", detail.getCurrentStatus());
                        logger.info("总金额: {} {}", detail.getTotalCharges(), detail.getCurrencyCode());
                        logger.info("酒店名称: {}", detail.getHotelName());
                        logger.info("入住日期: {}", detail.getCheckInDate());
                        logger.info("离店日期: {}", detail.getCheckOutDate());
                    }
                } else {
                    logger.error("预订失败: {}", response != null ? response.getMessage() : "无响应");
                }
            },
            error -> logger.error("预订过程中发生错误", error)
        );
    }
    
    /**
     * 取消预订示例
     * 
     * @param bookingId 预订ID
     * @param reason 取消原因
     */
    public void cancelBookingExample(String bookingId, String reason) {
        logger.info("=== QTECH取消预订示例 ===");
        logger.info("预订ID: {}, 取消原因: {}", bookingId, reason);
        
        Mono<QTechCancellationResponse> cancelResult = asianOverlandAdapter.cancelBooking(bookingId, reason);
        
        cancelResult.subscribe(
            response -> {
                if (response != null && "success".equals(response.getStatus())) {
                    logger.info("取消成功！");
                    logger.info("取消消息: {}", response.getMessage());
                } else {
                    logger.error("取消失败: {}", response != null ? response.getMessage() : "无响应");
                }
            },
            error -> logger.error("取消过程中发生错误", error)
        );
    }
    
    /**
     * 完整流程示例：搜索 -> 预订 -> 取消
     */
    public void fullWorkflowExample() {
        logger.info("=== QTECH完整流程示例 ===");
        
        String destination = "Dubai";
        String checkInDate = "25/12/2024";
        String checkOutDate = "27/12/2024";
        int rooms = 1;
        String agentRefNo = "TEST-" + System.currentTimeMillis();
        
        // 1. 搜索酒店
        asianOverlandAdapter.searchHotels(destination, checkInDate, checkOutDate, rooms)
            .flatMap(searchResponse -> {
                if (searchResponse != null && "success".equals(searchResponse.getMessage()) 
                    && searchResponse.getHotelList() != null && !searchResponse.getHotelList().isEmpty()) {
                    
                    // 选择第一家酒店进行预订
                    QTechSearchResponse.Hotel firstHotel = searchResponse.getHotelList().get(0);
                    String hotelId = firstHotel.getLocalHotelId();
                    
                    // 选择第一个可用房型
                    String roomId = null;
                    if (firstHotel.getHotelProperty() != null && !firstHotel.getHotelProperty().isEmpty()) {
                        QTechSearchResponse.HotelProperty property = firstHotel.getHotelProperty().get(0);
                        if (property.getRoomRates() != null && !property.getRoomRates().isEmpty()) {
                            roomId = property.getRoomRates().get(0).getClassUniqueId();
                        }
                    }
                    
                    if (roomId != null) {
                        logger.info("选择酒店: {} (ID: {})", firstHotel.getHotelName(), hotelId);
                        
                        // 2. 执行预订
                        return asianOverlandAdapter.bookHotel(hotelId, roomId, agentRefNo);
                    } else {
                        return Mono.error(new RuntimeException("未找到可用房型"));
                    }
                } else {
                    return Mono.error(new RuntimeException("搜索失败或无可用酒店"));
                }
            })
            .flatMap(bookingResponse -> {
                if (bookingResponse != null && "success".equals(bookingResponse.getStatus())
                    && bookingResponse.getBookingDetail() != null) {
                    
                    String bookingId = bookingResponse.getBookingDetail().getId();
                    logger.info("预订成功，预订ID: {}", bookingId);
                    
                    // 3. 模拟取消预订（实际使用中谨慎操作）
                    logger.info("等待5秒后执行取消操作...");
                    return Mono.delay(java.time.Duration.ofSeconds(5))
                            .then(asianOverlandAdapter.cancelBooking(bookingId, "测试取消"));
                } else {
                    return Mono.error(new RuntimeException("预订失败"));
                }
            })
            .subscribe(
                cancelResponse -> {
                    if (cancelResponse != null && "success".equals(cancelResponse.getStatus())) {
                        logger.info("完整流程执行成功：搜索 -> 预订 -> 取消");
                    } else {
                        logger.error("取消操作失败: {}", cancelResponse);
                    }
                },
                error -> logger.error("完整流程执行失败", error)
            );
    }
    
    /**
     * 健康检查示例
     */
    public void healthCheckExample() {
        logger.info("=== QTECH适配器健康检查示例 ===");
        
        // 检查适配器是否启用
        boolean isEnabled = asianOverlandAdapter.isEnabled();
        logger.info("适配器启用状态: {}", isEnabled);
        
        // 检查支持的城市
        boolean supportsDubai = asianOverlandAdapter.supportsCity("Dubai");
        boolean supportsKualaLumpur = asianOverlandAdapter.supportsCity("Kuala Lumpur");
        logger.info("支持迪拜: {}, 支持吉隆坡: {}", supportsDubai, supportsKualaLumpur);
        
        // 获取适配器配置信息
        logger.info("供应商名称: {}", asianOverlandAdapter.getSupplierName());
        logger.info("优先级: {}", asianOverlandAdapter.getPriority());
        logger.info("超时时间: {} ms", asianOverlandAdapter.getTimeoutMs());
        logger.info("重试次数: {}", asianOverlandAdapter.getRetryCount());
        
        // 执行健康检查
        asianOverlandAdapter.healthCheck().subscribe(
            isHealthy -> logger.info("健康检查结果: {}", isHealthy ? "健康" : "不健康"),
            error -> logger.error("健康检查失败", error)
        );
    }
}
