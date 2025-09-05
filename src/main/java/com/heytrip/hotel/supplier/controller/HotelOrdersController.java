package com.heytrip.hotel.supplier.controller;

import com.heytrip.hotel.supplier.dto.request.CreateOrderRequest;
import com.heytrip.hotel.supplier.dto.response.CreateOrderResponse;
import com.heytrip.hotel.supplier.service.BookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 订单类控制器
 * 提供酒店预订相关的API接口
 * 
 * @author  Pax
 */
@RestController
@Validated
@RequestMapping("/orders")
public class HotelOrdersController {
    
    private static final Logger logger = LoggerFactory.getLogger(HotelOrdersController.class);
    
    @Autowired
    private BookingService bookingService;
    
    /**
     * 创建订单
     * POST /pax/api/xiwanSupplier/supp/orders/create
     */
    @PostMapping("/create")
    public Mono<ResponseEntity<CreateOrderResponse>> createOrder(
            @RequestParam @NotBlank String supplierName,
            @Valid @RequestBody CreateOrderRequest request) {
        
        logger.info("Received order creation request for supplier: {}, hotel: {}", 
                supplierName, request.getHotelId());
        
        return bookingService.createOrder(supplierName, request)
                .map(response -> {
                    if (response.getHasError() != null && response.getHasError()) {
                        return ResponseEntity.badRequest().body(response);
                    } else {
                        return ResponseEntity.ok(response);
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Order creation failed for supplier: {}", supplierName, error);
                    CreateOrderResponse errorResponse = new CreateOrderResponse(-1, -1, 
                            "Order creation failed: " + error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
                });
    }
    
    /**
     * 取消订单
     * POST /pax/api/xiwanSupplier/supp/orders/cancel
     */
    @PostMapping("/cancel")
    public Mono<ResponseEntity<Map<String, Object>>> cancelOrder(
            @RequestParam @NotBlank String bookingReference,
            @RequestParam(required = false) String reason) {
        
        logger.info("Received order cancellation request for booking: {}", bookingReference);
        
        return bookingService.cancelOrder(bookingReference, reason)
                .map(cancelled -> {
                    Map<String, Object> response = Map.of(
                            "success", cancelled,
                            "bookingReference", bookingReference,
                            "message", cancelled ? "Order cancelled successfully" : "Order cancellation failed"
                    );
                    
                    if (cancelled) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.badRequest().body(response);
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Order cancellation failed for booking: {}", bookingReference, error);
                    Map<String, Object> errorResponse = Map.of(
                            "success", false,
                            "bookingReference", bookingReference,
                            "message", "Cancellation failed: " + error.getMessage()
                    );
                    return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
                });
    }
    
    /**
     * 查询订单状态
     * GET /pax/api/xiwanSupplier/supp/orders/status/{bookingReference}
     */
    @GetMapping("/status/{bookingReference}")
    public Mono<ResponseEntity<Map<String, Object>>> getOrderStatus(
            @PathVariable @NotBlank String bookingReference) {
        
        logger.info("Received order status query for booking: {}", bookingReference);
        
        return bookingService.getOrderStatus(bookingReference)
                .map(status -> {
                    String statusDesc = getStatusDescription(status);
                    Map<String, Object> response = Map.of(
                            "bookingReference", bookingReference,
                            "status", status,
                            "statusDescription", statusDesc
                    );
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    logger.error("Order status query failed for booking: {}", bookingReference, error);
                    Map<String, Object> errorResponse = Map.of(
                            "bookingReference", bookingReference,
                            "status", 0,
                            "statusDescription", "Unknown",
                            "error", error.getMessage()
                    );
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    
    /**
     * 获取订单状态描述
     */
    private String getStatusDescription(Integer status) {
        if (status == null) return "Unknown";
        
        switch (status) {
            case 1: return "Pending Confirmation";
            case 2: return "Confirmed";
            case 3: return "In Progress";
            case 4: return "Completed";
            case 5: return "Checked In";
            case 6: return "Checked Out";
            case 7: return "Rejected";
            case 8: return "Cancellation Requested";
            case 9: return "Cancelled";
            case 10: return "Refunded";
            default: return "Unknown Status";
        }
    }
}
