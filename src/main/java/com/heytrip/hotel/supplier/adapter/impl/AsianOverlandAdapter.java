package com.heytrip.hotel.supplier.adapter.impl;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.adapter.AbstractSupplierAdapter;
import com.heytrip.hotel.supplier.client.HttpClientService;
import com.heytrip.hotel.supplier.dto.base.SupplierAuth;
import com.heytrip.hotel.supplier.dto.qtech.*;
import com.heytrip.hotel.supplier.dto.qtech.QTechCancellationPolicyResponse;
import com.heytrip.hotel.supplier.dto.qtech.QTechReservationResponse;
import com.heytrip.hotel.supplier.dto.qtech.QTechSearchResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Asianoverland Via QTECH 供应商适配器实现
 * 
 * 实现QTECH API的酒店搜索、预订、取消等核心功能
 * API文档参考：docs/需求记录/马来供应商(Asianoverland Via QTECH).md
 *
 * @author  Pax
 */
@Component
public class AsianOverlandAdapter extends AbstractSupplierAdapter {

    @Autowired
    private HttpClientService httpClientService;

    // 默认供应商信息（当数据库配置不可用时使用）
    private static final Long DEFAULT_SUPPLIER_ID = 1L;
    private static final String DEFAULT_SUPPLIER_NAME = "AsianOverland";
    private static final String DEFAULT_SUPPLIER_CODE = "AO_QTECH";

    // QTECH API 地址
    private static final String SEARCH_BASE_URL = "http://colosseum.otrams.com:8087/ws/index.php";
    private static final String API_BASE_URL = "https://colosseum.otrams.com/ws/index.php";
    
    // 支持的城市列表（可扩展）
    private static final List<String> SUPPORTED_CITIES = Arrays.asList(
            "Kuala Lumpur", "Penang", "Johor Bahru", "Malacca", "Ipoh", "Kota Kinabalu", "Kuching",
            "Dubai", "Singapore", "Bangkok", "Manila", "Jakarta"
    );
    
    // 日期格式化器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");


    /**
     * 初始化适配器
     */
    @PostConstruct
    public void init() {
        initialize();
    }

    /**
     * 获取供应商标识符
     * 用于从数据库加载供应商配置，避免循环依赖
     */
    @Override
    public String getSupplierCode() {
        return DEFAULT_SUPPLIER_CODE; // 供应商代码，用于数据库查询
    }

    /**
     * 安全获取供应商ID（优先使用数据库配置，否则使用默认值）
     */
    private Long getSafeSupplierId() {
        Long supplierId = getSupplierId();
        return supplierId != null ? supplierId : DEFAULT_SUPPLIER_ID;
    }

    /**
     * 安全获取供应商名称（优先使用数据库配置，否则使用默认值）
     */
    private String getSafeSupplierName() {
        String supplierName = getSupplierName();
        return StrUtil.isNotBlank(supplierName) ? supplierName : DEFAULT_SUPPLIER_NAME;
    }



    /**
     * 检查是否支持指定城市
     */
    @Override
    public boolean supportsCity(String city) {
        return SUPPORTED_CITIES.stream()
                .anyMatch(supportedCity -> supportedCity.equalsIgnoreCase(city));
    }


    /**
     * 检查是否支持指定国家
     */    @Override
    public boolean supportsCountry(String country) {
        // 目前仅支持马来西亚、新加坡、阿联酋、泰国
        List<String> supportedCountries = Arrays.asList("Malaysia", "Singapore", "UAE", "Thailand");
        return supportedCountries.stream()
                .anyMatch(supportedCountry -> supportedCountry.equalsIgnoreCase(country));
    }


    /**
     * 优先级设置
     * 数值越小优先级越高
     */
    @Override
    public int getPriority() {
        return 10; // 高优先级
    }


    /**
     * 构建头部信息，QTECH使用用户名密码认证
     */
    @Override
    protected void addAuthHeaders(HttpHeaders headers) {
        // QTECH API使用URL参数认证，不需要特殊头部
        headers.add("Content-Type", "application/json");
        headers.add("User-Agent", "HeyTrip-AsianOverland-Adapter-Pax/1.0");
        headers.add("X-Supplier", getSafeSupplierName());
    }


