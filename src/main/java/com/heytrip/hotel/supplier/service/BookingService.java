package com.heytrip.hotel.supplier.service;

import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.dto.request.CreateOrderRequest;
import com.heytrip.hotel.supplier.dto.response.CreateOrderResponse;
import com.heytrip.hotel.supplier.entity.BookingRecord;
import com.heytrip.hotel.supplier.repository.BookingRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 订单预订服务
 * 负责酒店预订业务逻辑，包括订单创建、取消、状态查询等
 * 
 * @author  Pax
 */
@Service
public class BookingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    
    @Autowired
    private SupplierAdapterManager supplierAdapterManager;
    
    @Autowired
    private BookingRecordRepository bookingRecordRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 创建订单
     * @param supplierName 供应商名称
     * @param request 订单请求
     * @return 订单响应
     */
    @Transactional
    public Mono<CreateOrderResponse> createOrder(String supplierName, CreateOrderRequest request) {
        logger.info("Creating order with supplier: {} for hotel: {}", supplierName, request.getHotelId());
        
        // 验证请求参数
        validateOrderRequest(request);
        
        // 生成内部订单号
        String internalOrderId = generateInternalOrderId();
        
        return supplierAdapterManager.createOrder(supplierName, request)
                .flatMap(response -> {
                    if (response.getHasError() != null && response.getHasError()) {
                        logger.error("Order creation failed with supplier: {}, error: {}", 
                                supplierName, response.getMessage());
                        return Mono.just(response);
                    }
                    
                    // 保存订单记录
                    return saveBookingRecord(supplierName, request, response, internalOrderId)
                            .then(Mono.just(response))
                            .doOnSuccess(savedResponse -> {
                                // 发送确认通知
                                notificationService.sendBookingConfirmation(savedResponse)
                                        .subscribe(
                                                success -> logger.info("Booking confirmation sent for order: {}", 
                                                        savedResponse.getBookingReference()),
                                                error -> logger.warn("Failed to send booking confirmation", error)
                                        );
                            });
                })
                .doOnSuccess(response -> 
                        logger.info("Order creation completed for supplier: {}, booking reference: {}", 
                                supplierName, response.getBookingReference()))
                .doOnError(error -> 
                        logger.error("Order creation failed for supplier: {}", supplierName, error));
    }
    
    /**
     * 取消订单
     * @param bookingReference 订单号
     * @param reason 取消原因
     * @return 取消结果
     */
    @Transactional
    public Mono<Boolean> cancelOrder(String bookingReference, String reason) {
        logger.info("Cancelling order: {}, reason: {}", bookingReference, reason);
        
        return Mono.fromCallable(() -> bookingRecordRepository.findByBookingReference(bookingReference))
                .flatMap(optionalRecord -> {
                    if (optionalRecord.isEmpty()) {
                        logger.error("Booking record not found: {}", bookingReference);
                        return Mono.just(false);
                    }
                    
                    BookingRecord record = optionalRecord.get();
                    String supplierName = extractSupplierName(record.getSupplierId());
                    
                    return supplierAdapterManager.cancelOrder(supplierName, bookingReference, reason)
                            .flatMap(cancelled -> {
                                if (cancelled) {
                                    // 更新订单状态
                                    record.setBookingStatus(9); // 已取消
                                    record.setUpdatedAt(LocalDateTime.now());
                                    
                                    return Mono.fromCallable(() -> bookingRecordRepository.save(record))
                                            .then(Mono.just(true))
                                            .doOnSuccess(success -> {
                                                // 发送取消通知
                                                notificationService.sendCancellationNotification(record, reason)
                                                        .subscribe(
                                                                notified -> logger.info("Cancellation notification sent for order: {}", 
                                                                        bookingReference),
                                                                error -> logger.warn("Failed to send cancellation notification", error)
                                                        );
                                            });
                                } else {
                                    return Mono.just(false);
                                }
                            });
                })
                .doOnSuccess(cancelled -> 
                        logger.info("Order cancellation {} for booking: {}", 
                                cancelled ? "successful" : "failed", bookingReference))
                .doOnError(error -> 
                        logger.error("Order cancellation failed for booking: {}", bookingReference, error));
    }
    
    /**
     * 查询订单状态
     * @param bookingReference 订单号
     * @return 订单状态
     */
    public Mono<Integer> getOrderStatus(String bookingReference) {
        logger.info("Getting order status for: {}", bookingReference);
        
        return Mono.fromCallable(() -> bookingRecordRepository.findByBookingReference(bookingReference))
                .flatMap(optionalRecord -> {
                    if (optionalRecord.isEmpty()) {
                        logger.error("Booking record not found: {}", bookingReference);
                        return Mono.just(0);
                    }
                    
                    BookingRecord record = optionalRecord.get();
                    String supplierName = extractSupplierName(record.getSupplierId());
                    
                    // 从供应商获取最新状态
                    return supplierAdapterManager.getOrderStatus(supplierName, bookingReference)
                            .flatMap(status -> {
                                // 如果状态有变化，更新本地记录
                                if (!status.equals(record.getBookingStatus())) {
                                    record.setBookingStatus(status);
                                    record.setUpdatedAt(LocalDateTime.now());
                                    
                                    return Mono.fromCallable(() -> bookingRecordRepository.save(record))
                                            .then(Mono.just(status));
                                } else {
                                    return Mono.just(status);
                                }
                            })
                            .onErrorReturn(record.getBookingStatus()); // 如果供应商查询失败，返回本地状态
                })
                .doOnSuccess(status -> 
                        logger.info("Order status retrieved for booking: {}, status: {}", bookingReference, status))
                .doOnError(error -> 
                        logger.error("Failed to get order status for booking: {}", bookingReference, error));
    }
    
    /**
     * 获取用户的订单列表
     * @param guestName 客人姓名
     * @param limit 限制数量
     * @return 订单列表
     */
    public Mono<List<BookingRecord>> getUserBookings(String guestName, int limit) {
        logger.info("Getting bookings for guest: {}, limit: {}", guestName, limit);
        
        return Mono.fromCallable(() -> 
                bookingRecordRepository.findByGuestName(guestName)
                        .stream()
                        .limit(limit)
                        .toList()
        );
    }
    
    /**
     * 获取订单详情
     * @param bookingReference 订单号
     * @return 订单详情
     */
    public Mono<BookingRecord> getBookingDetails(String bookingReference) {
        logger.info("Getting booking details for: {}", bookingReference);
        
        return Mono.fromCallable(() -> 
                bookingRecordRepository.findByBookingReference(bookingReference)
                        .orElse(null)
        );
    }
    

    
    /**
     * 保存订单记录
     */
    private Mono<BookingRecord> saveBookingRecord(String supplierName, CreateOrderRequest request, 
                                                 CreateOrderResponse response, String internalOrderId) {
        return Mono.fromCallable(() -> {
            BookingRecord record = new BookingRecord();
            record.setBookingReference(response.getBookingReference());
            record.setSupplierBookingId(response.getSupplierBookingId());
            record.setDistributorOrderId(internalOrderId);
            record.setSupplierId(getSupplierIdByName(supplierName));
            record.setHotelId(Long.parseLong(request.getHotelId()));
            record.setGuestName(request.getGuestName());
            record.setCheckInDate(request.getCheckInDate());
            record.setCheckOutDate(request.getCheckOutDate());
            record.setRoomCount(request.getRoomCount());
            record.setGuestCount(request.getGuestCount());
            record.setTotalAmount(response.getTotalAmount());
            record.setCurrency(response.getCurrency());
            record.setBookingStatus(response.getStatus() != null ? response.getStatus() : 1);
            record.setChannel(request.getChannel());
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            
            return bookingRecordRepository.save(record);
        });
    }
    
    /**
     * 验证订单请求
     */
    private void validateOrderRequest(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request cannot be null");
        }
        
        if (request.getHotelId() == null || request.getHotelId().trim().isEmpty()) {
            throw new IllegalArgumentException("Hotel ID is required");
        }
        
        if (request.getRoomId() == null || request.getRoomId().trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID is required");
        }
        
        if (request.getGuestName() == null || request.getGuestName().trim().isEmpty()) {
            throw new IllegalArgumentException("Guest name is required");
        }
        
        if (request.getCheckInDate() == null) {
            throw new IllegalArgumentException("Check-in date is required");
        }
        
        if (request.getCheckOutDate() == null) {
            throw new IllegalArgumentException("Check-out date is required");
        }
        
        if (request.getCheckInDate().isAfter(request.getCheckOutDate()) || 
            request.getCheckInDate().isEqual(request.getCheckOutDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
        
        if (request.getRoomCount() == null || request.getRoomCount() <= 0) {
            throw new IllegalArgumentException("Room count must be greater than 0");
        }
        
        if (request.getGuestCount() == null || request.getGuestCount() <= 0) {
            throw new IllegalArgumentException("Guest count must be greater than 0");
        }
        
        if (request.getSalePrice() == null || request.getSalePrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sale price must be greater than 0");
        }
    }
    
    /**
     * 生成内部订单号
     */
    private String generateInternalOrderId() {
        return "HT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 根据供应商名称获取供应商ID
     */
    private Long getSupplierIdByName(String supplierName) {
        // 简化实现，实际项目中应从数据库查询
        switch (supplierName.toLowerCase()) {
            case "asianoverland":
                return 1L;
            default:
                return 0L;
        }
    }
    
    /**
     * 从供应商ID提取供应商名称
     */
    private String extractSupplierName(Long supplierId) {
        // 简化实现，实际项目中应从数据库查询
        if (supplierId == 1L) {
            return "AsianOverland";
        }
        return "Unknown";
    }
}
