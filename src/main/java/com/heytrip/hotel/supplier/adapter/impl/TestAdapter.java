package com.heytrip.hotel.supplier.adapter.impl;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.adapter.AbstractSupplierAdapter;
import com.heytrip.hotel.supplier.adapter.builder.QTechQueryBuilder;
import com.heytrip.hotel.supplier.client.HttpClientService;
import com.heytrip.hotel.supplier.dto.supplier.SupplierAuth;
import com.heytrip.hotel.supplier.dto.qtech.req.*;
import com.heytrip.hotel.supplier.dto.qtech.resp.*;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 测试 供应商适配器实现
 *
 * @author Pax
 */
@Component
public class TestAdapter extends AbstractSupplierAdapter {

    @Autowired
    private HttpClientService httpClientService;
    

    // 默认供应商信息（当数据库配置不可用时使用）
    private static final Long DEFAULT_SUPPLIER_ID = 2L;
    private static final String DEFAULT_SUPPLIER_NAME = "TestSupplier";
    private static final String DEFAULT_SUPPLIER_CODE = "TEST_DEMO";

    // QTECH API 地址
    private static final String SEARCH_BASE_URL = "http://colosseum.otrams.com:8087";
    private static final String API_BASE_URL = "https://colosseum.otrams.com";


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
     * 获取当前供应商配置
     */
    public SupplierConfig getSupplierConfig(){
        return  supplierConfig;
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
     */
    @Override
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
     * 统一的HTTP GET请求方法，自动应用认证头部
     *
     * @param baseUrl      基础URL
     * @param endpoint     端点路径
     * @param responseType 响应类型
     * @return 响应结果
     */
    protected <T> Mono<T> executeGetRequest(String baseUrl, String endpoint, Class<T> responseType) {
        return httpClientService.get(
                baseUrl,
                endpoint,
                responseType,
                headers -> {
                    // 应用通用的认证头部
                    headers.header("Content-Type", "application/json");
                    headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter-Pax/1.0");
                    headers.header("X-Supplier", getSafeSupplierName());
                },
                getSafeSupplierId()
        );
    }







    /**
     * 执行QTECH酒店搜索
     *
     * @param request 搜索请求对象
     * @return 搜索结果
     */
    public Mono<QTechSearchResponse> searchHotels(QTechSearchRequest request) {
        logger.info("开始QTECH酒店搜索，目的地: {}, 入住: {}, 离店: {}",
                request.getSelCity(), request.getCheckinDate(), request.getCheckoutDate());

        try {
            // 获取动态认证配置并设置到请求DTO中
            SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
            request.setUsername(authConfig.getUsername());
            request.setPassword(authConfig.getPassword());

            // 校验数据一致性
            if (!request.isRoomDetailsValid()) {
                String msg = request.getRoomDetailsValidationError();
                logger.error("房间校验失败: {}", msg);
                throw new IllegalArgumentException("数据校验失败: "+msg);
            }

            logger.debug("调用QTECH搜索API，目的地: {}, 入住: {}, 离店: {}",
                    request.getSelCity(), request.getCheckinDate(), request.getCheckoutDate());

            String endpoint = QTechQueryBuilder.buildEndpoint(request);
            logger.debug("构建的搜索端点: {}", endpoint);

            return executeGetRequest(SEARCH_BASE_URL, endpoint, QTechSearchResponse.class);

        } catch (Exception e) {
            logger.error("QTECH搜索请求失败", e);
            return Mono.error(new RuntimeException("搜索请求失败: " + e.getMessage()));
        }
    }

    /**
     * 执行QTECH酒店预订
     *
     * @param request 预订请求对象
     * @return 预订结果
     */
    public Mono<QTechReservationResponse> bookHotel(QTechReservationRequest request) {
        logger.info("开始QTECH酒店预订，酒店ID: {}, 房间ID: {}, 订单号: {}",
                request.getHotelId(), request.getSectionUniqueId(), request.getAgentRefNo());

        try {
            // 1. 先获取取消规则（必需步骤）
            QTechCancellationPolicyRequest policyRequest = new QTechCancellationPolicyRequest();
            policyRequest.setHotelId(request.getHotelId());
            policyRequest.setUniqueId(request.getUniqueId());
            policyRequest.setSectionUniqueId(request.getSectionUniqueId());

            return getCancellationPolicy(policyRequest)
                    .flatMap(policy -> {
                        if (policy == null || !"success".equals(policy.getMessage())) {
                            return Mono.error(new RuntimeException("获取取消规则失败,无法进行预定"));
                        }

                        if (policy == null || policy.getTotalBookingAmount() == null) {
                            return Mono.error(new RuntimeException("获取取消规则失败,无法获取预定价格"));
                        }

                        QTechCancellationPolicyResponse.BookingAllowedInfo allowedInfo = policy.getBookingAllowedInfo();
                        if (allowedInfo == null || !"yes".equalsIgnoreCase(allowedInfo.getBookingAllowed())) {
                            return Mono.error(new RuntimeException("当前房型不可预订"));
                        }

                        // 2. 执行预订
                        return executeReservation(request, policy);
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
     * @param request 酒店详情请求对象
     * @return 酒店详情
     */
    public Mono<QTechHotelDetailResponse> getHotelDetail(QTechHotelDetailRequest request) {
        logger.info("开始获取QTECH酒店详情，酒店ID: {}", request.getHotelId());

        try {
            // 获取动态认证配置并设置到请求DTO中
            SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
            request.setUsername(authConfig.getUsername());
            request.setPassword(authConfig.getPassword());

            logger.debug("调用QTECH酒店详情API，酒店ID: {}", request.getHotelId());

            String endpoint = QTechQueryBuilder.buildEndpoint(request);
            logger.debug("构建的酒店详情端点: {}", endpoint);

            return executeGetRequest(API_BASE_URL, endpoint, QTechHotelDetailResponse.class)
                    .doOnSuccess(result -> logger.info("QTECH酒店详情获取完成，酒店ID: {}", request.getHotelId()))
                    .doOnError(error -> logger.error("QTECH酒店详情获取失败，酒店ID: {}", request.getHotelId(), error));

        } catch (Exception e) {
            logger.error("QTECH酒店详情请求构建失败", e);
            return Mono.error(new RuntimeException("酒店详情请求构建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取QTECH预订详情
     *
     * @param request 预订详情请求对象
     * @return 预订详情
     */
    public Mono<QTechBookingDetailResponse> getBookingDetail(QTechBookingDetailRequest request) {
        logger.info("开始获取QTECH预订详情，预订ID: {}", request.getBookingId());

        try {
            // 获取动态认证配置并设置到请求DTO中
            SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
            request.setUsername(authConfig.getUsername());
            request.setPassword(authConfig.getPassword());

            logger.debug("调用QTECH预订详情API，预订ID: {}", request.getBookingId());

            String endpoint = QTechQueryBuilder.buildEndpoint(request);
            logger.debug("构建的预订详情端点: {}", endpoint);

            return executeGetRequest(API_BASE_URL, endpoint, QTechBookingDetailResponse.class)
                    .doOnSuccess(result -> logger.info("QTECH预订详情获取完成，预订ID: {}", request.getBookingId()))
                    .doOnError(error -> logger.error("QTECH预订详情获取失败，预订ID: {}", request.getBookingId(), error));

        } catch (Exception e) {
            logger.error("QTECH预订详情请求构建失败", e);
            return Mono.error(new RuntimeException("预订详情请求构建失败: " + e.getMessage()));
        }
    }

    /**
     * 执行QTECH取消预订
     *
     * @param request 取消请求对象
     * @return 取消结果
     */
    public Mono<QTechCancellationResponse> cancelBooking(QTechCancellationBookingRequest request) {
        logger.info("开始QTECH取消预订，预订ID: {}", request.getBookingId());

        try {

            QTechGetCancellationChargesRequest chargesRequest = new QTechGetCancellationChargesRequest();
            chargesRequest.setBookingId(request.getBookingId());
            chargesRequest.setBookingReference(request.getBookingReference());
            // 1. 先获得取消费用
            return getCancellationCharges(chargesRequest)
                    .flatMap(chargesResult -> {
                        if (chargesResult == null || !"success".equals(chargesResult.getStatus())) {
                            return Mono.error(new RuntimeException("获取取消费用失败"));
                        }

                        // 2. 执行取消
                        return executeCancellation(request);
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
     * 获取取消规则 （获取的精准最新的预定价格）
     *
     * @param request 取消规则请求DTO
     * @return
     */
    private Mono<QTechCancellationPolicyResponse> getCancellationPolicy(QTechCancellationPolicyRequest request) {
        return executeCancellationPolicy(request);
    }

    /**
     * 执行取消规则查询
     */
    private Mono<QTechCancellationPolicyResponse> executeCancellationPolicy(QTechCancellationPolicyRequest request) {
        // 获取动态认证配置并设置到请求DTO中
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        request.setUsername(authConfig.getUsername());
        request.setPassword(authConfig.getPassword());

        logger.debug("调用QTECH取消规则API，酒店ID: {}, 房型ID: {}",
                request.getHotelId(), request.getSectionUniqueId());

        String endpoint = QTechQueryBuilder.buildEndpoint(request);
        logger.debug("构建的取消规则端点: {}", endpoint);

        return executeGetRequest(API_BASE_URL, endpoint, QTechCancellationPolicyResponse.class);
    }

    /**
     * 执行预订
     */
    private Mono<QTechReservationResponse> executeReservation(QTechReservationRequest request,
                                                              QTechCancellationPolicyResponse policy) {
        // 获取动态认证配置并设置到请求DTO中
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        request.setUsername(authConfig.getUsername());
        request.setPassword(authConfig.getPassword());

        // 设置预期价格（从取消规则响应中获取）
        if (policy != null && policy.getTotalBookingAmount() != null) {
            request.setExpectedPrice(policy.getTotalBookingAmount());
        }

        logger.debug("调用QTECH预订API，酒店ID: {}, 房型ID: {}, 订单号: {}",
                request.getHotelId(), request.getSectionUniqueId(), request.getAgentRefNo());

        String endpoint = QTechQueryBuilder.buildEndpoint(request);
        logger.debug("构建的预订端点: {}", endpoint);

        return executeGetRequest(API_BASE_URL, endpoint, QTechReservationResponse.class);
    }

    /**
     * 获取酒店预定取消费用
     *
     * @param request
     * @return
     */
    private Mono<QTechCancellationChargesResponse> getCancellationCharges(QTechGetCancellationChargesRequest request) {
        // 获取动态认证配置并设置到请求DTO中
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        request.setUsername(authConfig.getUsername());
        request.setPassword(authConfig.getPassword());

        logger.debug("调用QTECH取消费用API，预订ID: {}", request.getBookingId());

        String endpoint = QTechQueryBuilder.buildEndpoint(request);
        logger.debug("构建的取消费用端点: {}", endpoint);

        return executeGetRequest(API_BASE_URL, endpoint, QTechCancellationChargesResponse.class);
    }

    /**
     * 执行取消
     */
    private Mono<QTechCancellationResponse> executeCancellation(QTechCancellationBookingRequest request) {
        // 获取动态认证配置并设置到请求DTO中
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        request.setUsername(authConfig.getUsername());
        request.setPassword(authConfig.getPassword());

        logger.debug("调用QTECH取消预订API，预订ID: {}", request.getBookingId());

        String endpoint = QTechQueryBuilder.buildEndpoint(request);
        logger.debug("构建的取消预订端点: {}", endpoint);

        return executeGetRequest(API_BASE_URL, endpoint, QTechCancellationResponse.class);
    }


}