    /**
     * 执行QTECH酒店搜索
     * 
     * @param destination 目的地
     * @param checkInDate 入住日期
     * @param checkOutDate 离店日期
     * @param rooms 房间信息
     * @return 搜索结果
     */
    public Mono<QTechSearchResponse> searchHotels(String destination, String checkInDate, String checkOutDate, int rooms) {
        logger.info("开始QTECH酒店搜索，目的地: {}, 入住: {}, 离店: {}", destination, checkInDate, checkOutDate);
        
        try {
            // 构建搜索请求参数
            Map<String, String> params = buildSearchParams(destination, checkInDate, checkOutDate, rooms);
            
            // 调用QTECH搜索API
            return callQTechSearchApi(params)
                    .doOnSuccess(result -> logger.info("QTECH搜索完成，返回{}家酒店",
                            result != null && result.getHotelList() != null ? result.getHotelList().size() : 0))
                    .doOnError(error -> logger.error("QTECH搜索失败", error));
                    
        } catch (Exception e) {
            logger.error("QTECH搜索请求构建失败", e);
            return Mono.error(new RuntimeException("搜索请求构建失败: " + e.getMessage()));
        }
    }

    /**
     * 执行QTECH酒店预订
     * 
     * @param hotelId 酒店ID
     * @param roomId 房间ID
     * @param agentRefNo 代理参考号
     * @return 预订结果
     */
    public Mono<QTechReservationResponse> bookHotel(String hotelId, String roomId, String agentRefNo) {
        logger.info("开始QTECH酒店预订，酒店ID: {}, 房间ID: {}, 订单号: {}", hotelId, roomId, agentRefNo);
        
        try {
            // 1. 先获取取消政策（必需步骤）
            return getCancellationPolicy(hotelId, roomId)
                    .flatMap(policy -> {
                        if (policy == null || !"success".equals(policy.getMessage())) {
                            return Mono.error(new RuntimeException("获取取消政策失败"));
                        }
                        
                        // 2. 执行预订
                        return executeReservation(hotelId, roomId, agentRefNo, policy);
                    })
                    .doOnSuccess(result -> logger.info("QTECH预订完成，状态: {}", 
                            result != null ? result.getStatus() : "未知"))
                    .doOnError(error -> logger.error("QTECH预订失败", error));
                    
        } catch (Exception e) {
            logger.error("QTECH预订请求构建失败", e);
            return Mono.error(new RuntimeException("预订请求构建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取QTECH酒店详情
     * 
     * @param hotelId 酒店ID
     * @return 酒店详情
     */
    public Mono<QTechHotelDetailResponse> getHotelDetail(String hotelId) {
        logger.info("开始获取QTECH酒店详情，酒店ID: {}", hotelId);
        
        try {
            // 获取动态认证配置
            SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
            
            Map<String, String> params = new HashMap<>();
            params.put("action", "hotel_detail");
            params.put("username", authConfig.getUsername());
            params.put("password", authConfig.getPassword());
            params.put("hotel_id", hotelId);
            params.put("gzip", "no");
            
            StringBuilder endpoint = new StringBuilder("/ws/index.php?");
            params.forEach((key, value) -> {
                endpoint.append(key).append("=").append(value).append("&");
            });
            
            logger.debug("调用QTECH酒店详情API: {}{}", API_BASE_URL, endpoint.toString());
            
            return httpClientService.get(
                API_BASE_URL,
                endpoint.toString(),
                QTechHotelDetailResponse.class,
                headers -> headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter/1.0"),
                getSafeSupplierId()
            )
            .doOnSuccess(result -> logger.info("QTECH酒店详情获取完成，酒店ID: {}", hotelId))
            .doOnError(error -> logger.error("QTECH酒店详情获取失败，酒店ID: {}", hotelId, error));
            
        } catch (Exception e) {
            logger.error("QTECH酒店详情请求构建失败", e);
            return Mono.error(new RuntimeException("酒店详情请求构建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取QTECH预订详情
     * 
     * @param bookingId 预订ID
     * @return 预订详情
     */
    public Mono<QTechBookingDetailResponse> getBookingDetail(String bookingId) {
        logger.info("开始获取QTECH预订详情，预订ID: {}", bookingId);
        
        try {
            // 获取动态认证配置
            SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
            
            Map<String, String> params = new HashMap<>();
            params.put("action", "booking_detail");
            params.put("username", authConfig.getUsername());
            params.put("password", authConfig.getPassword());
            params.put("booking_id", bookingId);
            params.put("gzip", "no");
            
            StringBuilder endpoint = new StringBuilder("/ws/index.php?");
            params.forEach((key, value) -> {
                endpoint.append(key).append("=").append(value).append("&");
            });
            
            logger.debug("调用QTECH预订详情API: {}{}", API_BASE_URL, endpoint.toString());
            
            return httpClientService.get(
                API_BASE_URL,
                endpoint.toString(),
                QTechBookingDetailResponse.class,
                headers -> headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter/1.0"),
                getSafeSupplierId()
            )
            .doOnSuccess(result -> logger.info("QTECH预订详情获取完成，预订ID: {}", bookingId))
            .doOnError(error -> logger.error("QTECH预订详情获取失败，预订ID: {}", bookingId, error));
            
        } catch (Exception e) {
            logger.error("QTECH预订详情请求构建失败", e);
            return Mono.error(new RuntimeException("预订详情请求构建失败: " + e.getMessage()));
        }
    }

    /**
     * 执行QTECH取消预订
     * 
     * @param bookingId 预订ID
     * @param reason 取消原因
     * @return 取消结果
     */
    public Mono<QTechCancellationResponse> cancelBooking(String bookingId, String reason) {
        logger.info("开始QTECH取消预订，预订ID: {}, 原因: {}", bookingId, reason);
        
        try {
            // 1. 先获得取消费用
            return getCancellationCharges(bookingId)
                    .flatMap(chargesResult -> {
                        if (chargesResult == null || !"success".equals(chargesResult.getStatus())) {
                            return Mono.error(new RuntimeException("获取取消费用失败"));
                        }
                        
                        // 2. 执行取消
                        return executeCancellation(bookingId);
                    })
                    .doOnSuccess(result -> logger.info("QTECH取消完成，状态: {}", 
                            result != null ? result.getStatus() : "未知"))
                    .doOnError(error -> logger.error("QTECH取消失败", error));
                    
        } catch (Exception e) {
            logger.error("QTECH取消请求构建失败", e);
            return Mono.error(new RuntimeException("取消请求构建失败: " + e.getMessage()));
        }
    }




    /**
     * 构建搜索请求参数
     */
    private Map<String, String> buildSearchParams(String destination, String checkInDate, String checkOutDate, int rooms) {
        Map<String, String> params = new HashMap<>();
        
        // 获取动态认证配置
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        
        // 基础参数
        params.put("action", "hotel_search");
        params.put("username", authConfig.getUsername());
        params.put("password", authConfig.getPassword());
        
        // 日期参数
        params.put("checkin_date", checkInDate);
        params.put("checkout_date", checkOutDate);
        
        // 目的地参数（需要映射到QTECH的城市/国家ID）
        params.put("sel_country", "138"); // 默认阿联酋
        params.put("sel_city", "71649");   // 默认迪拜
        
        // 其他搜索参数
        params.put("chk_ratings", "1.0,2.0,3.0,4.0,5.0");
        params.put("sel_nationality", "106"); // 默认印度
        params.put("country_of_residence", "106");
        params.put("sel_currency", "USD");
        params.put("availableonly", "1");
        params.put("gzip", "no");
        params.put("timeout", "30");
        params.put("static_data", "1");
        params.put("limit_hotel_room_type", "5");
        
        // 房间信息
        params.put("number_of_rooms", String.valueOf(rooms));
        
        // 构建房间详情JSON（简化版）
        try {
            List<Map<String, Object>> roomDetails = new ArrayList<>();
            Map<String, Object> roomDetail = new HashMap<>();
            roomDetail.put("numberOfAdults", 2);
            roomDetails.add(roomDetail);
            params.put("roomDetails", objectMapper.writeValueAsString(roomDetails));
        } catch (Exception e) {
            logger.warn("构建房间详情JSON失败", e);
            params.put("roomDetails", "[{\"numberOfAdults\":2}]");
        }
        
        return params;
    }
    
    /**
     * 调用QTECH搜索API
     */
    private Mono<QTechSearchResponse> callQTechSearchApi(Map<String, String> params) {
        StringBuilder endpoint = new StringBuilder("/ws/index.php?");
        params.forEach((key, value) -> {
            endpoint.append(key).append("=").append(value).append("&");
        });
        
        logger.debug("调用QTECH搜索API: {}{}", SEARCH_BASE_URL, endpoint.toString());
        
        return httpClientService.get(
            SEARCH_BASE_URL,
            endpoint.toString(),
            QTechSearchResponse.class,
            headers -> headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter/1.0"),
            getSafeSupplierId()
        );
    }
    
    /**
     * 获取取消政策
     */
    private Mono<QTechCancellationPolicyResponse> getCancellationPolicy(String hotelId, String roomId) {
        // 获取动态认证配置
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        
        Map<String, String> params = new HashMap<>();
        params.put("action", "hotel_cancellation_policy");
        params.put("username", authConfig.getUsername());
        params.put("password", authConfig.getPassword());
        params.put("hotel_id", hotelId);
        params.put("unique_id", "test-unique-id");
        params.put("section_unique_id", roomId);
        params.put("gzip", "no");
        
        StringBuilder endpoint = new StringBuilder("/ws/index.php?");
        params.forEach((key, value) -> {
            endpoint.append(key).append("=").append(value).append("&");
        });
        
        logger.debug("调用QTECH取消政策API: {}{}", API_BASE_URL, endpoint.toString());
        
        return httpClientService.get(
            API_BASE_URL,
            endpoint.toString(),
            QTechCancellationPolicyResponse.class,
            headers -> headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter/1.0"),
            getSafeSupplierId()
        );
    }
    
    /**
     * 执行预订
     */
    private Mono<QTechReservationResponse> executeReservation(String hotelId, String roomId, String agentRefNo, 
                                                            QTechCancellationPolicyResponse policy) {
        // 获取动态认证配置
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        
        Map<String, String> params = new HashMap<>();
        params.put("action", "hotel_reservation");
        params.put("username", authConfig.getUsername());
        params.put("password", authConfig.getPassword());
        params.put("hotel_id", hotelId);
        params.put("unique_id", "test-unique-id");
        params.put("section_unique_id", roomId);
        params.put("agent_ref_no", agentRefNo);
        params.put("expected_price", policy.getTotalBookingAmount().toString());
        
        // 构建房间详情JSON（简化版）
        params.put("roomDetails", "[{\"numberOfAdults\":2,\"numberOfChilds\":\"0\",\"roomClassId\":\"" + roomId + "\",\"passangers\":[{\"salutation\":\"Mr\",\"first_name\":\"Test\",\"last_name\":\"User\"}]}]");
        
        StringBuilder endpoint = new StringBuilder("/ws/index.php?");
        params.forEach((key, value) -> {
            endpoint.append(key).append("=").append(value).append("&");
        });
        
        logger.debug("调用QTECH预订API: {}{}", API_BASE_URL, endpoint.toString());
        
        return httpClientService.get(
            API_BASE_URL,
            endpoint.toString(),
            QTechReservationResponse.class,
            headers -> headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter/1.0"),
            getSafeSupplierId()
        );
    }

    /**
     * 获取酒店预定取消费用
     * @param bookingId
     * @return
     */
    private Mono<QTechCancellationChargesResponse> getCancellationCharges(String bookingId) {
        // 获取动态认证配置
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        
        Map<String, String> params = new HashMap<>();
        params.put("action", "get_cancellation_charges");
        params.put("username", authConfig.getUsername());
        params.put("password", authConfig.getPassword());
        params.put("booking_id", bookingId);
        
        StringBuilder endpoint = new StringBuilder("/ws/index.php?");
        params.forEach((key, value) -> {
            endpoint.append(key).append("=").append(value).append("&");
        });
        
        logger.debug("调用QTECH取消费用API: {}{}", API_BASE_URL, endpoint.toString());
        
        return httpClientService.get(
            API_BASE_URL,
            endpoint.toString(),
            QTechCancellationChargesResponse.class,
            headers -> headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter/1.0"),
            getSafeSupplierId()
        );
    }
    
    /**
     * 执行取消
     */
    private Mono<QTechCancellationResponse> executeCancellation(String bookingId) {
        // 获取动态认证配置
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        
        Map<String, String> params = new HashMap<>();
        params.put("action", "cancel_the_booking");
        params.put("username", authConfig.getUsername());
        params.put("password", authConfig.getPassword());
        params.put("booking_id", bookingId);
        
        StringBuilder endpoint = new StringBuilder("/ws/index.php?");
        params.forEach((key, value) -> {
            endpoint.append(key).append("=").append(value).append("&");
        });
        
        logger.debug("调用QTECH取消预订API: {}{}", API_BASE_URL, endpoint.toString());
        
        return httpClientService.get(
            API_BASE_URL,
            endpoint.toString(),
            QTechCancellationResponse.class,
            headers -> headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter/1.0")
        );
    }
    


}
