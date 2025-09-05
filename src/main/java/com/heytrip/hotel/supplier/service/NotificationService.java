package com.heytrip.hotel.supplier.service;

import com.heytrip.hotel.supplier.dto.response.CreateOrderResponse;
import com.heytrip.hotel.supplier.entity.BookingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 通知服务
 * 负责发送各种业务通知
 * 
 * @author  Pax
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    /**
     * 发送预订确认通知
     * @param orderResponse 订单响应
     * @return 发送结果
     */
    public Mono<Boolean> sendBookingConfirmation(CreateOrderResponse orderResponse) {
        logger.info("Sending booking confirmation for order: {}", orderResponse.getBookingReference());
        
        return Mono.fromCallable(() -> {
            // 这里应该实现实际的通知逻辑，比如发送邮件、短信等
            // 目前只是记录日志
            logger.info("Booking confirmation sent successfully for order: {}", orderResponse.getBookingReference());
            return true;
        }).onErrorResume(error -> {
            logger.error("Failed to send booking confirmation for order: {}", 
                    orderResponse.getBookingReference(), error);
            return Mono.just(false);
        });
    }
    
    /**
     * 发送取消通知
     * @param bookingRecord 订单记录
     * @param reason 取消原因
     * @return 发送结果
     */
    public Mono<Boolean> sendCancellationNotification(BookingRecord bookingRecord, String reason) {
        logger.info("Sending cancellation notification for order: {}", bookingRecord.getBookingReference());
        
        return Mono.fromCallable(() -> {
            // 这里应该实现实际的通知逻辑
            logger.info("Cancellation notification sent successfully for order: {}, reason: {}", 
                    bookingRecord.getBookingReference(), reason);
            return true;
        }).onErrorResume(error -> {
            logger.error("Failed to send cancellation notification for order: {}", 
                    bookingRecord.getBookingReference(), error);
            return Mono.just(false);
        });
    }
}
