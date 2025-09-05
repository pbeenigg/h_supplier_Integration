package com.heytrip.hotel.supplier.adapter.impl;

import com.heytrip.hotel.supplier.adapter.AbstractSupplierAdapter;
import com.heytrip.hotel.supplier.dto.request.HotelSearchRequest;
import com.heytrip.hotel.supplier.dto.request.CreateOrderRequest;
import com.heytrip.hotel.supplier.dto.response.HotelSearchResponse;
import com.heytrip.hotel.supplier.dto.response.CreateOrderResponse;
import com.heytrip.hotel.supplier.dto.response.HotelInfo;
import com.heytrip.hotel.supplier.dto.response.RoomInfo;
import com.heytrip.hotel.supplier.dto.response.RatePlan;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Asianoverland Via QTECH 供应商适配器实现
 * 
 * @author  Pax
 */
@Component
public class AsianOverlandAdapter extends AbstractSupplierAdapter {
    
    private static final String SUPPLIER_NAME = "AsianOverland";
    private static final List<String> SUPPORTED_CITIES = Arrays.asList(
            "Kuala Lumpur", "Penang", "Johor Bahru", "Malacca", "Ipoh", "Kota Kinabalu", "Kuching"
    );
    
    @PostConstruct
    public void init() {
        initialize();
    }
    
    @Override
    public String getSupplierName() {
        return SUPPLIER_NAME;
    }
    
    @Override
    public boolean supportsCity(String city) {
        return SUPPORTED_CITIES.stream()
                .anyMatch(supportedCity -> supportedCity.equalsIgnoreCase(city));
    }
    
    @Override
    public int getPriority() {
        return 10; // 高优先级
    }
    
    @Override
    public Mono<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        validateSearchRequest(request);
        
        if (!supportsCity(request.getCity())) {
            return Mono.just(createEmptySearchResponse("City not supported: " + request.getCity()));
        }
        
        logger.info("Searching hotels for city: {} with supplier: {}", request.getCity(), getSupplierName());
        
        return executeWithRetry(
                webClient.post()
                        .uri("/hotel/search")
                        .bodyValue(buildSearchRequestBody(request))
                        .headers(headers -> addAuthHeaders(headers))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .map(this::parseSearchResponse)
        ).onErrorResume(error -> {
            logger.error("Hotel search failed for supplier: {}", getSupplierName(), error);
            return Mono.just(createErrorSearchResponse(error.getMessage()));
        });
    }
    
    @Override
    public Mono<CreateOrderResponse> createOrder(CreateOrderRequest request) {
        validateOrderRequest(request);
        
        logger.info("Creating order for hotel: {} with supplier: {}", request.getHotelId(), getSupplierName());
        
        return executeWithRetry(
                webClient.post()
                        .uri("/booking/create")
                        .bodyValue(buildOrderRequestBody(request))
                        .headers(headers -> addAuthHeaders(headers))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .map(this::parseOrderResponse)
        ).onErrorResume(error -> {
            logger.error("Order creation failed for supplier: {}", getSupplierName(), error);
            return Mono.just(createErrorOrderResponse(error.getMessage()));
        });
    }
    
    @Override
    public Mono<Boolean> cancelOrder(String bookingReference, String reason) {
        logger.info("Cancelling order: {} with supplier: {}", bookingReference, getSupplierName());
        
        return executeWithRetry(
                webClient.post()
                        .uri("/booking/cancel")
                        .bodyValue(buildCancelRequestBody(bookingReference, reason))
                        .headers(headers -> addAuthHeaders(headers))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .map(response -> {
                            Integer code = (Integer) response.get("code");
                            return code != null && code == 0;
                        })
        ).onErrorReturn(false);
    }
    
    @Override
    public Mono<Integer> getOrderStatus(String bookingReference) {
        logger.info("Getting order status for: {} with supplier: {}", bookingReference, getSupplierName());
        
        return executeWithRetry(
                webClient.get()
                        .uri("/booking/status?bookingReference={bookingReference}", bookingReference)
                        .headers(headers -> addAuthHeaders(headers))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .map(response -> {
                            Integer status = (Integer) response.get("status");
                            return status != null ? status : 0;
                        })
        ).onErrorReturn(0);
    }
    
    @Override
    protected void addAuthHeaders(HttpHeaders headers) {
        if (supplierConfig != null && supplierConfig.getAuthConfig() != null) {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String appId = extractFromAuthConfig("appId");
            String secretKey = extractFromAuthConfig("secretKey");
            
            if (appId != null && secretKey != null) {
                String signature = generateSignature(appId, secretKey, timestamp);
                headers.add("X-App-Id", appId);
                headers.add("X-Timestamp", timestamp);
                headers.add("X-Signature", signature);
                headers.add("Content-Type", "application/json");
            }
        }
    }
    
    private String extractFromAuthConfig(String key) {
        try {
            String authConfig = supplierConfig.getAuthConfig();
            // 简单的JSON解析，实际项目中应使用Jackson或其他JSON库
            if (authConfig.contains("\"" + key + "\"")) {
                int start = authConfig.indexOf("\"" + key + "\"") + key.length() + 3;
                int end = authConfig.indexOf("\"", start + 1);
                return authConfig.substring(start + 1, end);
            }
        } catch (Exception e) {
            logger.error("Failed to extract {} from authorization config", key, e);
        }
        return null;
    }
    
    private String generateSignature(String appId, String secretKey, String timestamp) {
        try {
            String data = appId + timestamp + secretKey;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Failed to generate signature", e);
            return "";
        }
    }
    
    private Map<String, Object> buildSearchRequestBody(HotelSearchRequest request) {
        return Map.of(
                "city", request.getCity(),
                "checkInDate", request.getCheckInDate().toString(),
                "checkOutDate", request.getCheckOutDate().toString(),
                "roomCount", request.getRoomCount(),
                "guestCount", request.getGuestCount(),
                "currency", request.getCurrency() != null ? request.getCurrency() : "MYR",
                "nationality", request.getNationality() != null ? request.getNationality() : "MY"
        );
    }
    
    private Map<String, Object> buildOrderRequestBody(CreateOrderRequest request) {
        return Map.of(
                "hotelId", request.getHotelId(),
                "roomId", request.getRoomId(),
                "ratePlanId", request.getRatePlanId(),
                "checkInDate", request.getCheckInDate().toString(),
                "checkOutDate", request.getCheckOutDate().toString(),
                "roomCount", request.getRoomCount(),
                "guestCount", request.getGuestCount(),
                "guestName", request.getGuestName(),
                "salePrice", request.getSalePrice(),
                "currency", request.getCurrency()
        );
    }
    
    private Map<String, Object> buildCancelRequestBody(String bookingReference, String reason) {
        return Map.of(
                "bookingReference", bookingReference,
                "reason", reason != null ? reason : "Customer request"
        );
    }
    
    private HotelSearchResponse parseSearchResponse(Map<String, Object> response) {
        HotelSearchResponse searchResponse = new HotelSearchResponse();
        
        Integer code = (Integer) response.get("code");
        if (code != null && code == 0) {
            List<Map<String, Object>> hotelList = (List<Map<String, Object>>) response.get("hotels");
            List<HotelInfo> hotels = new ArrayList<>();
            
            if (hotelList != null) {
                for (Map<String, Object> hotelData : hotelList) {
                    HotelInfo hotel = parseHotelInfo(hotelData);
                    hotels.add(hotel);
                }
            }
            
            searchResponse.setHotels(hotels);
            searchResponse.setTotalCount(hotels.size());
            searchResponse.setCurrency("MYR");
            searchResponse.setMessage("Success");
        } else {
            searchResponse.setMessage((String) response.get("message"));
        }
        
        return searchResponse;
    }
    
    private HotelInfo parseHotelInfo(Map<String, Object> hotelData) {
        HotelInfo hotel = new HotelInfo();
        hotel.setHotelId((String) hotelData.get("hotelId"));
        hotel.setSupplierHotelId((String) hotelData.get("supplierHotelId"));
        hotel.setHotelName((String) hotelData.get("hotelName"));
        hotel.setHotelAddress((String) hotelData.get("address"));
        hotel.setCity((String) hotelData.get("city"));
        hotel.setCountry("Malaysia");
        hotel.setSupplierName(getSupplierName());
        hotel.setIsActive(true);
        
        // 解析星级
        Object starRating = hotelData.get("starRating");
        if (starRating instanceof Number) {
            hotel.setStarRating(new BigDecimal(starRating.toString()));
        }
        
        // 解析坐标
        Object latitude = hotelData.get("latitude");
        Object longitude = hotelData.get("longitude");
        if (latitude instanceof Number) {
            hotel.setLatitude(new BigDecimal(latitude.toString()));
        }
        if (longitude instanceof Number) {
            hotel.setLongitude(new BigDecimal(longitude.toString()));
        }
        
        // 解析房间信息
        List<Map<String, Object>> roomList = (List<Map<String, Object>>) hotelData.get("rooms");
        if (roomList != null) {
            List<RoomInfo> rooms = new ArrayList<>();
            for (Map<String, Object> roomData : roomList) {
                RoomInfo room = parseRoomInfo(roomData);
                rooms.add(room);
            }
            hotel.setRooms(rooms);
        }
        
        return hotel;
    }
    
    private RoomInfo parseRoomInfo(Map<String, Object> roomData) {
        RoomInfo room = new RoomInfo();
        room.setRoomId((String) roomData.get("roomId"));
        room.setSupplierRoomId((String) roomData.get("supplierRoomId"));
        room.setRoomName((String) roomData.get("roomName"));
        room.setRoomType((String) roomData.get("roomType"));
        room.setBedType((String) roomData.get("bedType"));
        room.setIsActive(true);
        
        Object maxOccupancy = roomData.get("maxOccupancy");
        if (maxOccupancy instanceof Number) {
            room.setMaxOccupancy(((Number) maxOccupancy).intValue());
        }
        
        // 解析价格计划
        List<Map<String, Object>> ratePlanList = (List<Map<String, Object>>) roomData.get("ratePlans");
        if (ratePlanList != null) {
            List<RatePlan> ratePlans = new ArrayList<>();
            for (Map<String, Object> ratePlanData : ratePlanList) {
                RatePlan ratePlan = parseRatePlan(ratePlanData);
                ratePlans.add(ratePlan);
            }
            room.setRatePlans(ratePlans);
        }
        
        return room;
    }
    
    private RatePlan parseRatePlan(Map<String, Object> ratePlanData) {
        RatePlan ratePlan = new RatePlan();
        ratePlan.setRatePlanId((String) ratePlanData.get("ratePlanId"));
        ratePlan.setRatePlanName((String) ratePlanData.get("ratePlanName"));
        ratePlan.setCurrency("MYR");
        ratePlan.setSupplierName(getSupplierName());
        
        Object basePrice = ratePlanData.get("basePrice");
        if (basePrice instanceof Number) {
            ratePlan.setBasePrice(new BigDecimal(basePrice.toString()));
        }
        
        Object price = ratePlanData.get("price");
        if (price instanceof Number) {
            ratePlan.setPrice(new BigDecimal(price.toString()));
        }
        
        Object cancelable = ratePlanData.get("cancelable");
        if (cancelable instanceof Boolean) {
            ratePlan.setCancelable((Boolean) cancelable);
        }
        
        return ratePlan;
    }
    
    private CreateOrderResponse parseOrderResponse(Map<String, Object> response) {
        Integer code = (Integer) response.get("code");
        Integer bizCode = (Integer) response.get("bizCode");
        String message = (String) response.get("message");
        
        if (code != null && code == 0) {
            String bookingReference = (String) response.get("bookingReference");
            String supplierBookingId = (String) response.get("supplierBookingId");
            Integer status = (Integer) response.get("status");
            
            CreateOrderResponse orderResponse = new CreateOrderResponse(bookingReference, supplierBookingId, status);
            orderResponse.setMessage(message);
            
            Object totalAmount = response.get("totalAmount");
            if (totalAmount instanceof Number) {
                orderResponse.setTotalAmount(new BigDecimal(totalAmount.toString()));
            }
            orderResponse.setCurrency("MYR");
            
            return orderResponse;
        } else {
            return new CreateOrderResponse(code, bizCode, message);
        }
    }
    
    private HotelSearchResponse createEmptySearchResponse(String message) {
        HotelSearchResponse response = new HotelSearchResponse();
        response.setHotels(new ArrayList<>());
        response.setTotalCount(0);
        response.setMessage(message);
        return response;
    }
    
    private HotelSearchResponse createErrorSearchResponse(String errorMessage) {
        HotelSearchResponse response = new HotelSearchResponse();
        response.setHotels(new ArrayList<>());
        response.setTotalCount(0);
        response.setMessage("Search failed: " + errorMessage);
        return response;
    }
    
    private CreateOrderResponse createErrorOrderResponse(String errorMessage) {
        return new CreateOrderResponse(-1, -1, "Order creation failed: " + errorMessage);
    }
}
